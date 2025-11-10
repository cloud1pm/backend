// === CreatePostRequest.java ===
package com.cloud1pm.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {
    private String title;
    private String content;
}