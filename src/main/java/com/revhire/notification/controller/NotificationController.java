package com.revhire.notification.controller;

import com.revhire.notification.dto.NotificationResponse;
import com.revhire.notification.service.NotificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController
{

    private static final Logger logger = LogManager.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService)
    {
        this.notificationService = notificationService;
    }

    /**
     * Show all notifications for the logged-in user.
     * TODO: Replace hardcoded userId with authenticated user once Auth module is merged.
     */
    @GetMapping
    public String listNotifications(Model model)
    {
        // TODO: Get userId from SecurityContext after Ashwathy's auth module
        // Long userId = getAuthenticatedUserId();
        Long userId = 1L; // TEMPORARY placeholder
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
    public long getUnreadCount()
    {
        // TODO: Get userId from SecurityContext
        Long userId = 1L; // TEMPORARY placeholder
        return notificationService.getUnreadCount(userId);
    }

    /**
     * Mark a single notification as read.
     */
    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id)
    {
        logger.info("Marking notification {} as read", id);
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }

    /**
     * Mark all notifications as read.
     */
    @PostMapping("/read-all")
    public String markAllAsRead()
    {
        // TODO: Get userId from SecurityContext
        Long userId = 1L; // TEMPORARY placeholder
        logger.info("Marking all notifications as read for user {}", userId);
        notificationService.markAllAsRead(userId);
        return "redirect:/notifications";
    }

    /**
     * Delete a notification.
     */
    @PostMapping("/{id}/delete")
    public String deleteNotification(@PathVariable Long id)
    {
        logger.info("Deleting notification {}", id);
        notificationService.deleteNotification(id);
        return "redirect:/notifications";
    }
}
