package com.petd.paymentcrawl.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TiktokApiResponse {
    int code;
    String message;
    JsonNode data;
    String request_id;
}
