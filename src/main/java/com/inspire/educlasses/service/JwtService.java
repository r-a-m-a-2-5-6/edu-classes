package com.inspire.educlasses.service;

import com.inspire.educlasses.entity.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.expiry}")
    private Long expiry;


    // Generate jwt Token

    public String generateToken(Authentication authentication){

        Instant now = Instant.now();

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String userId = String.valueOf(userDetails.getUserId());

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(role -> role != null && !role.startsWith("FACTOR_"))
                .toList();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .claim("roles",roles)
                .build();

        Jwt jwt = jwtEncoder.encode(
                JwtEncoderParameters.from(claimsSet)
        );

        return jwt.getTokenValue();

    }


}
