package com.example.Evaluation.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String questionText;
    private String options; // Comma separated or JSON
    private String correctOption;
    private Integer points;
}
