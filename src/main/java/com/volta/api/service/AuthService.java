package com.volta.api.service;

import com.volta.api.database.entity.Users;
import com.volta.api.database.repository.UserRepository;
import com.volta.api.dto.auth.LoginRequestDTO;
import com.volta.api.dto.auth.TokenResponseDTO;
import com.volta.api.exception.ResourceNotFoundException;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.security.jwt.TokenProvider;
import com.volta.api.usecase.AuthUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

    public TokenResponseDTO login(LoginRequestDTO dto) {
        Authentication authentication = authenticationManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(
                                dto.email(),
                                dto.password()
                        )
                );
        String token = tokenProvider.generateToken(authentication);
        boolean firstLogin = isFirstLogin(authentication);
        return new TokenResponseDTO(token, tokenProvider.getExpirationTime(), firstLogin);
    }

    private boolean isFirstLogin(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        Users user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        return Boolean.TRUE.equals(user.getFirstLogin());
    }
}
