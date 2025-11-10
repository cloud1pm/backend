// === PostResponse.java ===
package com.cloud1pm.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String authorName;
    private String authorProfileImage;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;
}