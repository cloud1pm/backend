package com.cloud1pm.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class AlertSettingRequest {
    private LocalTime alertTime;
    private Boolean isEnabled;
}