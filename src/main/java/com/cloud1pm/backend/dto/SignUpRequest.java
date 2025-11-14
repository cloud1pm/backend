package com.cloud1pm.backend.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpRequest {
    private String email;
    private String username;
    private String nickname; // 닉네임 (선택 사항)
    private String password;
    private String confirmPassword;
    private String profileImageUrl; // 이미지 URL (선택 사항)
    private List<InitialSetupRequest.RiskSolutionDto> riskSolutions; // 단계별 해결 방법
}