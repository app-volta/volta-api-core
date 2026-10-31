package com.volta.api.database.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Incident {
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "incident_waste_type",
            joinColumns = @JoinColumn(name = "incident_id"),
            inverseJoinColumns = @JoinColumn(name = "waste_type_id")
    )
    private Set<WasteType> wasteTypes = new HashSet<>();

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @NotNull
    @Column(name = "employee_description", nullable = false)
    private String employeeDescription;

    @Column(name = "ai_description", columnDefinition = "TEXT")
    private String aiDescription;

    @Column(length = 100)
    private String title;

    @Column(name = "change_reason", length = 200)
    private String changeReason;

    @Column(name = "contamination_level", length = 50)
    private String contaminationLevel;

    @Column(name = "estimated_quantity", columnDefinition = "DECIMAL(12,2)")
    private BigDecimal estimatedQuantity;

    @NotNull
    @Column(length = 30, nullable = false)
    private String priority;

    @NotNull
    @Column(length = 50, nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Builder.Default
    @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY)
    private Set<Collection> collections = new HashSet<>();
}
