package com.revhire.notification.service;

import com.revhire.auth.entity.User;
import com.revhire.common.enums.ApplicationStatus;
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

    // ==========================================
    // Business-specific notification methods
    // ==========================================

    /**
     * Notify employer when a job seeker applies for their job.
     */
    void notifyNewApplication(User employer, String seekerName, String jobTitle, Long jobId);

    /**
     * Notify job seeker when their application status changes (shortlisted/rejected/under review).
     */
    void notifyApplicationStatusChange(User seeker, String jobTitle, ApplicationStatus newStatus, Long applicationId);

    /**
     * Notify job seeker when their application is withdrawn successfully.
     */
    void notifyApplicationWithdrawn(User seeker, String jobTitle, Long applicationId);

    /**
     * Notify employer when a seeker withdraws their application.
     */
    void notifyEmployerApplicationWithdrawn(User employer, String seekerName, String jobTitle, Long jobId);

    /**
     * Notify job seeker about a recommended job matching their profile.
     */
    void notifyJobRecommendation(User seeker, String jobTitle, String companyName, Long jobId);

    /**
     * Notify employer when a comment/note is added to an application.
     */
    void notifyEmployerNoteAdded(User employer, String jobTitle, String seekerName, Long applicationId);

    /**
     * Delete all notifications for a user.
     */
    void deleteAllNotifications(Long userId);
}
