package com.sergistan.learnengbot.utils;

import com.sergistan.learnengbot.models.YandexToken;
import com.sergistan.learnengbot.repositories.YandexTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class YandexTokenUpdater {
    private final YandexTokenRepository tokenRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public YandexTokenUpdater(YandexTokenRepository tokenRepository, RestTemplate restTemplate) {
        this.tokenRepository = tokenRepository;
        this.restTemplate = restTemplate;
    }

    // Метод, который проверяет и обновляет токен
    @Scheduled(cron = "0 0 0 * * ?") // Запускается каждый день в полночь
    public void checkAndUpdateToken() throws InterruptedException, IOException {
        // Проверка, нужно ли обновлять токен
        if (shouldUpdateToken()) {
            String newToken = updateYandexToken(); // Получаем новый токен
            saveNewToken(newToken);               // Сохраняем его в базу данных
        }
    }

    // Метод для проверки, прошло ли более 24 часов с последнего обновления
    private boolean shouldUpdateToken() {
        Optional<YandexToken> optionalToken = tokenRepository.findTopByOrderByUpdatedAtDesc();

        // Если токен отсутствует в базе данных, или последний токен был обновлен более 24 часов назад, обновляем токен
        if (optionalToken.isEmpty()) {
            return true; // Токен отсутствует, требуется обновление
        }

        YandexToken lastToken = optionalToken.get();
        LocalDateTime lastUpdate = lastToken.getUpdatedAt();
        LocalDateTime now = LocalDateTime.now();

        // Проверка, прошло ли более 24 часов с последнего обновления
        return Duration.between(lastUpdate, now).toHours() >= 24;
    }

    // Метод для обновления токена (например, вызов PowerShell или другого API)
    private String updateYandexToken() throws IOException, InterruptedException {
        // Команда для выполнения PowerShell скрипта (замените путь к скрипту)
        String[] command = {"powershell.exe", "-ExecutionPolicy", "Bypass", "-File", "src/main/resources/update-yandex-token.ps1"};

        // Запуск процесса для выполнения команды
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        // Чтение вывода команды
        StringBuilder output = new StringBuilder();
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        // Ожидание завершения процесса
        process.waitFor();

        // Возвращаем токен, убирая любые лишние символы
        return output.toString().trim();
    }

    // Сохранение нового токена в базу данных
    private void saveNewToken(String newToken) {
        YandexToken token = new YandexToken();
        token.setToken(newToken);
        token.setUpdatedAt(LocalDateTime.now()); // Устанавливаем текущее время как время обновления
        tokenRepository.save(token);
    }
}


