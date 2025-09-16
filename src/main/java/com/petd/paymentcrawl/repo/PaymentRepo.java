package com.petd.paymentcrawl.repo;

import com.petd.paymentcrawl.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepo extends JpaRepository<Payment, String> {

    List<Payment> findByStatus(String status);
}
