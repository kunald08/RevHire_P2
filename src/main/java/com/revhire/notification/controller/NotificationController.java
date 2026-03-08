package com.revhire.notification.controller;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.notification.dto.NotificationResponse;
import com.revhire.notification.service.NotificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

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
     * Show all notifications for the logged-in user.
     */
    @GetMapping
    public String listNotifications(Model model) {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            logger.warn("Unauthenticated user tried to access notifications");
            return "redirect:/auth/login";
        }
        logger.info("Listing notifications for user {}", userId);

        List<NotificationResponse> notifications = notificationService.getAllNotifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        return "notification/notifications";
    }

    /**
     * Get unread notification count (for AJAX calls from navbar).
     */
    @GetMapping("/unread-count")
    @ResponseBody
    public long getUnreadCount() {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            return 0;
        }
        return notificationService.getUnreadCount(userId);
    }

    /**
     * Mark a single notification as read.
     */
    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id) {
        logger.info("Marking notification {} as read", id);
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }

    /**
     * Mark all notifications as read.
     */
    @PostMapping("/read-all")
    public String markAllAsRead() {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            return "redirect:/auth/login";
        }
        logger.info("Marking all notifications as read for user {}", userId);
        notificationService.markAllAsRead(userId);
        return "redirect:/notifications";
    }

    /**
     * Delete a notification.
     */
    @PostMapping("/{id}/delete")
    public String deleteNotification(@PathVariable Long id) {
        logger.info("Deleting notification {}", id);
        notificationService.deleteNotification(id);
        return "redirect:/notifications";
    }

    /**
     * Delete all notifications for the logged-in user.
     */
    @PostMapping("/delete-all")
    public String deleteAllNotifications() {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            return "redirect:/auth/login";
        }
        logger.info("Deleting all notifications for user {}", userId);
        notificationService.deleteAllNotifications(userId);
        return "redirect:/notifications";
    }

    /**
     * Get unread notifications as JSON for dropdown preview.
     */
    @GetMapping("/recent")
    @ResponseBody
    public List<NotificationResponse> getRecentNotifications() {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        return notificationService.getUnreadNotifications(userId);
    }

    /**
     * Helper: Get current authenticated user's ID from SecurityContext.
     */
    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            try {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception e) {
                logger.error("Error finding user by email: {}", e.getMessage());
            }
        }
        return null;
    }
}
