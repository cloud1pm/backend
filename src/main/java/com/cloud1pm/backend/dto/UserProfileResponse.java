package com.cloud1pm.backend.dto;

import com.cloud1pm.backend.entity.User;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String provider;
    private Boolean hasCompletedInitialSetup;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .provider(user.getProvider())
                .hasCompletedInitialSetup(user.getHasCompletedInitialSetup())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}