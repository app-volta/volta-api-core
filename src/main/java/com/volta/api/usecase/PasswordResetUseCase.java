package com.volta.api.usecase;

import com.volta.api.dto.auth.ForgotPasswordRequestDTO;
import com.volta.api.dto.auth.MessageResponseDTO;
import com.volta.api.dto.auth.ResetPasswordRequestDTO;
import com.volta.api.dto.auth.VerifyCodeRequestDTO;

public interface PasswordResetUseCase {
    MessageResponseDTO requestCode(ForgotPasswordRequestDTO dto);

    void verifyCode(VerifyCodeRequestDTO dto);

    void resetPassword(ResetPasswordRequestDTO dto);
}
