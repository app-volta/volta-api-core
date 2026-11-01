package com.volta.api.database.repository;

import com.volta.api.database.entity.EsgMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EsgMetricRepository extends JpaRepository<EsgMetric, UUID> {

    List<EsgMetric> findByCompanyIdOrderByCalculatedAtDesc(UUID companyId);

    Optional<EsgMetric> findFirstByCompanyIdOrderByCalculatedAtDesc(UUID companyId);

    Optional<EsgMetric> findFirstByCompanyIdAndMonthlyTargetIsNotNullOrderByCalculatedAtDesc(UUID companyId);

    boolean existsByCompanyIdAndPeriod(UUID companyId, String period);
}
