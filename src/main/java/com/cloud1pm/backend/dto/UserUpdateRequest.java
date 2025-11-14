package com.cloud1pm.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String nickname;
    private String profileImageUrl;
}