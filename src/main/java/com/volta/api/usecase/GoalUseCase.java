package com.volta.api.usecase;

import com.volta.api.dto.goal.GoalRequestDTO;
import com.volta.api.dto.goal.GoalResponseDTO;
import com.volta.api.security.AuthenticatedUser;

public interface GoalUseCase {
    GoalResponseDTO getGoals(AuthenticatedUser author);

    GoalResponseDTO updateGoals(GoalRequestDTO dto, AuthenticatedUser author);
}
