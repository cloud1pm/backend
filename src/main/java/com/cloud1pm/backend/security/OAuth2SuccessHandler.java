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

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByProviderAndProviderId("google", providerId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .username(name)
                            .nickname(name)
                            .profileImageUrl(picture)
                            .provider("google")
                            .providerId(providerId)
                            .build();
                    return userRepository.save(newUser);
                });

        // 연속 출석 체크
        user.checkConsecutiveLogin();
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}