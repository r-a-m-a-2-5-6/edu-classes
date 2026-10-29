package com.inspire.educlasses.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class HealthResponse {

    private String serverStatus;
    private String dbStatus;
    private LocalDateTime timeStamp;
}
