// === CommentResponse.java ===
package com.cloud1pm.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private String authorName;
    private String authorProfileImage;
    private LocalDateTime createdAt;
}