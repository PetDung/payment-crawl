package com.petd.paymentcrawl.sdk;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TiktokException extends RuntimeException {
    int code;
    String message;
}
