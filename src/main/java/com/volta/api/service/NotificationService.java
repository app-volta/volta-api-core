package com.volta.api.service;

import com.volta.api.database.entity.Notification;
import com.volta.api.database.repository.NotificationRepository;
import com.volta.api.dto.notification.NotificationResponseDTO;
import com.volta.api.exception.ResourceNotFoundException;
import com.volta.api.mapper.NotificationMapper;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.usecase.NotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final NotificationRepository notificationRepository;

    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotifications(boolean unreadOnly, AuthenticatedUser author) {
        List<Notification> found;
        if (unreadOnly) {
            found = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(author.id());
        } else {
            found = notificationRepository.findByUserIdOrderByCreatedAtDesc(author.id());
        }

        List<NotificationResponseDTO> notifications = new ArrayList<>();

        for (Notification notification : found) {
            notifications.add(notificationMapper.toResponse(notification));
        }

        return notifications;
    }

    @Transactional
    public void markAsRead(UUID id, AuthenticatedUser author) {
        Notification notification = notificationRepository.findByIdAndUserId(id, author.id())
                .orElseThrow(() -> new ResourceNotFoundException("Notification"));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(AuthenticatedUser author) {
        notificationRepository.markAllAsRead(author.id());
    }
}
