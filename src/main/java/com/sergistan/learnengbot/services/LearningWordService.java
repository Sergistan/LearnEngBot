package com.sergistan.learnengbot.services;

import com.sergistan.learnengbot.exceptions.ServiceException;
import com.sergistan.learnengbot.models.User;
import com.sergistan.learnengbot.models.Word;
import com.sergistan.learnengbot.repositories.UserRepository;
import com.sergistan.learnengbot.repositories.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LearningWordService {

    private final WordRepository wordRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveWord(Long chatId, String userName, String wordWithTranslate) {
        String[] splitWord = wordWithTranslate.split("-");
        if (Arrays.stream(splitWord).count() == 2) {
            Optional<User> userOptional = userRepository.findByChatIdAndUsername(chatId, userName);
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                String russianWord = Arrays.stream(splitWord).findFirst().get();
                String englishWord = Arrays.stream(splitWord).skip(1).findFirst().get();

                Optional<Word> wordOptional = wordRepository.findByRussianWordAndEnglishWord(russianWord, englishWord);

                Word word;
                if (wordOptional.isPresent()) {
                    if (user.getWords().contains(wordOptional.get())) {
                        return;
                    } else {
                        word = wordOptional.get();
                    }
                } else {
                    word = Word.builder()
                            .russianWord(russianWord)
                            .englishWord(englishWord)
                            .build();
                    wordRepository.save(word);
                }
                user.getWords().add(word);
                userRepository.save(user);
            } else {
                throw new ServiceException("User not found");
            }
        } else {
            throw new ServiceException("Incorrect translation: The text-translation structure cannot be detected.");
        }
    }

    @Transactional
    public void deleteWordIfItWasAddedEarlier(Long chatId, String userName, String wordWithTranslate) {
        String[] splitWord = wordWithTranslate.split("-");
        if (Arrays.stream(splitWord).count() == 2) {
            Optional<User> userOptional = userRepository.findByChatIdAndUsername(chatId, userName);
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                String russianWord = Arrays.stream(splitWord).findFirst().get();
                String englishWord = Arrays.stream(splitWord).skip(1).findFirst().get();

                Optional<Word> wordOptional = wordRepository.findByRussianWordAndEnglishWord(russianWord, englishWord);

                if (wordOptional.isPresent()) {
                    Word word = wordOptional.get();
                    if (user.getWords().contains(wordOptional.get())) {
                        user.getWords().remove(word);
                        if (wordRepository.checkUsersAssociatedWithThisWord(word.getId()).isEmpty()) {
                            wordRepository.delete(word);
                        }
                    }
                } else {
                    return;
                }
            } else {
                throw new ServiceException("User not found");
            }
        } else {
            throw new ServiceException("Incorrect translation: The text-translation structure cannot be detected.");
        }

    }
    @Transactional
    public String workoutWords(Long chatId, String username) {
        Optional<User> userOptional = userRepository.findByChatIdAndUsername(chatId, username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Set<Word> words = user.getWords();
            if (user.getWords().isEmpty()){
                return  "У вас не добавлены слова для повторения!";
            } else{
                StringBuilder message = new StringBuilder("Вы сохранили следующие слова для повторения: \n");
                for (Word word : words) {
                    message.append(word.getRussianWord()).append(" - ").append(word.getEnglishWord()).append("\n");
                }
                return message.toString();
            }
        } else {
            throw new ServiceException("User not found");
        }

    }
}
