package com.volta.api.service;

import com.volta.api.database.entity.Collection;
import com.volta.api.database.entity.Incident;
import com.volta.api.database.entity.Notification;
import com.volta.api.database.entity.Users;
import com.volta.api.database.repository.NotificationRepository;
import com.volta.api.database.repository.UserRepository;
import com.volta.api.enums.CollectionStatusType;
import com.volta.api.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final List<String> REVIEWER_ROLES = List.of("ADMIN", "MANAGER");

    private static final List<String> MANAGER_ROLES = List.of("MANAGER");

    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;

    public void incidentRegistered(Incident incident) {
        List<Users> recipients = userRepository.findByCompanyIdAndRoleTypeIn(incident.getCompany().getId(), REVIEWER_ROLES);

        save(
                recipients,
                NotificationType.INCIDENT,
                "Ocorrência registrada",
                "A ocorrência " + label(incident) + " foi registrada e aguarda análise."
        );
    }

    public void incidentReviewed(Incident incident, boolean approved) {
        String title = approved ? "Ocorrência aprovada" : "Ocorrência recusada";
        String message = "A ocorrência " + label(incident) + (approved ? " foi aprovada." : " foi recusada.");

        if (!approved && incident.getChangeReason() != null && !incident.getChangeReason().isBlank()) {
            message = message + " Motivo: " + incident.getChangeReason();
        }

        save(List.of(incident.getUser()), NotificationType.INCIDENT, title, message);
    }

    public void collectionChanged(Collection collection, CollectionStatusType status) {
        String title = switch (status) {
            case REQUESTED -> "Coleta solicitada";
            case SCHEDULED -> "Coleta agendada";
            case COMPLETED -> "Coleta concluída";
            case CANCELED -> "Coleta cancelada";
            default -> null;
        };

        if (title == null) {
            return;
        }

        Incident incident = collection.getIncident();
        List<Users> recipients = new ArrayList<>();
        recipients.add(incident.getUser());
        recipients.addAll(userRepository.findByCompanyIdAndRoleTypeIn(incident.getCompany().getId(), MANAGER_ROLES));

        save(
                recipients,
                NotificationType.COLLECTION,
                title,
                title + " para a ocorrência " + label(incident) + "."
        );
    }

    private void save(List<Users> recipients, NotificationType type, String title, String message) {
        Map<UUID, Users> distinct = new LinkedHashMap<>();

        for (Users recipient : recipients) {
            distinct.putIfAbsent(recipient.getId(), recipient);
        }

        List<Notification> notifications = new ArrayList<>();

        for (Users recipient : distinct.values()) {
            notifications.add(Notification.builder()
                    .user(recipient)
                    .type(type.name())
                    .title(title)
                    .message(message)
                    .build());
        }

        notificationRepository.saveAll(notifications);
    }

    private String label(Incident incident) {
        if (incident.getTitle() == null || incident.getTitle().isBlank()) {
            return "sem título";
        }

        return "\"" + incident.getTitle() + "\"";
    }
}
