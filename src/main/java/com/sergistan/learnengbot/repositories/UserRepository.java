package com.sergistan.learnengbot.repositories;

import com.sergistan.learnengbot.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByChatIdAndUsername(Long chatId, String username);

}
