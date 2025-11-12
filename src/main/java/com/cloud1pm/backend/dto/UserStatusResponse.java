// === UserStatusResponse.java ===
package com.cloud1pm.backend.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusResponse {
    private Integer rice;
    private Integer characterLevel;
    private Integer feedCount;
    private Integer consecutiveDays;
    private Boolean hasCompletedInitialSetup;
}