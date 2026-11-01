package com.volta.api.service;

import com.volta.api.config.PasswordResetProperties;
import com.volta.api.database.entity.Users;
import com.volta.api.database.repository.UserRepository;
import com.volta.api.dto.auth.ForgotPasswordRequestDTO;
import com.volta.api.dto.auth.MessageResponseDTO;
import com.volta.api.dto.auth.ResetPasswordRequestDTO;
import com.volta.api.dto.auth.VerifyCodeRequestDTO;
import com.volta.api.exception.InvalidRequestException;
import com.volta.api.security.PasswordResetCodeProvider;
import com.volta.api.usecase.PasswordResetUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService implements PasswordResetUseCase {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final PasswordResetCodeProvider codeProvider;

    private final PasswordResetProperties properties;

    public MessageResponseDTO requestCode(ForgotPasswordRequestDTO dto) {
        Optional<Users> found = userRepository.findByEmail(dto.email());
        if (found.isPresent()) {
            Users user = found.get();
            String code = codeProvider.generate(user);
            long validityMinutes = properties.codeValidity().toMinutes();
            emailService.sendPasswordResetCode(user.getEmail(), user.getName(), code, validityMinutes);
        }

        return new MessageResponseDTO("If the email exists, a code was sent");
    }

    public void verifyCode(VerifyCodeRequestDTO dto) {
        findUserWithValidCode(dto.email(), dto.code());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO dto) {
        Users user = findUserWithValidCode(dto.email(), dto.code());

        String newPasswordHash = passwordEncoder.encode(dto.newPassword());
        user.setPasswordHash(newPasswordHash);
        user.setFirstLogin(false);
        userRepository.save(user);
    }

    private Users findUserWithValidCode(String email, String code) {
        Optional<Users> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            throw new InvalidRequestException("Invalid or expired code");
        }

        Users user = found.get();
        boolean codeMatches = codeProvider.matches(user, code);
        if (!codeMatches) {
            throw new InvalidRequestException("Invalid or expired code");
        }

        return user;
    }
}
