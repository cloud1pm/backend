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
        if (request.getRiskSolutions() == null || request.getRiskSolutions().isEmpty()) {
            throw new IllegalArgumentException("Initial setup solutions are required for sign-up");
        }

        // 닉네임 (mutable) 설정 (없을 때 이메일 접두사 사용)
        String defaultId = request.getEmail().split("@")[0];

        // 닉네임 (mutable) 설정 (없을 때 이메일 접두사 사용)
        String nickname = Optional.ofNullable(request.getNickname()) // [수정] getName -> getNickname
                .filter(n -> !n.isBlank())
                .orElseGet(() -> request.getEmail().split("@")[0]);

        // Username (immutable ID) 설정: 이메일 접두사를 기본값으로 사용
        String username = Optional.ofNullable(request.getUsername()) // [수정] request.getUsername() 추가
                .filter(u -> !u.isBlank())
                .orElse(defaultId);

        // 기본 프로필 이미지 설정 (입력 없을 때)
        String profileImageUrl = Optional.ofNullable(request.getProfileImageUrl())
                .filter(url -> !url.isBlank())
                .orElse(User.DEFAULT_PROFILE_IMAGE_URL);

        // User 엔티티 생성 및 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(username)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .provider(ProviderType.LOCAL.name()) // 일반 로그인
                .providerType(ProviderType.LOCAL)
                .providerId(username) // 일반 로그인
                .hasCompletedInitialSetup(true)
                .build();
        userRepository.save(user);

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

        return UserProfileResponse.from(user);
    }

    // 로그인
    public String signIn(SignInRequest request) {
        User user = userRepository.findByUsername(request.getUsername()) // [수정] findByEmail -> findByUsername
                .orElseThrow(() -> new EntityNotFoundException("Invalid username or password")); // [수정] email -> username

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new EntityNotFoundException("Invalid username or password"); // [수정] email -> username
        }

        // 로그인 성공 시 연속 출석 체크
        user.checkConsecutiveLogin();
        userRepository.save(user);

        // JWT 토큰 생성 및 반환
        return jwtUtil.generateToken(user.getId(), user.getEmail());
    }

    // completeInitialSetup - 초기 설정 재설정/수정 기능으로 활용
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

        // 닉네임만 수정 가능
        Optional.ofNullable(request.getNickname()) // [수정] getName -> getNickname
                .filter(n -> !n.isBlank())
                .ifPresent(user::updateNickname); // [수정] updateName -> updateNickname

        // null을 허용하면 기본 이미지로 업데이트
        String profileImageUrl = Optional.ofNullable(request.getProfileImageUrl())
                .filter(url -> !url.isBlank())
                .orElse(User.DEFAULT_PROFILE_IMAGE_URL);
        user.updateProfileImageUrl(profileImageUrl);

        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    // [추가] 비밀번호 수정
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

    // [추가] 회원 탈퇴
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // 관련된 모든 데이터 삭제 (실제로는 논리적 삭제를 고려해야 합니다.)
        riskSolutionRepository.deleteByUserId(userId);
        // encouragementMessageRepository.deleteByUser(user); // 해당 레포지토리의 deleteBy... 메서드 필요
        // commentRepository.deleteByUser(user);
        // postLikeRepository.deleteByUser(user);

        userRepository.delete(user);
    }

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
    // 반환 타입을 List<RiskSolution>에서 List<RiskSolutionResponse>로 변경
    public List<RiskSolutionResponse> getRiskSolutions(Long userId, Integer riskLevel) {
        List<RiskSolution> solutions;
        if (riskLevel != null) {
            solutions = riskSolutionRepository.findByUserIdAndRiskLevel(userId, riskLevel);
        } else {
            solutions = riskSolutionRepository.findByUserId(userId);
        }

        // Entity를 DTO로 변환하여 반환
        return solutions.stream()
                .map(this::convertToRiskSolutionResponse)
                .collect(Collectors.toList());
    }

    // RiskSolution Entity를 RiskSolutionResponse DTO로 변환하는 private 메서드
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

        // 오늘 이미 작성했는지 확인
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

        // 밥 +1
        user.addRice(1);
        userRepository.save(user);
    }

    // 응원 문구 전체 가져오기
    @Transactional(readOnly = true)
    public List<EncouragementMessageResponse> getEncouragementMessages(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }

        // 엔티티를 조회하고, 스트림을 통해 DTO로 변환하여 반환
        return encouragementMessageRepository.findByUserIdOrderByDateDesc(userId).stream()
                .map(EncouragementMessageResponse::from)
                .collect(Collectors.toList());
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
        if (level <= 1) {
            return 10; // 레벨 1은 10개로 시작
        }

        if (level <= 5) {
            // 레벨 2~5
            return 20 + (level - 1) * 5;
        }

        if (level <= 10) {
            // 레벨 6~10
            return 40 + (level - 5) * 10;
        }

        // 최대 레벨(10)을 초과하는 경우
        return 100;
    }
}