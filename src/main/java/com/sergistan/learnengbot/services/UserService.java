package com.sergistan.learnengbot.services;

import com.sergistan.learnengbot.models.User;
import com.sergistan.learnengbot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void createUser(Long chatId, String username) {
        userRepository.findByChatIdAndUsername(chatId, username)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .chatId(chatId)
                            .username(username)
                            .words(new HashSet<>())
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
