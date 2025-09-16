package com.petd.paymentcrawl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.petd.paymentcrawl.TelegramService;
import com.petd.paymentcrawl.api.GetPaymentApi;
import com.petd.paymentcrawl.dto.Message;
import com.petd.paymentcrawl.entity.Payment;
import com.petd.paymentcrawl.entity.Shop;
import com.petd.paymentcrawl.repo.PaymentRepo;
import com.petd.paymentcrawl.repo.ShopRepo;
import com.petd.paymentcrawl.sdk.RequestClient;
import com.petd.paymentcrawl.sdk.TiktokApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentHandler {


    PaymentRepo paymentRepo;
    ShopRepo shopRepo;
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper mapper = new ObjectMapper();
    ObjectMapper mapperSnackCase = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    RequestClient requestClient;
    TelegramService telegramService;


    /**
     * Crawl lần 1: 02:00 VN → dữ liệu từ 14:00 hôm trước → 02:00 sáng nay
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void crawlMorning() {
        telegramService.sendMessage("Run 2:00");
        pushJob();
    }

    /**
     * Crawl lần 2: 14:05 VN → dữ liệu từ 14:00 hôm nay → 14:05 VN
     */
    @Scheduled(cron = "0 5 14 * * *", zone = "Asia/Ho_Chi_Minh")
    public void crawlAfternoon() {
        telegramService.sendMessage("Run 14:00");
        pushJob();
    }

    public void pushJob() {
        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime nowVN = ZonedDateTime.now(vnZone);
        long createTimeLt = nowVN.toInstant().getEpochSecond();

        // Xác định mốc reset 14:00 hôm nay
        ZonedDateTime resetTimeVN = nowVN.toLocalDate().atTime(14, 0).atZone(vnZone);

        long createTimeGe;

        // 5 phút chồng lặp
        long overlapSeconds = 5 * 60L;

        if (nowVN.isBefore(resetTimeVN)) {
            // Nếu crawl sáng sớm (trước 14:00) → lấy từ 14:00 hôm qua
            createTimeGe = resetTimeVN.minusDays(1).toInstant().getEpochSecond() - overlapSeconds;
        } else {
            // Nếu crawl sau 14:00 → lấy từ 14:00 hôm nay
            createTimeGe = resetTimeVN.toInstant().getEpochSecond() - overlapSeconds;
        }

        List<Shop> shops = shopRepo.findAll();
        if (shops.isEmpty()) {
            log.info("No shops to crawl.");
            return;
        }
        shops.forEach(shop -> {
            Message msg = Message.builder()
                    .shop(shop)
                    .createTimeGe(createTimeGe)
                    .createTimeLt(createTimeLt)
                    .build();
            try {
                kafkaTemplate.send("crawl-payment", shop.getId(), mapper.writeValueAsString(msg));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        log.info("Pushed {} crawl jobs. Time window: {} → {}", shops.size(), createTimeGe, createTimeLt);
    }


    public void pushJobFromStartOfLastMonth() {
        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime nowVN = ZonedDateTime.now(vnZone);
        long createTimeLt = nowVN.toInstant().getEpochSecond(); // Thời điểm hiện tại UTC

        // Xác định đầu tháng trước theo giờ VN
        ZonedDateTime startOfLastMonthVN = nowVN.withDayOfMonth(1)   // đầu tháng hiện tại
                .minusMonths(1)                                       // lùi về tháng trước
                .toLocalDate()                                        // chỉ lấy ngày
                .atStartOfDay(vnZone);                                // 00:00 giờ VN đầu tháng trước

        long createTimeGe = startOfLastMonthVN.toInstant().getEpochSecond(); // Chuyển sang UTC

        List<Shop> shops = shopRepo.findAll();
        if (shops.isEmpty()) {
            log.info("No shops to crawl.");
            return;
        }

        shops.forEach(shop -> {
            Message msg = Message.builder()
                    .shop(shop)
                    .createTimeGe(createTimeGe)
                    .createTimeLt(createTimeLt)
                    .build();
            try {
                kafkaTemplate.send("crawl-payment", shop.getId(), mapper.writeValueAsString(msg));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        log.info("Pushed {} crawl jobs from start of last month. Time window: {} → {}",
                shops.size(), createTimeGe, createTimeLt);
    }



    @KafkaListener(topics = "crawl-payment",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "5"
    )
    public void handlerJob(ConsumerRecord<String, String> record, Acknowledgment ack) throws JsonProcessingException {
        try {
            List<Payment> payments = new ArrayList<>();
            Message msg = mapper.readValue(record.value(), Message.class);
            GetPaymentApi getPaymentApi = GetPaymentApi.builder()
                    .accessToken(msg.getShop().getAccessToken())
                    .createTimeGe(msg.getCreateTimeGe())
                    .createTimeLt(msg.getCreateTimeLt())
                    .requestClient(requestClient)
                    .shopCipher(msg.getShop().getCipher())
                    .pageSize(100)
                    .build();
            String nextPageToken = null;
            do {
                TiktokApiResponse response = getPaymentApi.callApi();

                if (response.getData() != null && response.getData().has("payments")) {
                    JsonNode paymentsNode = response.getData().get("payments");

                    System.out.println(paymentsNode.asText());
                    for (JsonNode paymentNode : paymentsNode) {
                        Payment payment = mapperSnackCase.treeToValue(paymentNode, Payment.class);
                        payment.setShop(msg.getShop());
                        payments.add(payment);
                    }
                }

                // Cập nhật nextPageToken
                nextPageToken = response.getData() != null && response.getData().has("next_page_token")
                        ? response.getData().get("next_page_token").asText()
                        : null;
                if ("".equals(nextPageToken)) {
                    nextPageToken = null;
                }

                getPaymentApi.setPageToken(nextPageToken);

            } while (nextPageToken != null);
            if(!payments.isEmpty()) {
                paymentRepo.saveAll(payments);
                log.info("Shop {}: saved {} payments, pageToken={}",
                        msg.getShop().getId(), payments.size(), nextPageToken);
            } else {
                log.info("Shop {}: no payments found for this interval", msg.getShop().getId());
            }
            ack.acknowledge();
        }catch (Exception e){
            log.error(e.getMessage());
            telegramService.sendMessage(e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
