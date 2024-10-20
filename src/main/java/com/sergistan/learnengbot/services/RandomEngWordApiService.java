package com.sergistan.learnengbot.services;

import com.google.gson.Gson;
import com.sergistan.learnengbot.models.response.RandomWordResponse;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class RandomEngWordApiService {

    @Autowired
    public RandomEngWordApiService(@Value("${random_eng_api_token}") String randomEngApiToken, @Value("${random_eng_api_url}") String randomEngApiUrl, Gson gson) {
        this.randomEngApiToken = randomEngApiToken;
        this.randomEngApiUrl = randomEngApiUrl;
        this.gson = gson;
    }

    private final String randomEngApiToken;
    private final String randomEngApiUrl;
    private final Gson gson;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String getRandomEnglishWord(String partOfSpeech) {
        try {
            URI uri = buildUri(partOfSpeech);
            HttpRequest request = buildHttpRequest(uri);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response.body());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException("Error while fetching random English word: " + e.getMessage(), e);
        }
    }

    private URI buildUri(String partOfSpeech) throws URISyntaxException {
        return new URIBuilder(randomEngApiUrl)
                .addParameter("type", partOfSpeech)
                .build();
    }

    private HttpRequest buildHttpRequest(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .header("x-API-key", randomEngApiToken)
                .build();
    }

    private String parseResponse(String responseBody) {
        RandomWordResponse randomWordResponse = gson.fromJson(responseBody, RandomWordResponse.class);
        return randomWordResponse.getWord().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No word found in response"));
    }
}
