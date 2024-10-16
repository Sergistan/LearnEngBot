package com.sergistan.learnengbot.services;

import com.sergistan.learnengbot.models.User;
import com.sergistan.learnengbot.models.Word;
import com.sergistan.learnengbot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final TelegramBotService telegramBotService;

    @Transactional
    @Scheduled(cron = "0 0 13 * * ?")
    public void sendDailyWordReminder() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            List<Word> userWords = new ArrayList<>(user.getWords());
            if (!userWords.isEmpty()) {
                sendNotification(user, userWords);
            }
        }
    }

    private void sendNotification(User user, List<Word> words) {
        StringBuilder message = new StringBuilder("Пора повторить следующие слова: \n");
        for (Word word : words) {
            message.append(word.getRussianWord()).append(" - ").append(word.getEnglishWord()).append("\n");
        }
        telegramBotService.sendMessage(user.getChatId(), message.toString());
    }

}
