package com.volta.api.service;

import com.volta.api.database.entity.EsgMetric;
import com.volta.api.database.repository.EsgMetricRepository;
import com.volta.api.dto.goal.GoalRequestDTO;
import com.volta.api.dto.goal.GoalResponseDTO;
import com.volta.api.exception.BusinessRuleException;
import com.volta.api.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.volta.api.support.UserFixtures.authenticatedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalService")
class GoalServiceTest {

    @Mock
    private EsgMetricRepository esgMetricRepository;

    @InjectMocks
    private GoalService service;

    private final UUID companyId = UUID.randomUUID();

    private final AuthenticatedUser author = authenticatedUser(UUID.randomUUID(), companyId, "MANAGER");

    @Test
    @DisplayName("getGoals devolve a meta mensal da métrica mais recente com meta")
    void getGoals_metricWithTarget_returnsMonthlyKg() {
        EsgMetric esgMetric = new EsgMetric();
        esgMetric.setMonthlyTarget(new BigDecimal("1200.00"));
        when(esgMetricRepository.findFirstByCompanyIdAndMonthlyTargetIsNotNullOrderByCalculatedAtDesc(companyId))
                .thenReturn(Optional.of(esgMetric));

        GoalResponseDTO response = service.getGoals(author);

        assertThat(response.monthlyKg()).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("getGoals sem meta definida devolve nulo")
    void getGoals_noTarget_returnsNull() {
        when(esgMetricRepository.findFirstByCompanyIdAndMonthlyTargetIsNotNullOrderByCalculatedAtDesc(companyId))
                .thenReturn(Optional.empty());

        GoalResponseDTO response = service.getGoals(author);

        assertThat(response.monthlyKg()).isNull();
    }

    @Test
    @DisplayName("updateGoals grava a meta na métrica mais recente")
    void updateGoals_existingMetric_savesMonthlyTarget() {
        EsgMetric esgMetric = new EsgMetric();
        when(esgMetricRepository.findFirstByCompanyIdOrderByCalculatedAtDesc(companyId))
                .thenReturn(Optional.of(esgMetric));

        GoalResponseDTO response = service.updateGoals(new GoalRequestDTO(new BigDecimal("1500.00")), author);

        assertThat(response.monthlyKg()).isEqualByComparingTo("1500.00");
        assertThat(esgMetric.getMonthlyTarget()).isEqualByComparingTo("1500.00");
        verify(esgMetricRepository).save(esgMetric);
    }

    @Test
    @DisplayName("updateGoals sem métrica registrada lança regra de negócio e não salva")
    void updateGoals_noMetric_throwsBusinessRuleAndDoesNotSave() {
        when(esgMetricRepository.findFirstByCompanyIdOrderByCalculatedAtDesc(companyId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateGoals(new GoalRequestDTO(new BigDecimal("1500.00")), author))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Register an ESG metric before setting a goal");

        verify(esgMetricRepository, never()).save(any());
    }
}
