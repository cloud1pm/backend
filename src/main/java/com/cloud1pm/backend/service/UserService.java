package com.cloud1pm.backend.service;

import com.cloud1pm.backend.dto.*;
import com.cloud1pm.backend.entity.*;
import com.cloud1pm.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors; // 추가

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RiskSolutionRepository riskSolutionRepository;
    private final EncouragementMessageRepository encouragementMessageRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional
    public void completeInitialSetup(Long userId, InitialSetupRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 위험도별 해결방안 저장
        request.getRiskSolutions().forEach(solution -> {
            RiskSolution riskSolution = RiskSolution.builder()
                    .user(user)
                    .riskLevel(solution.getRiskLevel())
                    .solution(solution.getSolution())
                    .build();
            riskSolutionRepository.save(riskSolution);
        });

        user.setHasCompletedInitialSetup(true);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    // [수정] 반환 타입을 List<RiskSolution>에서 List<RiskSolutionResponse>로 변경
    public List<RiskSolutionResponse> getRiskSolutions(Long userId, Integer riskLevel) {
        List<RiskSolution> solutions;
        if (riskLevel != null) {
            solutions = riskSolutionRepository.findByUserIdAndRiskLevel(userId, riskLevel);
        } else {
            solutions = riskSolutionRepository.findByUserId(userId);
        }

        // [추가] Entity를 DTO로 변환하여 반환
        return solutions.stream()
                .map(this::convertToRiskSolutionResponse)
                .collect(Collectors.toList());
    }

    // [추가] RiskSolution Entity를 RiskSolutionResponse DTO로 변환하는 private 메서드
    private RiskSolutionResponse convertToRiskSolutionResponse(RiskSolution solution) {
        return RiskSolutionResponse.builder()
                .id(solution.getId())
                .riskLevel(solution.getRiskLevel())
                .solution(solution.getSolution())
                .build();
    }

    @Transactional
    public void createEncouragementMessage(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();

        // 오늘 이미 작성했는지 확인
        if (encouragementMessageRepository.findByUserIdAndDate(userId, today).isPresent()) {
            throw new RuntimeException("Today's encouragement message already exists");
        }

        EncouragementMessage encouragementMessage = EncouragementMessage.builder()
                .user(user)
                .message(message)
                .date(today)
                .build();
        encouragementMessageRepository.save(encouragementMessage);

        // 밥 +1
        user.addRice(1);
        userRepository.save(user);
    }

    // 응원 문구 전체 가져오기 (추가됨)
    @Transactional(readOnly = true)
    public List<EncouragementMessage> getEncouragementMessages(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        return encouragementMessageRepository.findByUserIdOrderByDateDesc(userId);
    }

    @Transactional
    public FeedCharacterResponse feedCharacter(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        int currentLevel = user.getCharacterLevel();
        int requiredRice = getRequiredRiceForLevel(currentLevel);
        final int FEED_TO_LEVEL_UP = 5; // 레벨업에 필요한 밥 주기 횟수

        if (user.getRice() < requiredRice) {
            return FeedCharacterResponse.builder()
                    .newRiceCount(user.getRice())
                    .newLevel(currentLevel)
                    .message(String.format("밥이 부족합니다. 레벨 %d에서 필요한 밥은 %d개입니다. (보유: %d)", currentLevel, requiredRice, user.getRice()))
                    .build();
        }

        // 밥 소모
        user.setRice(user.getRice() - requiredRice);

        // 경험치 증가 (feed_count)
        user.setFeedCount(user.getFeedCount() + 1);

        String message;
        if (user.getFeedCount() >= FEED_TO_LEVEL_UP) {
            // 레벨업 처리
            user.setCharacterLevel(currentLevel + 1);
            user.setFeedCount(0); // 레벨업 후 카운트 초기화
            message = String.format("🎉 캐릭터가 레벨 %d로 성장했습니다! 레벨업 축하 메시지.", user.getCharacterLevel());
        } else {
            message = String.format("밥 %d개를 성공적으로 먹였습니다. 다음 레벨업까지 %d번 남았습니다.", requiredRice, FEED_TO_LEVEL_UP - user.getFeedCount());
        }

        userRepository.save(user);

        return FeedCharacterResponse.builder()
                .newRiceCount(user.getRice())
                .newLevel(user.getCharacterLevel())
                .message(message)
                .build();
    }

    @Transactional(readOnly = true)
    public UserStatusResponse getUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserStatusResponse.builder()
                .rice(user.getRice())
                .characterLevel(user.getCharacterLevel())
                .feedCount(user.getFeedCount())
                .consecutiveDays(user.getConsecutiveDays())
                .hasCompletedInitialSetup(user.getHasCompletedInitialSetup())
                .build();
    }

    @Transactional
    public void addRiceForComment(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long todayComments = commentRepository.countTodayCommentsByUser(userId, startOfDay);

        if (todayComments <= 10) { // 하루 10개까지만 보상
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.addRice(1);
            userRepository.save(user);
        }
    }

    @Transactional
    public void addRiceForLike(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long todayLikes = postLikeRepository.countTodayLikesByUser(userId, startOfDay);

        if (todayLikes <= 10) { // 하루 10개까지만 보상
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.addRice(1);
            userRepository.save(user);
        }
    }

    @Transactional
    public void addRiceForConsecutiveLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getConsecutiveDays() > 0 && user.getConsecutiveDays() % 7 == 0) {
            user.addRice(5); // 7일 연속 출석 보너스
        } else {
            user.addRice(1); // 일반 출석 보상
        }
        userRepository.save(user);
    }

    private int getRequiredRiceForLevel(int level) {
        final int BASE_RICE = 1;

        if (level <= 1) return BASE_RICE;

        // 밥 요구량 증가 로직 (예시: 레벨이 높을수록 가파르게 증가)
        if (level <= 5) return level * BASE_RICE;
        if (level <= 10) return 5 + (level - 5) * 2;
        if (level <= 20) return 15 + (level - 10) * 3;

        return 45 + (level - 20) * 5;
    }
}