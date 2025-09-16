package com.petd.paymentcrawl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Entity
@Table(name = "shop")
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Shop {
    @Id
    @Column(name = "id", nullable = false)
    String id;

    @Column(name = "access_token", nullable = false)
    String accessToken;

    @Column(name = "cipher", nullable = false)
    String cipher;

}