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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

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
}