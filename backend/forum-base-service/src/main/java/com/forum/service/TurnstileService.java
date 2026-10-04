package com.forum.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TurnstileService {

    private static final Logger logger = LoggerFactory.getLogger(TurnstileService.class);

    @Value("${app.turnstile.secret-key:1x0000000000000000000000000000000AA}")
    private String secretKey;

    @Value("${app.turnstile.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String verifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyToken(String token) {
        if (!StringUtils.hasText(token)) {
            logger.warn("Turnstile token is empty or null");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", secretKey);
            body.add("response", token);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(verifyUrl, requestEntity, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                return true;
            } else {
                logger.warn("Turnstile verification failed. Response: {}", response);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error calling Cloudflare Turnstile API: {}", e.getMessage(), e);
            return false;
        }
    }

    public void verifyTokenOrThrow(String token) {
        if (!verifyToken(token)) {
            throw new IllegalArgumentException("Xác thực chống Bot (Cloudflare Turnstile) không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.");
        }
    }
}
