package com.petd.paymentcrawl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petd.paymentcrawl.dto.Message;
import com.petd.paymentcrawl.entity.Payment;
import com.petd.paymentcrawl.repo.PaymentRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PROCESSINGHandler {

    PaymentRepo paymentRepo;

    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper mapper = new ObjectMapper();

    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void pubJob () {
        List<Payment> payments = paymentRepo.findByStatus("PROCESSING");

        if(payments.isEmpty()) {
            log.info("No PROCESSING payments found");
            return;
        }
        payments.forEach(payment -> {
            long createTimeGe = payment.getCreateTime();
            long createTimeLt = createTimeGe + 1;

            Message msg = Message.builder()
                    .shop(payment.getShop())
                    .createTimeGe(createTimeGe)
                    .createTimeLt(createTimeLt)
                    .build();
            try {
                kafkaTemplate.send("crawl-payment", payment.getShop().getId(),
                        mapper.writeValueAsString(msg));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

    }
}
