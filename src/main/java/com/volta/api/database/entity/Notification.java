package com.volta.api.database.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Notification {
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
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @NotNull
    @Column(length = 50, nullable = false)
    private String type;

    @NotNull
    @Column(length = 150, nullable = false)
    private String title;

    @NotNull
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Builder.Default
    @Column(nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
