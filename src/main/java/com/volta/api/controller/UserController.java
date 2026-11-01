package com.volta.api.controller;

import com.volta.api.controller.docs.UserControllerDocs;
import com.volta.api.dto.user.PasswordChangeRequestDTO;
import com.volta.api.dto.user.UserRequestDTO;
import com.volta.api.dto.user.UserRoleUpdateRequestDTO;
import com.volta.api.dto.user.UserUpdateRequestDTO;
import com.volta.api.dto.user.UserResponseDTO;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.usecase.UserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserControllerDocs {

    private final UserUseCase userUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO create(@Valid @RequestBody UserRequestDTO dto) {
        return userUseCase.register(dto);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UserResponseDTO> show() {
        List<UserResponseDTO> users = userUseCase.getUsers();
        return users;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UserResponseDTO showById(@PathVariable UUID id) {
        UserResponseDTO user = userUseCase.getUserById(id);
        return user;
    }

    @GetMapping("/me")
    public UserResponseDTO showMe(@AuthenticationPrincipal AuthenticatedUser author) {
        UserResponseDTO user = userUseCase.getUserById(author.id());
        return user;
    }

    @PutMapping("/me")
    public UserResponseDTO update(
            @Valid @RequestBody UserUpdateRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        UserResponseDTO response = userUseCase.update(author.id(), dto);
        return response;
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody PasswordChangeRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        userUseCase.changePassword(author.id(), dto);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UserResponseDTO updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UserRoleUpdateRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        UserResponseDTO response = userUseCase.updateRole(id, dto, author);
        return response;
    }
}
