package com.cloud1pm.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    public static final String DEFAULT_PROFILE_IMAGE_URL = "/default/profile.png";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false, length = 50)
    private String nickname; // 닉네임 (수정 가능)

    // OAuth로 전달되는 사용자의 이름 (Google 닉네임) 또는 일반 로그인 시 ID
    @Column(length = 255, unique = true)
    private String username;

    // 소셜 로그인 필드 추가
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType providerType;

    private String profileImageUrl;

    @Column(nullable = false)
    private String provider; // google

    @Column(nullable = false, unique = true)
    private String providerId;

    @Builder.Default
    @Column(nullable = false)
    private Integer rice = 0; // 밥 수량

    @Builder.Default
    @Column(nullable = false)
    private Integer characterLevel = 1; // 캐릭터 레벨

    @Builder.Default
    @Column(nullable = false)
    private Integer feedCount = 0; // 밥 먹인 횟수

    @Builder.Default
    @Column(nullable = false)
    private Integer consecutiveDays = 0; // 연속 출석 일수

    private LocalDate lastLoginDate; // 마지막 로그인 날짜

    @Builder.Default
    @Column(nullable = false)
    private Boolean hasCompletedInitialSetup = false; // 초기 설정 완료 여부

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // OAuth 로그인으로 User를 생성하거나 업데이트하는 메서드
    public User updateOAuthInfo(String username, String email) {
        this.username = username;
        this.email = email;
        // 닉네임이 설정되지 않았다면 username으로 설정 (초기 설정 페이지에서 변경 가능)
        if (this.nickname == null || this.nickname.isEmpty()) {
            this.nickname = username; // [수정] name -> username
        }
        return this;
    }

    // 밥 추가
    public void addRice(int amount) {
        this.rice += amount;
    }

    // 밥 먹이기 (10번당 레벨업)
    public void feedCharacter() {
        this.feedCount++;
        if (this.feedCount % 10 == 0) {
            this.characterLevel++;
        }
    }

    // 연속 출석 체크
    public void checkConsecutiveLogin() {
        LocalDate today = LocalDate.now();
        if (lastLoginDate == null) {
            consecutiveDays = 1;
        } else if (lastLoginDate.plusDays(1).equals(today)) {
            consecutiveDays++;
        } else if (!lastLoginDate.equals(today)) {
            consecutiveDays = 1;
        }
        lastLoginDate = today;
    }

    // 닉네임 수정 (ID는 수정 불가)
    public void updateNickname(String nickname) { // [수정] updateName -> updateNickname
        this.nickname = nickname; // [수정] name -> nickname
    }

    // 프로필 이미지 수정
    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}