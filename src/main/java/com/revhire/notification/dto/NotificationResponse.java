package com.revhire.notification.dto;

import com.revhire.common.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
