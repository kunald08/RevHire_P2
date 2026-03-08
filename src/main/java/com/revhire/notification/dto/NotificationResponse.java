package com.revhire.notification.dto;

import com.revhire.common.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private String link;
    private LocalDateTime createdAt;

    /**
     * Returns a human-readable relative time string.
     */
    public String getTimeAgo() {
        if (createdAt == null) return "";
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(createdAt, now);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = ChronoUnit.HOURS.between(createdAt, now);
        if (hours < 24) return hours + "h ago";
        long days = ChronoUnit.DAYS.between(createdAt, now);
        if (days < 7) return days + "d ago";
        if (days < 30) return (days / 7) + "w ago";
        return createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    /**
     * Returns a formatted date string.
     */
    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"));
    }

    /**
     * Returns a Bootstrap icon class based on notification type.
     */
    public String getTypeIcon() {
        if (type == null) return "bi-bell";
        return switch (type) {
            case APPLICATION_UPDATE -> "bi-file-earmark-check";
            case APPLICATION_RECEIVED -> "bi-inbox-fill";
            case APPLICATION_WITHDRAWN -> "bi-arrow-return-left";
            case JOB_RECOMMENDATION -> "bi-briefcase-fill";
            case SYSTEM -> "bi-gear-fill";
        };
    }

    /**
     * Returns a CSS color class based on notification type.
     */
    public String getTypeColor() {
        if (type == null) return "text-secondary";
        return switch (type) {
            case APPLICATION_UPDATE -> "text-primary";
            case APPLICATION_RECEIVED -> "text-success";
            case APPLICATION_WITHDRAWN -> "text-warning";
            case JOB_RECOMMENDATION -> "text-info";
            case SYSTEM -> "text-secondary";
        };
    }

    /**
     * Returns a background color class for notification card.
     */
    public String getTypeBgColor() {
        if (type == null) return "#f3f4f6";
        return switch (type) {
            case APPLICATION_UPDATE -> "#dbeafe";
            case APPLICATION_RECEIVED -> "#dcfce7";
            case APPLICATION_WITHDRAWN -> "#fef3c7";
            case JOB_RECOMMENDATION -> "#e0f2fe";
            case SYSTEM -> "#f3f4f6";
        };
    }
}
