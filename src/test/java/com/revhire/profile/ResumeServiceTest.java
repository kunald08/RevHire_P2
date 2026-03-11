package com.revhire.profile;

import com.revhire.exception.BadRequestException;
import com.revhire.exception.FileStorageException;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.application.repository.ApplicationRepository;
import com.revhire.profile.entity.JobSeekerProfile;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.repository.ResumeRepository;
import com.revhire.profile.service.ProfileService;
import com.revhire.profile.service.ResumeServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResumeServiceImpl.
 * Uses JUnit 4 with Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private JobSeekerProfile testProfile;

    @Before
    public void setUp() {
        testProfile = JobSeekerProfile.builder()
                .id(1L)
                .build();
    }

    // ==================== Get Resumes ====================

    @Test
    public void testGetResumesByEmail_Success() {
        Resume resume1 = Resume.builder().id(1L).objective("My objective").build();
        Resume resume2 = Resume.builder().id(2L).fileName("resume.pdf").fileData(new byte[100]).build();

        when(profileService.getOrCreateProfile("kunal@test.com")).thenReturn(testProfile);
        when(resumeRepository.findByProfileId(1L)).thenReturn(Arrays.asList(resume1, resume2));

        List<Resume> resumes = resumeService.getResumesByEmail("kunal@test.com");

        assertNotNull(resumes);
        assertEquals(2, resumes.size());
    }

    @Test
    public void testGetResumeById_Success() {
        Resume resume = Resume.builder().id(1L).fileName("resume.pdf").build();
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        Resume result = resumeService.getResumeById(1L);

        assertNotNull(result);
        assertEquals("resume.pdf", result.getFileName());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetResumeById_NotFound() {
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

        resumeService.getResumeById(999L);
    }

    // ==================== Textual Resume ====================

    @Test
    public void testSaveTextualResume_Success() {
        when(profileService.getOrCreateProfile("kunal@test.com")).thenReturn(testProfile);
        when(resumeRepository.findTopByProfileIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(resumeRepository.save(any(Resume.class))).thenReturn(new Resume());

        resumeService.saveTextualResume("kunal@test.com", "My career objective", "Project 1, Project 2");

        verify(resumeRepository, times(1)).save(any(Resume.class));
    }

    @Test
    public void testSaveTextualResume_UpdateExisting() {
        Resume existingResume = Resume.builder().id(1L).profile(testProfile).build();

        when(profileService.getOrCreateProfile("kunal@test.com")).thenReturn(testProfile);
        when(resumeRepository.findTopByProfileIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(existingResume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(existingResume);

        resumeService.saveTextualResume("kunal@test.com", "Updated objective", "Updated projects");

        verify(resumeRepository, times(1)).save(any(Resume.class));
    }

    // ==================== File Upload ====================

    @Test
    public void testUploadResumeFile_PDF_Success() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(mockFile.getBytes()).thenReturn(new byte[1024]);

        when(profileService.getOrCreateProfile("kunal@test.com")).thenReturn(testProfile);

        resumeService.uploadResumeFile("kunal@test.com", mockFile);

        verify(resumeRepository, times(1)).save(any(Resume.class));
    }

    @Test
    public void testUploadResumeFile_DOCX_Success() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(2048L);
        when(mockFile.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(mockFile.getOriginalFilename()).thenReturn("resume.docx");
        when(mockFile.getBytes()).thenReturn(new byte[2048]);

        when(profileService.getOrCreateProfile("kunal@test.com")).thenReturn(testProfile);

        resumeService.uploadResumeFile("kunal@test.com", mockFile);

        verify(resumeRepository, times(1)).save(any(Resume.class));
    }

    @Test(expected = BadRequestException.class)
    public void testUploadResumeFile_EmptyFile() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);

        resumeService.uploadResumeFile("kunal@test.com", mockFile);
    }

    @Test(expected = BadRequestException.class)
    public void testUploadResumeFile_ExceedsMaxSize() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(3 * 1024 * 1024L); // 3MB

        resumeService.uploadResumeFile("kunal@test.com", mockFile);
    }

    @Test(expected = BadRequestException.class)
    public void testUploadResumeFile_InvalidFileType() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("image/png");

        resumeService.uploadResumeFile("kunal@test.com", mockFile);
    }

    @Test(expected = FileStorageException.class)
    public void testUploadResumeFile_IOError() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getBytes()).thenThrow(new IOException("Disk error"));

        when(profileService.getOrCreateProfile("kunal@test.com")).thenReturn(testProfile);

        resumeService.uploadResumeFile("kunal@test.com", mockFile);
    }

    // ==================== Download ====================

    @Test
    public void testDownloadResume_Success() {
        Resume resume = Resume.builder()
                .id(1L)
                .fileName("resume.pdf")
                .fileType("PDF")
                .fileData(new byte[1024])
                .fileSize(1024L)
                .build();

        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        Resume result = resumeService.downloadResume(1L);

        assertNotNull(result);
        assertNotNull(result.getFileData());
        assertEquals("resume.pdf", result.getFileName());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDownloadResume_NotFound() {
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

        resumeService.downloadResume(999L);
    }

    @Test(expected = BadRequestException.class)
    public void testDownloadResume_NoFileData() {
        Resume resume = Resume.builder()
                .id(1L)
                .objective("Text only resume")
                .fileData(null)
                .build();

        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        resumeService.downloadResume(1L);
    }

    // ==================== Delete ====================

    @Test
    public void testDeleteResume_Success() {
        Resume resume = Resume.builder()
                .id(1L)
                .profile(testProfile)
                .fileName("resume.pdf")
                .fileData(new byte[1024])
                .build();

        when(profileService.getOrCreateProfile("kunal@test.com")).thenReturn(testProfile);
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        resumeService.deleteResume("kunal@test.com", 1L);

        verify(resumeRepository, times(1)).delete(resume);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeleteResume_NotFound() {
        when(profileService.getOrCreateProfile("kunal@test.com")).thenReturn(testProfile);
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

        resumeService.deleteResume("kunal@test.com", 999L);
    }

    @Test(expected = BadRequestException.class)
    public void testDeleteResume_NotOwner() {
        JobSeekerProfile otherProfile = JobSeekerProfile.builder().id(2L).build();
        Resume resume = Resume.builder()
                .id(1L)
                .profile(otherProfile)
                .fileName("resume.pdf")
                .build();

        when(profileService.getOrCreateProfile("kunal@test.com")).thenReturn(testProfile);
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        resumeService.deleteResume("kunal@test.com", 1L);
    }
}
