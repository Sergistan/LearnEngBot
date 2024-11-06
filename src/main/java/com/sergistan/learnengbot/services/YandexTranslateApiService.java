package com.sergistan.learnengbot.services;

import com.atlascopco.hunspell.Hunspell;
import com.sergistan.learnengbot.models.request.YandexTranslateRequest;
import com.sergistan.learnengbot.models.response.YandexTranslateResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

@Service
public class YandexTranslateApiService {

    @Autowired
    public YandexTranslateApiService(RestTemplate restTemplate, @Value("${yandex_api_token}") String yandexApiToken, @Value("${yandex_url}") String url,
                                     @Value("${format_request}") String format, @Value("${folderId_request}") String folderId) {
        this.restTemplate = restTemplate;
        this.yandexApiToken = yandexApiToken;
        this.url = url;
        this.format = format;
        this.folderId = folderId;
    }

    private final RestTemplate restTemplate;
    private final String yandexApiToken;
    private final String url;
    private final String format;
    private final String folderId;
    private Hunspell dictionary;

    @PostConstruct
    public void init() {
        try {
            // Загружаем файлы словарей из ресурсов во временные файлы
            Path dicTemp = Files.createTempFile("ru_RU", ".dic");
            Path affTemp = Files.createTempFile("ru_RU", ".aff");

            // Копируем содержимое ресурсов во временные файлы
            try (var dicStream = getClass().getResourceAsStream("/hunspell/ru_RU.dic");
                 var affStream = getClass().getResourceAsStream("/hunspell/ru_RU.aff")) {
                if (dicStream != null && affStream != null) {
                    Files.copy(dicStream, dicTemp, StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(affStream, affTemp, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    throw new IOException("Словари не найдены в ресурсах.");
                }
            }

            // Инициализируем Hunspell с временными файлами
            dictionary = new Hunspell(dicTemp.toString(), affTemp.toString());

        } catch (IOException e) {
            throw new RuntimeException("Ошибка инициализации словаря Hunspell", e);
        }
    }

    public String translateRuToEn(String text) {
        if (!isOneRussianWord(text) || !isRussianWordExistByDictionary(text)) {
            return """
                    Ваше введенное слово не подходит!
                    Нужно ввести русское существующее слово!
                    """;
        }
        return translateWord(text, "ru", "en");
    }

    public String translateEnToRu(String randomWord) {
        return translateWord(randomWord, "en", "ru");
    }

    private String translateWord(String word, String sourceLang, String targetLang) {
        try {
            YandexTranslateRequest request = createYandexTranslateRequest(word, sourceLang, targetLang);
            HttpHeaders headers = createHttpHeaders();
            return sendTranslationRequest(request, headers);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while translating: " + e.getMessage(), e);
        }
    }

    private YandexTranslateRequest createYandexTranslateRequest(String word, String sourceLanguage, String targetLanguage) {
        return YandexTranslateRequest.builder()
                .sourceLanguageCode(sourceLanguage)
                .targetLanguageCode(targetLanguage)
                .format(format)
                .folderId(folderId)
                .texts(List.of(word))
                .speller(true)
                .build();
    }

    private HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Api-Key " + yandexApiToken);
        headers.set("Content-type", "application/json");
        return headers;
    }

    private String sendTranslationRequest(YandexTranslateRequest request, HttpHeaders headers){
        HttpEntity<YandexTranslateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<YandexTranslateResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, YandexTranslateResponse.class);
        return Objects.requireNonNull(response.getBody()).getTranslations().get(0).getText();
    }

    private boolean isOneRussianWord(String word) {
        return word.matches("[А-Яа-яЁё]+(-[А-Яа-яЁё]+)*");
    }

    @Transactional
    public boolean isRussianWordExistByDictionary(String word) {
        return dictionary.isCorrect(word);
    }
}
