// === EmotionTrendResponse.java ===
package com.cloud1pm.backend.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmotionTrendResponse {
    private List<DailyEmotion> trends;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyEmotion {
        private LocalDate date;
        private String sentiment;
        private Double averageScore;
    }
}