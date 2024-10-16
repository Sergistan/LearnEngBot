package com.sergistan.learnengbot.services;

import com.sergistan.learnengbot.models.User;
import com.sergistan.learnengbot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void createUser (Long chatId, String username){
        Optional<User> user = userRepository.findByChatIdAndUsername(chatId, username);
        if (user.isEmpty()){
            User userBuild = User.builder()
                    .chatId(chatId)
                    .username(username)
                    .words(new HashSet<>())
                    .build();
            userRepository.save(userBuild);
        }
    }
}
