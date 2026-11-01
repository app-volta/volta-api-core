package com.volta.api.service;

import com.volta.api.database.entity.EsgMetric;
import com.volta.api.database.repository.EsgMetricRepository;
import com.volta.api.dto.goal.GoalRequestDTO;
import com.volta.api.dto.goal.GoalResponseDTO;
import com.volta.api.exception.BusinessRuleException;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.usecase.GoalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoalService implements GoalUseCase {

    private final EsgMetricRepository esgMetricRepository;

    @Transactional(readOnly = true)
    public GoalResponseDTO getGoals(AuthenticatedUser author) {
        Optional<EsgMetric> found = esgMetricRepository
                .findFirstByCompanyIdAndMonthlyTargetIsNotNullOrderByCalculatedAtDesc(author.companyId());

        BigDecimal monthlyKg = null;
        if (found.isPresent()) {
            monthlyKg = found.get().getMonthlyTarget();
        }

        return new GoalResponseDTO(monthlyKg);
    }

    @Transactional
    public GoalResponseDTO updateGoals(GoalRequestDTO dto, AuthenticatedUser author) {
        Optional<EsgMetric> found = esgMetricRepository.findFirstByCompanyIdOrderByCalculatedAtDesc(author.companyId());
        if (found.isEmpty()) {
            throw new BusinessRuleException("Register an ESG metric before setting a goal");
        }

        EsgMetric esgMetric = found.get();
        esgMetric.setMonthlyTarget(dto.monthlyKg());
        esgMetricRepository.save(esgMetric);

        return new GoalResponseDTO(dto.monthlyKg());
    }
}
