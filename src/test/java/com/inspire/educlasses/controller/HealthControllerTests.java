package com.inspire.educlasses.controller;

import com.inspire.educlasses.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class HealthControllerTests {

    private static final String HEALTH_URL = "/api/v1/health";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;


    @Test
    void getHealth_whenDBIsUp_returns200WithUpStatus() throws Exception {
        // Arrange
        when(healthService.getHealth()).thenReturn("UP");

        // Act + Assert
        mockMvc.perform(get(HEALTH_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.serverStatus").value("UP")) // Fixed key
                .andExpect(jsonPath("$.dbStatus").value("UP"))
                .andExpect(jsonPath("$.timeStamp").exists());

        verify(healthService).getHealth();
    }

    @Test
    void getHealth_whenDBIsDown_returns200WithDbStatusDown() throws Exception {
        // Arrange
        when(healthService.getHealth()).thenReturn("DOWN");

        // Act + Assert
        mockMvc.perform(get(HEALTH_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverStatus").value("UP"))
                .andExpect(jsonPath("$.dbStatus").value("DOWN"))
                .andExpect(jsonPath("$.timeStamp").exists());
    }

    @Test
    void getHealth_whenMethodIsNotGet_returns405() throws Exception {
        mockMvc.perform(post(HEALTH_URL))
                .andExpect(status().isMethodNotAllowed());
    }
}