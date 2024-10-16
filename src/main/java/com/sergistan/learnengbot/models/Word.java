package com.sergistan.learnengbot.models;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Builder
@Table(name = "words")
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "russian_word", nullable = false)
    private String russianWord;

    @Column(name = "english_word", nullable = false)
    private String englishWord;
}
