package com.inspire.educlasses.controller;


import com.inspire.educlasses.dto.responseDto.HealthResponse;
import com.inspire.educlasses.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth(){
        String dbStatus = healthService.getHealth();
        return ResponseEntity.ok(new HealthResponse("UP",dbStatus, LocalDateTime.now()));
    }
}
