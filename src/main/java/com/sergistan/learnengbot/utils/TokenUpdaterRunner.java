package com.sergistan.learnengbot.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenUpdaterRunner implements CommandLineRunner {
    private final YandexTokenUpdater yandexTokenUpdater;

    @Override
    public void run(String... args) throws Exception {
        yandexTokenUpdater.checkAndUpdateToken();  // Проверка и обновление токена при запуске приложения
    }
}
