package com.inspire.educlasses.controller;


import com.inspire.educlasses.dto.requestDto.LoginRequest;
import com.inspire.educlasses.dto.requestDto.RegisterRequest;
import com.inspire.educlasses.dto.responseDto.LoginResponse;
import com.inspire.educlasses.exception.DuplicateResourceException;
import com.inspire.educlasses.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Boot 3.4+. On older versions use @MockBean
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTests {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;


    private RegisterRequest buildRegisterRequest(String name, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginRequest buildLoginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginResponse buildLoginResponse() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 28, 20, 24, 59);
        return new LoginResponse("jwt-token", "siva@example.com", "siva", time, time);
    }

    //  register

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        static Stream<Arguments> invalidRegisterRequests() {
            return Stream.of(
                    Arguments.of("blank name", "", "siva@example.com", "password123"),
                    Arguments.of("missing name", null, "siva@example.com", "password123"),
                    Arguments.of("blank email", "siva", "", "password123"),
                    Arguments.of("invalid email format", "siva", "not-an-email", "password123"),
                    Arguments.of("blank password", "siva", "siva@example.com", ""),
                    Arguments.of("password shorter than 8", "siva", "siva@example.com", "short")
            );
        }

        @Test
        void register_whenRequestIsValid_returns201WithTokenResponse() throws Exception {
            // Arrange
            when(authService.register(any(RegisterRequest.class))).thenReturn(buildLoginResponse());
            RegisterRequest request = buildRegisterRequest("siva", "siva@example.com", "password123");

            // Act + Assert
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                    .andExpect(jsonPath("$.email").value("siva@example.com"))
                    .andExpect(jsonPath("$.name").value("siva"));

            verify(authService).register(any(RegisterRequest.class));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidRegisterRequests")
        void register_whenRequestIsInvalid_returns400AndDoesNotCallService(
                String caseName, String name, String email, String password) throws Exception {

            RegisterRequest request = buildRegisterRequest(name, email, password);

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        void register_whenBodyIsMalformedJson_returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"siva\", "))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        void register_whenBodyIsMissing_returns400() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void register_whenContentTypeIsNotJson_returns415() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("hello"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void register_whenEmailAlreadyExists_returns409() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new DuplicateResourceException("User already exists"));
            RegisterRequest request = buildRegisterRequest("siva", "siva@example.com", "password123");

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    //  login

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        static Stream<Arguments> invalidLoginRequests() {
            return Stream.of(
                    Arguments.of("blank email", "", "password123"),
                    Arguments.of("missing email", null, "password123"),
                    Arguments.of("invalid email format", "not-an-email", "password123"),
                    Arguments.of("blank password", "siva@example.com", ""),
                    Arguments.of("password shorter than 8", "siva@example.com", "short")
            );
        }

        @Test
        void login_whenCredentialsAreValid_returns200WithTokenResponse() throws Exception {
            // Arrange
            when(authService.login(any(LoginRequest.class))).thenReturn(buildLoginResponse());
            LoginRequest request = buildLoginRequest("siva@example.com", "password123");

            // Act + Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                    .andExpect(jsonPath("$.email").value("siva@example.com"))
                    .andExpect(jsonPath("$.name").value("siva"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidLoginRequests")
        void login_whenRequestIsInvalid_returns400AndDoesNotCallService(
                String caseName, String email, String password) throws Exception {

            LoginRequest request = buildLoginRequest(email, password);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        void login_whenCredentialsAreWrong_returns401() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));
            LoginRequest request = buildLoginRequest("siva@example.com", "wrong-password");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}