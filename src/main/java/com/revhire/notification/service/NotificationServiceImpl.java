package com.revhire.notification.service;

import com.revhire.auth.entity.User;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.common.enums.NotificationType;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.notification.dto.NotificationResponse;
import com.revhire.notification.entity.Notification;
import com.revhire.notification.repository.NotificationRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LogManager.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void createNotification(User user, String message, NotificationType type, String link) {
        logger.info("Creating notification for user {}: {}", user.getId(), message);
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .link(link)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
        logger.info("Notification created successfully with ID: {}", notification.getId());
    }

    @Override
    public List<NotificationResponse> getAllNotifications(Long userId) {
        logger.info("Fetching all notifications for user {}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        logger.info("Found {} notifications for user {}", notifications.size(), userId);
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        logger.info("Fetching unread notifications for user {}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long userId) {
        logger.debug("Getting unread count for user {}", userId);
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        logger.info("Marking notification {} as read", notificationId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        logger.info("Notification {} marked as read", notificationId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        logger.info("Marking all notifications as read for user {}", userId);
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        logger.info("Marked {} notifications as read for user {}", unread.size(), userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        logger.info("Deleting notification {}", notificationId);
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }
        notificationRepository.deleteById(notificationId);
        logger.info("Notification {} deleted successfully", notificationId);
    }

    @Override
    @Transactional
    public void deleteAllNotifications(Long userId) {
        logger.info("Deleting all notifications for user {}", userId);
        notificationRepository.deleteAllByUserId(userId);
        logger.info("All notifications deleted for user {}", userId);
    }

    // ==========================================
    // Business-specific notification methods
    // ==========================================

    @Override
    @Transactional
    public void notifyNewApplication(User employer, String seekerName, String jobTitle, Long jobId) {
        String message = String.format("%s applied for your job posting: %s", seekerName, jobTitle);
        String link = "/employer/jobs/" + jobId + "/applicants";
        createNotification(employer, message, NotificationType.APPLICATION_UPDATE, link);
        logger.info("Sent new application notification to employer {} for job {}", employer.getId(), jobTitle);
    }

    @Override
    @Transactional
    public void notifyApplicationStatusChange(User seeker, String jobTitle, ApplicationStatus newStatus, Long applicationId) {
        String statusText;
        switch (newStatus) {
            case SHORTLISTED:
                statusText = "Congratulations! Your application for \"" + jobTitle + "\" has been shortlisted!";
                break;
            case REJECTED:
                statusText = "Your application for \"" + jobTitle + "\" has been reviewed. Unfortunately, it was not selected at this time.";
                break;
            case UNDER_REVIEW:
                statusText = "Your application for \"" + jobTitle + "\" is now under review.";
                break;
            default:
                statusText = "Your application status for \"" + jobTitle + "\" has been updated to " + newStatus.name() + ".";
        }
        String link = "/applications/" + applicationId;
        createNotification(seeker, statusText, NotificationType.APPLICATION_UPDATE, link);
        logger.info("Sent status change notification to seeker {} - {} for job {}", seeker.getId(), newStatus, jobTitle);
    }

    @Override
    @Transactional
    public void notifyApplicationWithdrawn(User seeker, String jobTitle, Long applicationId) {
        String message = "You have successfully withdrawn your application for \"" + jobTitle + "\".";
        String link = "/applications";
        createNotification(seeker, message, NotificationType.APPLICATION_UPDATE, link);
        logger.info("Sent withdrawal confirmation notification to seeker {} for job {}", seeker.getId(), jobTitle);
    }

    @Override
    @Transactional
    public void notifyEmployerApplicationWithdrawn(User employer, String seekerName, String jobTitle, Long jobId) {
        String message = seekerName + " has withdrawn their application for \"" + jobTitle + "\".";
        String link = "/employer/jobs/" + jobId + "/applicants";
        createNotification(employer, message, NotificationType.APPLICATION_UPDATE, link);
        logger.info("Sent withdrawal notification to employer {} for job {}", employer.getId(), jobTitle);
    }

    @Override
    @Transactional
    public void notifyJobRecommendation(User seeker, String jobTitle, String companyName, Long jobId) {
        String message = "New job matching your profile: \"" + jobTitle + "\" at " + companyName + ". Check it out!";
        String link = "/jobs/" + jobId;
        createNotification(seeker, message, NotificationType.JOB_RECOMMENDATION, link);
        logger.info("Sent job recommendation notification to seeker {} for job {}", seeker.getId(), jobTitle);
    }

    @Override
    @Transactional
    public void notifyEmployerNoteAdded(User employer, String jobTitle, String seekerName, Long applicationId) {
        String message = "A note was added to " + seekerName + "'s application for \"" + jobTitle + "\".";
        String link = "/employer/applicants/" + applicationId;
        createNotification(employer, message, NotificationType.SYSTEM, link);
        logger.info("Sent note added notification to employer {} for application {}", employer.getId(), applicationId);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .link(notification.getLink())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
