package com.sergistan.learnengbot.repositories;

import com.sergistan.learnengbot.models.YandexToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YandexTokenRepository extends JpaRepository<YandexToken, Long> {
    Optional<YandexToken> findTopByOrderByUpdatedAtDesc();
}
