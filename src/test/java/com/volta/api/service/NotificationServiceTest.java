package com.volta.api.service;

import com.volta.api.database.entity.Notification;
import com.volta.api.database.repository.NotificationRepository;
import com.volta.api.dto.notification.NotificationResponseDTO;
import com.volta.api.exception.ResourceNotFoundException;
import com.volta.api.mapper.NotificationMapper;
import com.volta.api.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.volta.api.support.UserFixtures.authenticatedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService service;

    private final AuthenticatedUser author = authenticatedUser(UUID.randomUUID(), UUID.randomUUID(), "EMPLOYEE");

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, new NotificationMapper());
    }

    private Notification notification(boolean read) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .type("COLLECTION")
                .title("Coleta solicitada")
                .message("Mensagem")
                .read(read)
                .createdAt(LocalDateTime.of(2026, 11, 2, 9, 0))
                .build();
    }

    @Test
    @DisplayName("lists all notifications of the user")
    void listsAll() {
        Notification unread = notification(false);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(author.id())).thenReturn(List.of(unread));

        List<NotificationResponseDTO> result = service.getNotifications(false, author);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(unread.getId());
        assertThat(result.get(0).read()).isFalse();
    }

    @Test
    @DisplayName("lists only unread notifications when requested")
    void listsUnreadOnly() {
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(author.id()))
                .thenReturn(List.of(notification(false)));

        List<NotificationResponseDTO> result = service.getNotifications(true, author);

        assertThat(result).hasSize(1);
        verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(author.id());
    }

    @Test
    @DisplayName("marks an owned notification as read")
    void marksAsRead() {
        Notification unread = notification(false);
        when(notificationRepository.findByIdAndUserId(unread.getId(), author.id())).thenReturn(Optional.of(unread));

        service.markAsRead(unread.getId(), author);

        assertThat(unread.isRead()).isTrue();
        verify(notificationRepository).save(unread);
    }

    @Test
    @DisplayName("fails when the notification does not belong to the user")
    void failsWhenNotOwned() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(id, author.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(id, author)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("marks every notification of the user as read")
    void marksAllAsRead() {
        service.markAllAsRead(author);

        verify(notificationRepository).markAllAsRead(author.id());
    }
}
