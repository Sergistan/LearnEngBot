package com.sergistan.learnengbot.repositories;

import com.sergistan.learnengbot.models.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    Optional<Word> findByRussianWordAndEnglishWord(String russianWord, String englishWord);

    Optional<Word> findByEnglishWordIgnoreCase(String englishWord);

    @Query(value = "SELECT uw.user_id FROM user_words uw WHERE uw.word_id = ?1", nativeQuery = true)
    Optional<Long> checkUsersAssociatedWithThisWord(Long wordId);
}
