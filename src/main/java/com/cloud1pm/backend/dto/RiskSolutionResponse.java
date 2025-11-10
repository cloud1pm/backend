package com.cloud1pm.backend.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskSolutionResponse {
    private Long id;
    private Integer riskLevel;
    private String solution;
}