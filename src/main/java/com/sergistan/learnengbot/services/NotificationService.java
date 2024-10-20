package com.sergistan.learnengbot.services;

import com.sergistan.learnengbot.models.User;
import com.sergistan.learnengbot.models.Word;
import com.sergistan.learnengbot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final TelegramBotService telegramBotService;

    @Transactional
    @Scheduled(cron = "0 0 13 * * ?")
    public void sendDailyWordReminder() {
        userRepository.findAll().stream()
                .filter(user -> !user.getWords().isEmpty()) // Отправляем только тем, у кого есть слова
                .forEach(this::sendNotification); // Отправляем уведомление для каждого пользователя
    }

    private void sendNotification(User user) {
        String message = buildMessage(user.getWords());
        telegramBotService.sendMessage(user.getChatId(), message);
    }

    private String buildMessage(Set<Word> words) {
        return words.stream()
                .map(word -> word.getRussianWord() + " - " + word.getEnglishWord())
                .collect(Collectors.joining("\n", "Пора повторить следующие слова: \n", ""));
    }

}
