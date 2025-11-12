// === InitialSetupRequest.java ===
package com.cloud1pm.backend.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InitialSetupRequest {
    private List<RiskSolutionDto> riskSolutions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskSolutionDto {
        private Integer riskLevel;
        private String solution;
    }
}