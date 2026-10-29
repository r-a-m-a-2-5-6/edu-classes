package com.inspire.educlasses.service;

import com.inspire.educlasses.dto.requestDto.LoginRequest;
import com.inspire.educlasses.dto.requestDto.RegisterRequest;
import com.inspire.educlasses.dto.responseDto.LoginResponse;
import com.inspire.educlasses.entity.CustomUserDetails;
import com.inspire.educlasses.entity.User;
import com.inspire.educlasses.exception.DuplicateResourceException;
import com.inspire.educlasses.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 28, 20, 24, 59);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("siva");
        request.setEmail("siva@example.com");
        request.setPassword("plain-password");
        return request;
    }

    private LoginRequest buildLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("siva@example.com");
        request.setPassword("plain-password");
        return request;
    }

    private User buildUser() {
        User user = new User();
        user.setId(5L);
        user.setName("siva");
        user.setEmail("siva@example.com");
        user.setPasswordHash("encoded-password");
        user.setCreatedAt(CREATED_AT);
        user.setUpdatedAt(CREATED_AT);
        return user;
    }

    //  register

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void register_whenEmailIsNew_savesUserWithEncodedPasswordAndReturnsToken() {
            // Arrange
            RegisterRequest request = buildRegisterRequest();

            when(userRepository.existsByEmail("siva@example.com")).thenReturn(false);
            when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(5L);
                saved.setCreatedAt(CREATED_AT);
                saved.setUpdatedAt(CREATED_AT);
                return saved;
            });
            when(jwtService.generateToken(any(Authentication.class))).thenReturn("jwt-token");

            // Act
            LoginResponse result = authService.register(request);

            // Assert - what was saved
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertEquals("siva@example.com", savedUser.getEmail());
            assertEquals("siva", savedUser.getName());
            assertEquals("encoded-password", savedUser.getPasswordHash());
            assertNotEquals("plain-password", savedUser.getPasswordHash(), "Raw password must never be stored");

            // Assert - token generated for the saved user
            ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
            verify(jwtService).generateToken(authCaptor.capture());
            assertInstanceOf(CustomUserDetails.class, authCaptor.getValue().getPrincipal());

            // Assert - response
            assertEquals("jwt-token", result.getAccessToken());
            assertEquals("siva@example.com", result.getEmail());
            assertEquals("siva", result.getName());
            assertEquals(CREATED_AT, result.getCreatedAt());
            assertEquals(CREATED_AT, result.getUpdatedAt());
        }

        @Test
        void register_whenEmailAlreadyExists_throwsExceptionAndDoesNothingElse() {
            // Arrange
            when(userRepository.existsByEmail("siva@example.com")).thenReturn(true);

            // Act + Assert
            DuplicateResourceException exception = assertThrows(
                    DuplicateResourceException.class,
                    () -> authService.register(buildRegisterRequest())
            );
            assertEquals("User already exists", exception.getMessage());

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        void register_whenDBIsDown_throwsExceptionAndGeneratesNoToken() {
            // Arrange
            when(userRepository.existsByEmail("siva@example.com")).thenReturn(false);
            when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataAccessResourceFailureException("Connection refused"));

            // Act + Assert
            assertThrows(
                    DataAccessResourceFailureException.class,
                    () -> authService.register(buildRegisterRequest())
            );
            verify(jwtService, never()).generateToken(any());
        }
    }

    //  login

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        void login_whenCredentialsAreValid_returnsTokenAndUserDetails() {
            // Arrange
            CustomUserDetails userDetails = new CustomUserDetails(buildUser());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
            when(jwtService.generateToken(authentication)).thenReturn("jwt-token");

            // Act
            LoginResponse result = authService.login(buildLoginRequest());

            // Assert - credentials passed to Spring Security
            ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(authCaptor.capture());
            assertEquals("siva@example.com", authCaptor.getValue().getPrincipal());
            assertEquals("plain-password", authCaptor.getValue().getCredentials());

            // Assert - response
            assertEquals("jwt-token", result.getAccessToken());
            assertEquals("siva@example.com", result.getEmail());
            assertEquals("siva", result.getName());
            assertEquals(CREATED_AT, result.getCreatedAt());
            verify(jwtService).generateToken(authentication);
        }

        @Test
        void login_whenCredentialsAreInvalid_throwsExceptionAndGeneratesNoToken() {
            // Arrange
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act + Assert
            assertThrows(
                    BadCredentialsException.class,
                    () -> authService.login(buildLoginRequest())
            );
            verify(jwtService, never()).generateToken(any());
        }
    }
}