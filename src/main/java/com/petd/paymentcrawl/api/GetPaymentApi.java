package com.petd.paymentcrawl.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.petd.paymentcrawl.sdk.RequestClient;
import com.petd.paymentcrawl.sdk.TiktokApiResponse;
import com.petd.paymentcrawl.sdk.TiktokCallApi;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetPaymentApi implements TiktokCallApi {

    private final String api = "/finance/202309/payments";

    RequestClient requestClient;
    String shopCipher;
    String accessToken;
    Long createTimeLt;
    Long createTimeGe;
    Integer pageSize;
    String pageToken;

    @Override
    public Map<String, String> createParameters() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("shop_cipher", shopCipher);
        params.put("create_time_lt", createTimeLt.toString());
        params.put("create_time_ge", createTimeGe.toString());
        params.put("sort_field", "create_time");
        params.put("sort_order", "DESC");
        params.put("page_size", pageSize.toString());
        if(pageToken != null){
            params.put("page_token", pageToken);
        }
        return params;
    }

    @Override
    public TiktokApiResponse callApi() throws JsonProcessingException {
        return  requestClient.get(api, accessToken, createParameters());
    }
}
