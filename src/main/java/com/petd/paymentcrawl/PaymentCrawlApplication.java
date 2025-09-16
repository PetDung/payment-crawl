package com.petd.paymentcrawl;

import com.petd.paymentcrawl.service.PROCESSINGHandler;
import com.petd.paymentcrawl.service.PaymentHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableScheduling
public class PaymentCrawlApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentCrawlApplication.class, args);
    }

}
