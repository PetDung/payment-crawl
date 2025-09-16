package com.petd.paymentcrawl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class TelegramService {

    @Value("${bot.api-key}")
    String apikey;

    @Value("${bot.chat-id}")
    String chatId;


    public boolean sendMessage(String text) {
        OkHttpClient client = new OkHttpClient();

        try {
            // Encode text để tránh lỗi ký tự đặc biệt
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());

            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=HTML",
                    apikey, chatId, encodedText
            );

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Telegram API error: " + response.code());
                    return false;
                }
                String body = response.body() != null ? response.body().string() : "";
                return parseResponse(body);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Boolean parseResponse(String responseBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(
                responseBody,
                new TypeReference<Map<String, Object>>() {}
        );
        Object okeValue = responseMap.get("ok");
        boolean code;
        if (okeValue == null) {
            code = false; // Nếu không có "oke", coi như thất bại
        } else if (okeValue instanceof Boolean) {
            code = (boolean) okeValue; // Nếu là Boolean, ép kiểu trực tiếp
        } else if (okeValue instanceof String) {
            code = Boolean.parseBoolean((String) okeValue); // Nếu là String, parse thành boolean
        } else {
            code = false; // Các trường hợp khác, mặc định thất bại
        }
        return code;
    }
}
