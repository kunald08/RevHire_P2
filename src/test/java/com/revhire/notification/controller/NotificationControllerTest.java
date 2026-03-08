package com.revhire.notification.controller;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.NotificationType;
import com.revhire.common.enums.Role;
import com.revhire.notification.dto.NotificationResponse;
import com.revhire.notification.service.NotificationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Model model;

    @InjectMocks
    private NotificationController notificationController;

    private User testUser;

    @Before
    public void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .role(Role.SEEKER)
                .build();

        // Set up SecurityContext with authenticated user
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "test@example.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("SEEKER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    public void testListNotifications_AuthenticatedUser() {
        List<NotificationResponse> notifications = Arrays.asList(
                NotificationResponse.builder()
                        .id(1L)
                        .message("Test notification")
                        .type(NotificationType.APPLICATION_UPDATE)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        when(notificationService.getAllNotifications(1L)).thenReturn(notifications);
        when(notificationService.getUnreadCount(1L)).thenReturn(1L);

        String viewName = notificationController.listNotifications(model);

        assertEquals("notification/notifications", viewName);
        verify(model).addAttribute("notifications", notifications);
        verify(model).addAttribute("unreadCount", 1L);
    }

    @Test
    public void testListNotifications_UnauthenticatedUser() {
        // Clear security context
        SecurityContextHolder.clearContext();

        String viewName = notificationController.listNotifications(model);

        assertEquals("redirect:/auth/login", viewName);
    }

    @Test
    public void testGetUnreadCount_AuthenticatedUser() {
        when(notificationService.getUnreadCount(1L)).thenReturn(3L);

        long count = notificationController.getUnreadCount();

        assertEquals(3L, count);
    }

    @Test
    public void testGetUnreadCount_UnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        long count = notificationController.getUnreadCount();

        assertEquals(0L, count);
    }

    @Test
    public void testMarkAsRead_Success() {
        String result = notificationController.markAsRead(1L);

        assertEquals("redirect:/notifications", result);
        verify(notificationService, times(1)).markAsRead(1L);
    }

    @Test
    public void testMarkAllAsRead_AuthenticatedUser() {
        String result = notificationController.markAllAsRead();

        assertEquals("redirect:/notifications", result);
        verify(notificationService, times(1)).markAllAsRead(1L);
    }

    @Test
    public void testMarkAllAsRead_UnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        String result = notificationController.markAllAsRead();

        assertEquals("redirect:/auth/login", result);
        verify(notificationService, never()).markAllAsRead(anyLong());
    }

    @Test
    public void testDeleteNotification_Success() {
        String result = notificationController.deleteNotification(1L);

        assertEquals("redirect:/notifications", result);
        verify(notificationService, times(1)).deleteNotification(1L);
    }

    @Test
    public void testDeleteAllNotifications_AuthenticatedUser() {
        String result = notificationController.deleteAllNotifications();

        assertEquals("redirect:/notifications", result);
        verify(notificationService, times(1)).deleteAllNotifications(1L);
    }

    @Test
    public void testGetRecentNotifications_AuthenticatedUser() {
        List<NotificationResponse> unread = Arrays.asList(
                NotificationResponse.builder()
                        .id(1L)
                        .message("New notification")
                        .type(NotificationType.JOB_RECOMMENDATION)
                        .isRead(false)
                        .build()
        );
        when(notificationService.getUnreadNotifications(1L)).thenReturn(unread);

        List<NotificationResponse> result = notificationController.getRecentNotifications();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetRecentNotifications_UnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        List<NotificationResponse> result = notificationController.getRecentNotifications();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
