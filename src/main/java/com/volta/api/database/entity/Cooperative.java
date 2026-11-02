package com.volta.api.database.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Cooperative {
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
    @Column(unique = true, nullable = false, length = 150)
    private String name;

    @NotNull
    @Column(length = 18, unique = true, nullable = false)
    private String cnpj;

    @NotNull
    @Column(columnDefinition = "DECIMAL(9,6)", nullable = false)
    private BigDecimal latitude;

    @NotNull
    @Column(columnDefinition = "DECIMAL(9,6)", nullable = false)
    private BigDecimal longitude;

    @Builder.Default
    @Column(name = "average_rating", columnDefinition = "NUMERIC(3,2)")
    private BigDecimal averageRating = BigDecimal.valueOf(0);

    @NotNull
    @Column(length = 150, nullable = false)
    private String email;

    @Column(length = 500)
    private String specialties;

    @Builder.Default
    @OneToMany(mappedBy = "cooperative", fetch = FetchType.LAZY)
    private Set<Collection> collections = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "cooperative", fetch = FetchType.LAZY)
    private Set<Review> reviews = new HashSet<>();
}
