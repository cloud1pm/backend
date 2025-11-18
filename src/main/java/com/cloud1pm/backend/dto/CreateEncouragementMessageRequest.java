// === CreateEncouragementMessageRequest.java ===
package com.cloud1pm.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEncouragementMessageRequest {
    private String message;
    private String emotion; // ✨ 추가된 감정 필드
}