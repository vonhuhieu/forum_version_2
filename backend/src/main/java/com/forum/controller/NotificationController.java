package com.forum.controller;

import com.forum.dto.NotificationDTO;
import com.forum.dto.ResponseDTO;
import com.forum.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ResponseDTO<List<NotificationDTO>>> getNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ResponseDTO<Long>> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getMyUnreadCount());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ResponseDTO<Void>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ResponseDTO<Void>> markAllRead() {
        return ResponseEntity.ok(notificationService.markAllAsRead());
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<ResponseDTO<Void>> clearAll() {
        return ResponseEntity.ok(notificationService.clearAll());
    }
}
