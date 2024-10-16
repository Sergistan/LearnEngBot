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

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Service
public class YandexTranslateApiService {

    @Autowired
    public YandexTranslateApiService(RestTemplate restTemplate, @Value("${yandex_token}") String yandexToken, @Value("${yandex_url}") String url,
                                     @Value("${format_request}") String format, @Value("${folderId_request}") String folderId) {
        this.restTemplate = restTemplate;
        this.yandexToken = yandexToken;
        this.url = url;
        this.format = format;
        this.folderId = folderId;
    }

    private final RestTemplate restTemplate;
    private final String yandexToken;
    private final String url;
    private final String format;
    private final String folderId;
    private Hunspell dictionary;

    public String translateRuToEn(String text) {
        if (!isOneRussianWord(text) || !isRussianWordExistByDictionary(text)) {
            return """
                    Ваше введенное слово не подходит!
                    Нужно ввести русское существующее слово!
                    """;
        }

        YandexTranslateRequest yandexTranslateRequest = createYandexTranslateRequest(text, "ru", "en");

        HttpHeaders httpHeaders = createHttpHeaders();

        return getTranslateWordFromRequest(yandexTranslateRequest, httpHeaders);
    }


    public String translateEnToRu(String randomWord) {

        YandexTranslateRequest yandexTranslateRequest = createYandexTranslateRequest(randomWord, "en", "ru");

        HttpHeaders httpHeaders = createHttpHeaders();

        return getTranslateWordFromRequest(yandexTranslateRequest, httpHeaders);
    }


    private YandexTranslateRequest createYandexTranslateRequest(String randomWord, String sourceLanguage, String targetLanguage) {
        return YandexTranslateRequest.
                builder()
                .sourceLanguageCode(sourceLanguage)
                .targetLanguageCode(targetLanguage)
                .format(format)
                .folderId(folderId)
                .texts(List.of(randomWord))
                .speller(true)
                .build();
    }

    private HttpHeaders createHttpHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization", "Bearer " + yandexToken);
        httpHeaders.set("Content-type", "application/json");
        return httpHeaders;
    }

    private String getTranslateWordFromRequest(YandexTranslateRequest yandexTranslateRequest, HttpHeaders httpHeaders) {
        HttpEntity<YandexTranslateRequest> httpEntity = new HttpEntity<>(yandexTranslateRequest, httpHeaders);

        ResponseEntity<YandexTranslateResponse> exchange = restTemplate.exchange(url, HttpMethod.POST, httpEntity, YandexTranslateResponse.class);

        return Objects.requireNonNull(exchange.getBody()).getTranslations().get(0).getText();
    }

    private boolean isOneRussianWord(String word) {
        // Русское слово состоит из одного слово, либо содержит дефис
        return word.matches("[А-Яа-яЁё]+(-[А-Яа-яЁё]+)*");
    }


        @PostConstruct
        public void init() {
            // Инициализируем словарь при старте приложения
            String dicPath = Paths.get("src/main/resources/hunspell/ru_RU.dic").toString();
            String affPath = Paths.get("src/main/resources/hunspell/ru_RU.aff").toString();
            dictionary = new Hunspell(dicPath, affPath);
        }

        @Transactional
        public boolean isRussianWordExistByDictionary(String word) {
            // Проверяем правильность слова
            return dictionary.isCorrect(word);
        }

}
