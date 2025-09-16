package com.petd.paymentcrawl.dto;

import com.petd.paymentcrawl.entity.Shop;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Message {
    Shop shop;
    Long createTimeLt;
    Long createTimeGe;
}
