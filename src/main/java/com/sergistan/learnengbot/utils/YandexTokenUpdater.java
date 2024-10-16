package com.sergistan.learnengbot.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class YandexTokenUpdater {

    @Autowired
    public YandexTokenUpdater(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Путь к файлу для хранения времени последнего обновления токена
    private static final String LAST_UPDATE_FILE = "last_token_update.txt";

    // Путь к файлу .env
    private static final String ENV_FILE = ".env";

    private final RestTemplate restTemplate;

    // Actuator URL для перезапуска
    private static final String RESTART_ENDPOINT = "http://localhost:8080/actuator/restart";

    // Метод, который проверяет и обновляет токен
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkAndUpdateToken() throws InterruptedException {
        try {
            // Проверка, нужно ли обновлять токен
            if (shouldUpdateToken()) {
                updateYandexToken();
                updateLastUpdateTime();
                restartApplication();  // Перезапуск через Actuator
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для проверки, прошло ли более 24 часов с последнего обновления
    private boolean shouldUpdateToken() throws IOException {
        // Чтение файла с временем последнего обновления
        // Проверяем, существует ли файл
        Path path = Path.of(LAST_UPDATE_FILE);
        if (!Files.exists(path)) {
            // Если файл не существует, создаём его с текущим временем
            updateLastUpdateTime();
            return true; // Нужно обновить токен
        }

        // Чтение файла с временем последнего обновления
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            return true; // Если файл пустой, нужно обновить токен
        }

        LocalDateTime lastUpdate = LocalDateTime.parse(lines.get(0)); // Время последнего обновления
        LocalDateTime now = LocalDateTime.now();

        // Проверка, прошло ли более 24 часов с последнего обновления
        return Duration.between(lastUpdate, now).toHours() >= 24;

    }

    // Метод для обновления времени последнего обновления токена
    private void updateLastUpdateTime() throws IOException {
        // Запись текущего времени в файл
        Files.write(Paths.get(LAST_UPDATE_FILE), Collections.singletonList(LocalDateTime.now().toString()),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // Метод для обновления токена
    private void updateYandexToken() throws IOException, InterruptedException {
        // Здесь реализуйте логику получения нового токена, например, вызов PowerShell или другого API
        String newToken = getNewYandexToken(); // Полученный новый токен

        try {
            // Чтение содержимого файла .env
            Path path = Path.of(ENV_FILE);
            List<String> lines = Files.readAllLines(path);

            // Замена строки с YANDEX_TOKEN
            List<String> updatedLines = lines.stream()
                    .map(line -> {
                        if (line.startsWith("YANDEX_TOKEN")) {
                            return "YANDEX_TOKEN=" + newToken;
                        }
                        return line;
                    })
                    .collect(Collectors.toList());

            // Запись обновленного файла .env
            Files.write(path, updatedLines, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("Токен успешно обновлен в файле .env");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getNewYandexToken() throws IOException, InterruptedException {
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

    private void restartApplication() {
        try {
            // Отправка POST-запроса для перезапуска приложения через Actuator
            restTemplate.postForObject(RESTART_ENDPOINT, null, String.class);
            System.out.println("Приложение перезапускается через Spring Actuator.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


