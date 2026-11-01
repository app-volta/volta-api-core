package com.volta.api.controller;

import com.volta.api.controller.docs.AuthControllerDocs;
import com.volta.api.dto.auth.ForgotPasswordRequestDTO;
import com.volta.api.dto.auth.LoginRequestDTO;
import com.volta.api.dto.auth.MessageResponseDTO;
import com.volta.api.dto.auth.ResetPasswordRequestDTO;
import com.volta.api.dto.auth.TokenResponseDTO;
import com.volta.api.dto.auth.VerifyCodeRequestDTO;
import com.volta.api.usecase.AuthUseCase;
import com.volta.api.usecase.PasswordResetUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController implements AuthControllerDocs {

    private final AuthUseCase authUseCase;

    private final PasswordResetUseCase passwordResetUseCase;

    @PostMapping("/login")
    public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO loginRequestDto) {
        return authUseCase.login(loginRequestDto);
    }

    @PostMapping("/forgot-password")
    public MessageResponseDTO forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        MessageResponseDTO response = passwordResetUseCase.requestCode(dto);
        return response;
    }

    @PostMapping("/verify-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyCode(@Valid @RequestBody VerifyCodeRequestDTO dto) {
        passwordResetUseCase.verifyCode(dto);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        passwordResetUseCase.resetPassword(dto);
    }
}
