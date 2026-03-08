package com.revhire.notification.service;

import com.revhire.auth.entity.User;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.common.enums.NotificationType;
import com.revhire.common.enums.Role;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.notification.dto.NotificationResponse;
import com.revhire.notification.entity.Notification;
import com.revhire.notification.repository.NotificationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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
    private User testEmployer;
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

        testEmployer = User.builder()
                .id(2L)
                .name("Employer User")
                .email("employer@example.com")
                .password("password")
                .role(Role.EMPLOYER)
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

    // ==========================================
    // Core CRUD Tests
    // ==========================================

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
    public void testGetAllNotifications_EmptyList() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        List<NotificationResponse> result = notificationService.getAllNotifications(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetUnreadNotifications_Success() {
        List<Notification> unread = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L)).thenReturn(unread);

        List<NotificationResponse> result = notificationService.getUnreadNotifications(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsRead());
    }

    @Test
    public void testGetUnreadCount_Success() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    public void testGetUnreadCount_Zero() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(0L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(0L, count);
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
    public void testMarkAllAsRead_NoUnread() {
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        notificationService.markAllAsRead(1L);

        verify(notificationRepository, times(1)).saveAll(Collections.emptyList());
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

    @Test
    public void testDeleteAllNotifications_Success() {
        notificationService.deleteAllNotifications(1L);

        verify(notificationRepository, times(1)).deleteAllByUserId(1L);
    }

    // ==========================================
    // Business Notification Tests
    // ==========================================

    @Test
    public void testNotifyNewApplication_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyNewApplication(testEmployer, "John Doe", "Software Engineer", 10L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(testEmployer, saved.getUser());
        assertTrue(saved.getMessage().contains("John Doe"));
        assertTrue(saved.getMessage().contains("Software Engineer"));
        assertEquals(NotificationType.APPLICATION_UPDATE, saved.getType());
        assertTrue(saved.getLink().contains("/employer/jobs/10/applicants"));
    }

    @Test
    public void testNotifyApplicationStatusChange_Shortlisted() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyApplicationStatusChange(
                testUser, "Data Analyst", ApplicationStatus.SHORTLISTED, 5L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(testUser, saved.getUser());
        assertTrue(saved.getMessage().contains("shortlisted"));
        assertTrue(saved.getMessage().contains("Data Analyst"));
        assertEquals(NotificationType.APPLICATION_UPDATE, saved.getType());
    }

    @Test
    public void testNotifyApplicationStatusChange_Rejected() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyApplicationStatusChange(
                testUser, "Backend Dev", ApplicationStatus.REJECTED, 7L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertTrue(saved.getMessage().contains("not selected"));
        assertTrue(saved.getMessage().contains("Backend Dev"));
    }

    @Test
    public void testNotifyApplicationStatusChange_UnderReview() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyApplicationStatusChange(
                testUser, "Frontend Dev", ApplicationStatus.UNDER_REVIEW, 3L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertTrue(saved.getMessage().contains("under review"));
    }

    @Test
    public void testNotifyApplicationWithdrawn_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyApplicationWithdrawn(testUser, "DevOps Engineer", 4L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertTrue(saved.getMessage().contains("withdrawn"));
        assertTrue(saved.getMessage().contains("DevOps Engineer"));
        assertEquals("/applications", saved.getLink());
    }

    @Test
    public void testNotifyEmployerApplicationWithdrawn_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyEmployerApplicationWithdrawn(
                testEmployer, "Jane Seeker", "ML Engineer", 8L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertTrue(saved.getMessage().contains("Jane Seeker"));
        assertTrue(saved.getMessage().contains("withdrawn"));
        assertTrue(saved.getMessage().contains("ML Engineer"));
    }

    @Test
    public void testNotifyJobRecommendation_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyJobRecommendation(testUser, "Full Stack Dev", "TechCorp", 15L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertTrue(saved.getMessage().contains("Full Stack Dev"));
        assertTrue(saved.getMessage().contains("TechCorp"));
        assertEquals(NotificationType.JOB_RECOMMENDATION, saved.getType());
        assertEquals("/jobs/15", saved.getLink());
    }

    @Test
    public void testNotifyEmployerNoteAdded_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.notifyEmployerNoteAdded(testEmployer, "QA Engineer", "Bob Smith", 20L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertTrue(saved.getMessage().contains("note"));
        assertTrue(saved.getMessage().contains("Bob Smith"));
        assertEquals(NotificationType.SYSTEM, saved.getType());
    }

    // ==========================================
    // Response Mapping Tests
    // ==========================================

    @Test
    public void testToResponse_MapsAllFields() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(notifications);

        List<NotificationResponse> result = notificationService.getAllNotifications(1L);

        NotificationResponse response = result.get(0);
        assertEquals(Long.valueOf(1L), response.getId());
        assertEquals("Your application has been shortlisted", response.getMessage());
        assertEquals(NotificationType.APPLICATION_UPDATE, response.getType());
        assertFalse(response.getIsRead());
        assertEquals("/applications/1", response.getLink());
        assertNotNull(response.getCreatedAt());
    }
}
