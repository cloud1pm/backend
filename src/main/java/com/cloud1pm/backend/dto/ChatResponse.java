// === ChatResponse.java ===
package com.cloud1pm.backend.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String sentiment;
    private Integer riskLevel;
}