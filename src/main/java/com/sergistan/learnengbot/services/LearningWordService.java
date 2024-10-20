package com.sergistan.learnengbot.services;

import com.sergistan.learnengbot.exceptions.ServiceException;
import com.sergistan.learnengbot.models.User;
import com.sergistan.learnengbot.models.Word;
import com.sergistan.learnengbot.repositories.UserRepository;
import com.sergistan.learnengbot.repositories.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningWordService {

    private final WordRepository wordRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveWord(Long chatId, String userName, String wordWithTranslate) {
        String[] splitWord = validateWordWithTranslate(wordWithTranslate);
        String russianWord = splitWord[0].trim();
        String englishWord = splitWord[1].trim();
        User user = getUserByChatIdAndUsername(chatId, userName);

        Word word = wordRepository.findByRussianWordAndEnglishWord(russianWord, englishWord)
                .orElseGet(() -> createAndSaveWord(russianWord, englishWord));

        if (user.getWords().contains(word)) {
            return;
        }

        user.getWords().add(word);
        userRepository.save(user);
    }

    @Transactional
    public void deleteWordIfItWasAddedEarlier(Long chatId, String userName, String wordWithTranslate) {
        String[] splitWord = validateWordWithTranslate(wordWithTranslate);
        String russianWord = splitWord[0].trim();
        String englishWord = splitWord[1].trim();

        User user = getUserByChatIdAndUsername(chatId, userName);

        wordRepository.findByRussianWordAndEnglishWord(russianWord, englishWord).ifPresent(word -> {
            if (user.getWords().remove(word)) {
                if (wordRepository.checkUsersAssociatedWithThisWord(word.getId()).isEmpty()) {
                    wordRepository.delete(word);
                }
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public String workoutWords(Long chatId, String username) {
        User user = getUserByChatIdAndUsername(chatId, username);

        Set<Word> words = user.getWords();
        if (words.isEmpty()) {
            return "У вас не добавлены слова для повторения!";
        }

        return words.stream()
                .map(word -> word.getRussianWord() + " - " + word.getEnglishWord())
                .collect(Collectors.joining("\n", "Вы сохранили следующие слова для повторения:\n", ""));
    }

    @Transactional
    public String manualDeletingWord(Long chatId, String userName, String englishWord) {
        return wordRepository.findByEnglishWordIgnoreCase(englishWord)
                .map(foundWord -> {
                    User user = getUserByChatIdAndUsername(chatId, userName);
                    if (user.getWords().contains(foundWord)) {
                        user.getWords().remove(foundWord);
                        wordRepository.delete(foundWord);
                        return "Слово %s было удалено из вашего списка".formatted(englishWord);
                    } else {
                        return "Слово %s не было ранее у вас сохранено".formatted(englishWord);
                    }
                })
                .orElse("Слово %s нет в базе данных".formatted(englishWord));
    }

    // Вспомогательный метод для проверки структуры строки
    private String[] validateWordWithTranslate(String wordWithTranslate) {
        String[] splitWord = wordWithTranslate.split("-");
        if (splitWord.length != 2) {
            throw new ServiceException("Incorrect translation: The text-translation structure cannot be detected.");
        }
        return splitWord;
    }

    // Вспомогательный метод для получения пользователя
    private User getUserByChatIdAndUsername(Long chatId, String username) {
        return userRepository.findByChatIdAndUsername(chatId, username)
                .orElseThrow(() -> new ServiceException("User not found"));
    }

    // Вспомогательный метод для создания и сохранения нового слова
    private Word createAndSaveWord(String russianWord, String englishWord) {
        Word word = Word.builder()
                .russianWord(russianWord)
                .englishWord(englishWord)
                .build();
        return wordRepository.save(word);
    }
}
