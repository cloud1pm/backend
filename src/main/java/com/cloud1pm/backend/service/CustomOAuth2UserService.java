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
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = (String) attributes.get(userNameAttributeName);

        // ProviderType 결정
        ProviderType providerType = ProviderType.valueOf(registrationId.toUpperCase());

        saveOrUpdate(email, name, registrationId, providerId, providerType);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName
        );
    }

    private User saveOrUpdate(String email, String name, String provider, String providerId, ProviderType providerType) {
        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    if (existingUser.getProviderType() == providerType) {
                        return existingUser.updateOAuthInfo(name, email);
                    } else {
                        // 이메일은 같지만 가입 경로가 다른 경우 (예: 일반 가입 후 구글 로그인 시도)
                        // 정책에 따라 병합하거나 에러를 낼 수 있습니다. 여기선 에러 처리.
                        throw new IllegalArgumentException("Email already signed up with " + existingUser.getProviderType());
                    }
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .username(name) // Username을 구글 이름으로 설정
                        .nickname(name)
                        .providerType(providerType)
                        .provider(provider)
                        .providerId(providerId)
                        .password(null)
                        .hasCompletedInitialSetup(false) // 신규 구글 회원은 초기 설정 필요
                        .build()));
    }
}