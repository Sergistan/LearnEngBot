package com.sergistan.learnengbot.services;

import com.sergistan.learnengbot.models.YandexToken;
import com.sergistan.learnengbot.repositories.YandexTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class YandexTokenService {

    @Autowired
    public YandexTokenService(YandexTokenRepository yandexTokenRepository) {
        this.yandexTokenRepository = yandexTokenRepository;
    }

    private final YandexTokenRepository yandexTokenRepository;

    public String getCurrentToken() {
        // Получение самого свежего токена
        return yandexTokenRepository.findTopByOrderByUpdatedAtDesc().map(YandexToken::getToken)
                .orElseThrow(() -> new RuntimeException("Token not found"));
    }
}
