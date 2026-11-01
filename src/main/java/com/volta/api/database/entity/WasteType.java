package com.volta.api.database.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "waste_type")
public class WasteType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            columnDefinition = "UUID DEFAULT gen_random_uuid()",
            updatable = false,
            nullable = false,
            unique = true
    )
    private UUID id;

    @NotNull
    @Column(length = 100, nullable = false)
    private String category;

    private String description;

    @NotNull
    @Column(name = "default_risk_level", length = 50, nullable = false)
    private String defaultRiskLevel;

    @Builder.Default
    @ManyToMany(mappedBy = "wasteTypes", fetch = FetchType.LAZY)
    private Set<Incident> incidents = new HashSet<>();
}
