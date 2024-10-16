package com.sergistan.learnengbot.services;


import com.google.gson.Gson;
import com.sergistan.learnengbot.models.response.RandomWordResponse;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    public String getRandomEnglishWord(String partOfSpeech) throws URISyntaxException, IOException, InterruptedException {

        URIBuilder uriBuilder = new URIBuilder(randomEngApiUrl);
        uriBuilder.addParameter("type", partOfSpeech);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uriBuilder.build())
                .GET()
                .header("x-API-key", randomEngApiToken)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        RandomWordResponse randomWordResponse = gson.fromJson(response.body(), RandomWordResponse.class);

        return randomWordResponse.getWord().get(0);

    }
}
