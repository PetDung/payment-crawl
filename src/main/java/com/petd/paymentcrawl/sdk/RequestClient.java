package com.petd.paymentcrawl.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestClient {

    private final TikTokSignatureUtil signatureUtil;
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.tiktok.url.request}")
    String baseUrl;

    @Value("${app.tiktok.app.key}")
    String appKey;

    @Value("${app.tiktok.app.secret}")
    String appSecret;


    public TiktokApiResponse get(String path, String accessToken, Map<String, String> queryParams) {
        return execute("GET", path, accessToken, queryParams, null);
    }

    public TiktokApiResponse post(String path, String accessToken, Map<String, String> queryParams, String jsonBody) {
        return execute("POST", path, accessToken, queryParams, jsonBody);
    }

    public TiktokApiResponse put(String path, String accessToken, Map<String, String> queryParams, String jsonBody) {
        return execute("PUT", path, accessToken, queryParams, jsonBody);
    }
    public TiktokApiResponse delete(String path, String accessToken, Map<String, String> queryParams, String jsonBody) {
        return execute("DELETE", path, accessToken, queryParams, jsonBody );
    }


    private TiktokApiResponse execute(String method, String path, String accessToken, Map<String, String> queryParams, String jsonBody) {
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> params = new TreeMap<>();
        params.put("app_key", appKey);
        params.put("timestamp", String.valueOf(timestamp));
        if (queryParams != null) params.putAll(queryParams);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + path).newBuilder();
        params.forEach(urlBuilder::addQueryParameter);

        RequestBody body = null;
        if ("POST".equalsIgnoreCase(method)  || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)   && jsonBody != null) {
            body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        }

        Request unsigned = new Request.Builder()
                .url(urlBuilder.build())
                .method(method, body)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-tts-access-token", accessToken)
                .build();

        String sign = signatureUtil.generateSignature(unsigned, appSecret);
        HttpUrl signedUrl = unsigned.url().newBuilder().addQueryParameter("sign", sign).build();

        Request signedRequest = unsigned.newBuilder().url(signedUrl).build();

        try (Response response = okHttpClient.newCall(signedRequest).execute()) {

            if (!response.isSuccessful()) {
                log.error("Failed : HTTP {}", response.body().string());
                throw TiktokException.builder()
                        .code(400)
                        .message(response.body().string())
                        .build();
            }

            TiktokApiResponse res = objectMapper.readValue(response.body().string(), TiktokApiResponse.class);
            if(res.getCode() != 0){
                log.error("{}", res.getCode());
                throw TiktokException.builder()
                        .code(res.getCode())
                        .message(res.getMessage())
                        .build();
            }
            return res;
        } catch (IOException  e) {
            log.error("IO: {}", e.getMessage(), e);
            throw new TiktokException(500, "Lỗi hệ thống!");
        }
    }


}
