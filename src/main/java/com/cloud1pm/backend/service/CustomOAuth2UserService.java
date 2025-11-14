// src/main/java/com/cloud1pm/backend/service/CustomOAuth2UserService.java
package com.cloud1pm.backend.service;

import com.cloud1pm.backend.entity.User;
import com.cloud1pm.backend.entity.ProviderType;
import com.cloud1pm.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 기본 DefaultOAuth2UserService를 사용하여 사용자 정보를 가져옵니다.
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName(); // "sub" (Google의 고유 ID)

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        User user = saveOrUpdate(email, name, ProviderType.GOOGLE);

        // Spring Security가 인증 정보를 저장할 수 있도록 DefaultOAuth2User 객체를 반환합니다.
        // authorities는 User 엔티티의 Role을 사용해야 하지만, 현재 단계에서는 "ROLE_USER"를 기본으로 설정합니다.
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName
        );
    }

    /**
     * 사용자의 이메일과 ProviderType을 기반으로 DB에 저장하거나 업데이트합니다.
     */
    private User saveOrUpdate(String email, String name, ProviderType providerType) {
        // 1. 기존 사용자를 이메일로 찾습니다.
        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    // 2. 이미 존재하는 경우: OAuth 정보로 업데이트 (이름 등)
                    if (existingUser.getProviderType() == providerType) {
                        return existingUser.updateOAuthInfo(name, email);
                    } else {
                        // 이메일은 같지만 ProviderType이 다르면 (예: LOCAL과 GOOGLE),
                        // 이는 보안상 문제될 수 있으므로 예외를 발생시키거나 로그인이 불가능하게 처리해야 합니다.
                        throw new IllegalArgumentException("이미 다른 방식으로 가입된 이메일입니다: " + providerType);
                    }
                })
                .orElseGet(() -> {
                    // 3. 존재하지 않는 경우: 새로운 User 엔티티를 생성하고 저장
                    return userRepository.save(User.builder()
                            .email(email)
                            .username(name)
                            .nickname(name) // 닉네임 초기값은 이름으로 설정
                            .providerType(providerType)
                            .password(null) // 소셜 로그인이므로 비밀번호는 null
                            .build());
                });

        return user;
    }
}