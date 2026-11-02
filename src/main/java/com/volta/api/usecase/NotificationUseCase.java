package com.volta.api.usecase;

import com.volta.api.dto.notification.NotificationResponseDTO;
import com.volta.api.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface NotificationUseCase {

    List<NotificationResponseDTO> getNotifications(boolean unreadOnly, AuthenticatedUser author);

    void markAsRead(UUID id, AuthenticatedUser author);

    void markAllAsRead(AuthenticatedUser author);
}
