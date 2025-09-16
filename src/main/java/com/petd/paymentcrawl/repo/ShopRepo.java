package com.petd.paymentcrawl.repo;

import com.petd.paymentcrawl.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepo  extends JpaRepository<Shop, String> {
}
