package com.volta.api.controller;

import com.volta.api.controller.docs.GoalControllerDocs;
import com.volta.api.dto.goal.GoalRequestDTO;
import com.volta.api.dto.goal.GoalResponseDTO;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.usecase.GoalUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/goals")
public class GoalController implements GoalControllerDocs {

    private final GoalUseCase goalUseCase;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public GoalResponseDTO show(@AuthenticationPrincipal AuthenticatedUser author) {
        return goalUseCase.getGoals(author);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public GoalResponseDTO update(
            @Valid @RequestBody GoalRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        return goalUseCase.updateGoals(dto, author);
    }
}
