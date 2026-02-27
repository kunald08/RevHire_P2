package com.revhire.notification.service;

import com.revhire.auth.entity.User;
import com.revhire.common.enums.NotificationType;
import com.revhire.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    /**
     * Create and save a new notification for a user.
     */
    void createNotification(User user, String message, NotificationType type, String link);

    /**
     * Get all notifications for a user, ordered by most recent first.
     */
    List<NotificationResponse> getAllNotifications(Long userId);

    /**
     * Get only unread notifications for a user.
     */
    List<NotificationResponse> getUnreadNotifications(Long userId);

    /**
     * Get the count of unread notifications for a user.
     */
    long getUnreadCount(Long userId);

    /**
     * Mark a single notification as read.
     */
    void markAsRead(Long notificationId);

    /**
     * Mark all notifications as read for a user.
     */
    void markAllAsRead(Long userId);

    /**
     * Delete a notification by its ID.
     */
    void deleteNotification(Long notificationId);
}
