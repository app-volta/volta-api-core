package com.volta.api.controller;

import com.volta.api.controller.docs.NotificationControllerDocs;
import com.volta.api.dto.notification.NotificationResponseDTO;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.usecase.NotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController implements NotificationControllerDocs {

    private final NotificationUseCase notificationUseCase;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public List<NotificationResponseDTO> show(
            @RequestParam(name = "unread", defaultValue = "false") boolean unread,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        return notificationUseCase.getNotifications(unread, author);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser author) {
        notificationUseCase.markAsRead(id, author);
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@AuthenticationPrincipal AuthenticatedUser author) {
        notificationUseCase.markAllAsRead(author);
    }
}
