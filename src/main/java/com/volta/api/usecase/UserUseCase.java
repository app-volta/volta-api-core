package com.volta.api.usecase;

import com.volta.api.dto.user.PasswordChangeRequestDTO;
import com.volta.api.dto.user.UserRequestDTO;
import com.volta.api.dto.user.UserRoleUpdateRequestDTO;
import com.volta.api.dto.user.UserUpdateRequestDTO;
import com.volta.api.dto.user.UserResponseDTO;
import com.volta.api.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface UserUseCase {
    UserResponseDTO register(UserRequestDTO dto);

    List<UserResponseDTO> getUsers();

    UserResponseDTO getUserById(UUID id);

    UserResponseDTO update(UUID id, UserUpdateRequestDTO dto);

    void changePassword(UUID id, PasswordChangeRequestDTO dto);

    UserResponseDTO updateRole(UUID id, UserRoleUpdateRequestDTO dto, AuthenticatedUser author);
}
