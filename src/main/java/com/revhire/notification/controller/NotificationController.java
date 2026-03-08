package com.revhire.notification.controller;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.NotificationType;
import com.revhire.notification.dto.NotificationResponse;
import com.revhire.notification.service.NotificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private static final Logger logger = LogManager.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Autowired
    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Resolve the authenticated user's ID from Spring Security.
     */
    private Long getAuthenticatedUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return user.getId();
    }

    /**
     * Show all notifications for the logged-in user, with optional type filter.
     */
    @GetMapping
    public String listNotifications(
            @RequestParam(required = false) String type,
            Model model,
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        logger.info("Listing notifications for user {}, filter type: {}", userId, type);

        List<NotificationResponse> notifications = notificationService.getAllNotifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);

        // Filter by type if specified
        if (type != null && !type.isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            try {
                NotificationType filterType = NotificationType.valueOf(type);
                notifications = notifications.stream()
                        .filter(n -> n.getType() == filterType)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid notification type filter: {}", type);
            }
        }

        // Count by type for tab badges
        List<NotificationResponse> all = notificationService.getAllNotifications(userId);
        long applicationUpdateCount = all.stream().filter(n -> n.getType() == NotificationType.APPLICATION_UPDATE).count();
        long applicationReceivedCount = all.stream().filter(n -> n.getType() == NotificationType.APPLICATION_RECEIVED).count();
        long applicationWithdrawnCount = all.stream().filter(n -> n.getType() == NotificationType.APPLICATION_WITHDRAWN).count();
        long systemCount = all.stream().filter(n -> n.getType() == NotificationType.SYSTEM || n.getType() == NotificationType.JOB_RECOMMENDATION).count();

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("totalCount", all.size());
        model.addAttribute("applicationUpdateCount", applicationUpdateCount);
        model.addAttribute("applicationReceivedCount", applicationReceivedCount);
        model.addAttribute("applicationWithdrawnCount", applicationWithdrawnCount);
        model.addAttribute("systemCount", systemCount);
        model.addAttribute("activeFilter", type != null ? type : "ALL");

        return "notification/notifications";
    }

    /**
     * Get unread notification count (for AJAX calls from navbar).
     */
    @GetMapping("/unread-count")
    @ResponseBody
    public long getUnreadCount(Authentication authentication) {
        if (authentication == null) return 0;
        Long userId = getAuthenticatedUserId(authentication);
        return notificationService.getUnreadCount(userId);
    }

    /**
     * Get recent notifications as JSON (for navbar dropdown).
     */
    @GetMapping("/recent")
    @ResponseBody
    public Map<String, Object> getRecentNotifications(Authentication authentication) {
        if (authentication == null) {
            return Map.of("notifications", List.of(), "unreadCount", 0L);
        }
        Long userId = getAuthenticatedUserId(authentication);
        List<NotificationResponse> recent = notificationService.getUnreadNotifications(userId);
        // Limit to top 5 for dropdown
        if (recent.size() > 5) {
            recent = recent.subList(0, 5);
        }
        long unreadCount = notificationService.getUnreadCount(userId);
        return Map.of("notifications", recent, "unreadCount", unreadCount);
    }

    /**
     * Mark a single notification as read (AJAX + form POST support).
     */
    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        logger.info("Marking notification {} as read", id);
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Mark all notifications as read (AJAX + form POST support).
     */
    @PostMapping("/read-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        logger.info("Marking all notifications as read for user {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Delete a notification (AJAX + form POST support).
     */
    @PostMapping("/{id}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id) {
        logger.info("Deleting notification {}", id);
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
