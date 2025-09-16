package com.petd.paymentcrawl.sdk;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES)) // tối đa 50 connection, giữ 5 phút
                .connectTimeout(5, TimeUnit.SECONDS) // timeout kết nối
                .readTimeout(10, TimeUnit.SECONDS)   // timeout đọc dữ liệu
                .writeTimeout(10, TimeUnit.SECONDS)  // timeout ghi dữ liệu
                .retryOnConnectionFailure(true)      // tự retry khi mất kết nối
                .build();
    }
}
