package com.revhire.notification.service;

import com.revhire.auth.entity.User;
import com.revhire.common.enums.NotificationType;
import com.revhire.common.enums.Role;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.notification.dto.NotificationResponse;
import com.revhire.notification.entity.Notification;
import com.revhire.notification.repository.NotificationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Notification testNotification;

    @Before
    public void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .role(Role.SEEKER)
                .build();

        testNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .message("Your application has been shortlisted")
                .type(NotificationType.APPLICATION_UPDATE)
                .isRead(false)
                .link("/applications/1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void testCreateNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.createNotification(testUser, "Test message", NotificationType.APPLICATION_UPDATE, "/test");

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void testGetAllNotifications_Success() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(notifications);

        List<NotificationResponse> result = notificationService.getAllNotifications(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Your application has been shortlisted", result.get(0).getMessage());
    }

    @Test
    public void testGetUnreadCount_Success() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    public void testMarkAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.markAsRead(1L);

        assertTrue(testNotification.getIsRead());
        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testMarkAsRead_NotFound_ThrowsException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        notificationService.markAsRead(99L);
    }

    @Test
    public void testMarkAllAsRead_Success() {
        Notification notif1 = Notification.builder().id(1L).user(testUser).isRead(false).build();
        Notification notif2 = Notification.builder().id(2L).user(testUser).isRead(false).build();
        List<Notification> unread = Arrays.asList(notif1, notif2);

        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L)).thenReturn(unread);

        notificationService.markAllAsRead(1L);

        assertTrue(notif1.getIsRead());
        assertTrue(notif2.getIsRead());
        verify(notificationRepository, times(1)).saveAll(unread);
    }

    @Test
    public void testDeleteNotification_Success() {
        when(notificationRepository.existsById(1L)).thenReturn(true);

        notificationService.deleteNotification(1L);

        verify(notificationRepository, times(1)).deleteById(1L);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeleteNotification_NotFound_ThrowsException() {
        when(notificationRepository.existsById(99L)).thenReturn(false);

        notificationService.deleteNotification(99L);
    }
}
