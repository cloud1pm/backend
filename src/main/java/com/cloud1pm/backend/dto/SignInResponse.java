package com.cloud1pm.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignInResponse {
    private String token;
}