package com.cloud1pm.backend.service;

import com.cloud1pm.backend.dto.*;
import com.cloud1pm.backend.entity.*;
import com.cloud1pm.backend.repository.*;
import com.cloud1pm.backend.security.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RiskSolutionRepository riskSolutionRepository;
    private final EncouragementMessageRepository encouragementMessageRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserProfileResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        // riskSolutions가 있으면 true, 없으면 false (나중에 설정)
        boolean hasInitialSetup = request.getRiskSolutions() != null && !request.getRiskSolutions().isEmpty();

        // 닉네임 설정 (없을 때 이메일 접두사 사용)
        String nickname = Optional.ofNullable(request.getNickname())
                .filter(n -> !n.isBlank())
                .orElseGet(() -> request.getEmail().split("@")[0]);

        // Username 설정
        String defaultId = request.getEmail().split("@")[0];
        String username = Optional.ofNullable(request.getUsername())
                .filter(u -> !u.isBlank())
                .orElse(defaultId);

        String profileImageUrl = Optional.ofNullable(request.getProfileImageUrl())
                .filter(url -> !url.isBlank())
                .orElse(User.DEFAULT_PROFILE_IMAGE_URL);

        // User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(username)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .provider(ProviderType.LOCAL.name())
                .providerType(ProviderType.LOCAL)
                .providerId(username)
                .hasCompletedInitialSetup(hasInitialSetup) // 초기 설정 여부 반영
                .build();
        userRepository.save(user);

        // 초기 설정(위험도 해결방안)이 포함된 경우 저장
        if (hasInitialSetup) {
            request.getRiskSolutions().forEach(solution -> {
                if (solution.getRiskLevel() < 1 || solution.getRiskLevel() > 5) {
                    throw new IllegalArgumentException("Risk level must be between 1 and 5");
                }
                RiskSolution riskSolution = RiskSolution.builder()
                        .user(user)
                        .riskLevel(solution.getRiskLevel())
                        .solution(solution.getSolution())
                        .build();
                riskSolutionRepository.save(riskSolution);
            });
        }

        return UserProfileResponse.from(user);
    }

    @Transactional
    public void completeInitialSetup(Long userId, InitialSetupRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 기존 해결방안이 있다면 삭제 (재설정/수정 시 중복 방지)
        // RiskSolutionRepository에 deleteByUserId 메서드가 필요합니다.
        riskSolutionRepository.deleteByUserId(userId);

        // 새 해결방안 저장
        if (request.getRiskSolutions() != null) {
            request.getRiskSolutions().forEach(solution -> {
                if (solution.getRiskLevel() < 1 || solution.getRiskLevel() > 5) {
                    throw new IllegalArgumentException("Risk level must be between 1 and 5");
                }

                RiskSolution riskSolution = RiskSolution.builder()
                        .user(user)
                        .riskLevel(solution.getRiskLevel())
                        .solution(solution.getSolution())
                        .build();
                riskSolutionRepository.save(riskSolution);
            });
        }

        // 초기 설정 완료 상태로 변경
        user.setHasCompletedInitialSetup(true);
        userRepository.save(user);
    }

    // 로그인
    public String signIn(SignInRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Invalid username or password"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new EntityNotFoundException("Invalid username or password");
        }

        // 로그인 성공 시 연속 출석 체크
        user.checkConsecutiveLogin();
        userRepository.save(user);

        return jwtUtil.generateToken(user.getId(), user.getEmail());
    }

    @Transactional
    public void saveOrUpdateRiskSolutions(Long userId, InitialSetupRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 기존 해결방안 삭제 후 다시 저장 (수정) - deleteByUserId가 RiskSolutionRepository에 추가되어 해결
        riskSolutionRepository.deleteByUserId(userId);

        request.getRiskSolutions().forEach(solution -> {
            if (solution.getRiskLevel() < 1 || solution.getRiskLevel() > 5) {
                throw new IllegalArgumentException("Risk level must be between 1 and 5");
            }
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

    // 회원 정보 조회
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return UserProfileResponse.from(user);
    }

    // 회원 정보 수정 (닉네임, 사진): username은 수정 불가
    @Transactional
    public UserProfileResponse updateUserProfile(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Optional.ofNullable(request.getNickname())
                .filter(n -> !n.isBlank())
                .ifPresent(user::updateNickname);

        String profileImageUrl = Optional.ofNullable(request.getProfileImageUrl())
                .filter(url -> !url.isBlank())
                .orElse(User.DEFAULT_PROFILE_IMAGE_URL);
        user.updateProfileImageUrl(profileImageUrl);

        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    // 비밀번호 수정
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getPassword() == null) {
            throw new IllegalArgumentException("Social login user cannot change password.");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // 회원 탈퇴
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        riskSolutionRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<RiskSolutionResponse> getRiskSolutions(Long userId, Integer riskLevel) {
        List<RiskSolution> solutions;
        if (riskLevel != null) {
            solutions = riskSolutionRepository.findByUserIdAndRiskLevel(userId, riskLevel);
        } else {
            solutions = riskSolutionRepository.findByUserId(userId);
        }
        return solutions.stream()
                .map(this::convertToRiskSolutionResponse)
                .collect(Collectors.toList());
    }

    private RiskSolutionResponse convertToRiskSolutionResponse(RiskSolution solution) {
        return RiskSolutionResponse.builder()
                .id(solution.getId())
                .riskLevel(solution.getRiskLevel())
                .solution(solution.getSolution())
                .build();
    }

    @Transactional
    public void createEncouragementMessage(Long userId, CreateEncouragementMessageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();

        if (encouragementMessageRepository.findByUserIdAndDate(userId, today).isPresent()) {
            throw new RuntimeException("Today's encouragement message already exists");
        }

        EncouragementMessage encouragementMessage = EncouragementMessage.builder()
                .user(user)
                .message(request.getMessage())
                .emotion(request.getEmotion())
                .date(today)
                .build();
        encouragementMessageRepository.save(encouragementMessage);

        user.addRice(1);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<EncouragementMessageResponse> getEncouragementMessages(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        return encouragementMessageRepository.findByUserIdOrderByDateDesc(userId).stream()
                .map(EncouragementMessageResponse::from)
                .collect(Collectors.toList());
    }

      @Transactional
    public FeedCharacterResponse feedCharacter(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getRice() < 1) {
            return FeedCharacterResponse.builder()
                    .newRiceCount(user.getRice())
                    .newLevel(user.getCharacterLevel())
                    .message("밥이 부족합니다. 밥을 1개 이상 모아주세요!")
                    .build();
        }

        int currentLevel = user.getCharacterLevel();
        final int FEED_TO_LEVEL_UP = 5; 

        user.setRice(user.getRice() - 1);
        user.setFeedCount(user.getFeedCount() + 1);

        String message;
        

        if (user.getFeedCount() >= FEED_TO_LEVEL_UP) {
            user.setCharacterLevel(currentLevel + 1);
            user.setFeedCount(0); 
            message = String.format("🎉 축하합니다! 캐릭터가 레벨 %d로 성장했습니다!", user.getCharacterLevel());
        } else {
            int remaining = FEED_TO_LEVEL_UP - user.getFeedCount();
            message = String.format("냠냠! 맛있게 먹었어요 😋 다음 레벨업까지 %d번 남았습니다.", remaining);
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

    private int getRequiredRiceForLevel(int level) {
        if (level <= 1) return 10;
        if (level <= 5) return 20 + (level - 1) * 5;
        if (level <= 10) return 40 + (level - 5) * 10;
        return 100;
    }

    // 댓글 작성 시 밥 지급 (하루 10개 제한)
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

    // 좋아요 클릭 시 밥 지급 (하루 10개 제한)
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

    // 연속 출석 보상 (로그인 시 호출됨)
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
}