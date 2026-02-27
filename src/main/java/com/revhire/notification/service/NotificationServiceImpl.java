package com.revhire.notification.service;

import com.revhire.auth.entity.User;
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
