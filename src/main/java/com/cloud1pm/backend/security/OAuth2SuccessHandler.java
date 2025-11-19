package com.cloud1pm.backend.security;

import com.cloud1pm.backend.entity.User;
import com.cloud1pm.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // CustomOAuth2UserService에서 이미 저장했으므로 이메일로 조회
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

        // 연속 출석 체크
        user.checkConsecutiveLogin();
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // 프론트엔드로 리다이렉트 (토큰 전달)
        // 프론트엔드는 이 토큰으로 /api/user/status를 호출하여 hasCompletedInitialSetup을 확인해야 함
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}