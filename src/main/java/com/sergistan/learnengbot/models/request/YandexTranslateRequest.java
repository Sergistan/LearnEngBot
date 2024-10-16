package com.sergistan.learnengbot.models.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class YandexTranslateRequest {
    private String sourceLanguageCode;
    private String targetLanguageCode;
    @Value("${format_request}")
    private String format;
    private List<String> texts;
    @Value("${folderId_request}")
    private String folderId;
    private Boolean speller;
}
