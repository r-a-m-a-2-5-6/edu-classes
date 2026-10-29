package com.inspire.educlasses.service;

import com.inspire.educlasses.dto.requestDto.LoginRequest;
import com.inspire.educlasses.dto.requestDto.RegisterRequest;
import com.inspire.educlasses.dto.responseDto.LoginResponse;
import com.inspire.educlasses.entity.CustomUserDetails;
import com.inspire.educlasses.entity.User;
import com.inspire.educlasses.exception.DuplicateResourceException;
import com.inspire.educlasses.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService  {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    // Custom Register method for Authentication
    @Transactional
    public LoginResponse register(RegisterRequest request){

        //check the user exist already or not

        if(userRepository.existsByEmail(request.getEmail())){
            throw new DuplicateResourceException("User already exists");
        }

        //if user not exists create a new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null,userDetails.getAuthorities()
        );

        String token = jwtService.generateToken(authentication);

        return new LoginResponse(token,
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt());

    }

    // Login Method handles by Spring Security

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request){

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtService.generateToken(authentication);

        return new LoginResponse(token,
                user.getEmail(),
                user.getName(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

}
