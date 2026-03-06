package com.revhire.profile;

import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import com.revhire.common.enums.Role;
import com.revhire.exception.ResourceNotFoundException;
import com.revhire.exception.UnauthorizedException;
import com.revhire.profile.dto.*;
import com.revhire.profile.entity.*;
import com.revhire.profile.repository.*;
import com.revhire.profile.service.ProfileService;
import com.revhire.profile.service.ProfileServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfileServiceImpl.
 * Uses JUnit 4 with Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private EducationRepository educationRepository;
    @Mock
    private ExperienceRepository experienceRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private CertificationRepository certificationRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User testUser;
    private JobSeekerProfile testProfile;

    @Before
    public void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Kunal")
                .email("kunal@test.com")
                .password("encoded_password")
                .phone("9876543210")
                .location("Chennai")
                .role(Role.SEEKER)
                .build();

        testProfile = JobSeekerProfile.builder()
                .id(1L)
                .user(testUser)
                .headline("Java Developer")
                .summary("Experienced Java developer")
                .currentEmploymentStatus("Actively Looking")
                .educations(new ArrayList<>())
                .experiences(new ArrayList<>())
                .skills(new ArrayList<>())
                .certifications(new ArrayList<>())
                .resumes(new ArrayList<>())
                .build();
    }

    // ==================== Profile Tests ====================

    @Test
    public void testGetOrCreateProfile_ExistingProfile() {
        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        JobSeekerProfile result = profileService.getOrCreateProfile("kunal@test.com");

        assertNotNull(result);
        assertEquals("Java Developer", result.getHeadline());
        verify(profileRepository, never()).save(any());
    }

    @Test
    public void testGetOrCreateProfile_NewProfile() {
        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(JobSeekerProfile.class))).thenReturn(testProfile);

        JobSeekerProfile result = profileService.getOrCreateProfile("kunal@test.com");

        assertNotNull(result);
        verify(profileRepository, times(1)).save(any(JobSeekerProfile.class));
    }

    @Test
    public void testGetProfileByEmail_Success() {
        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        ProfileResponse response = profileService.getProfileByEmail("kunal@test.com");

        assertNotNull(response);
        assertEquals("Kunal", response.getUserName());
        assertEquals("Java Developer", response.getHeadline());
        assertEquals("kunal@test.com", response.getUserEmail());
    }

    @Test
    public void testGetProfileByUserId_Success() {
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        ProfileResponse response = profileService.getProfileByUserId(1L);

        assertNotNull(response);
        assertEquals("Kunal", response.getUserName());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetProfileByUserId_NotFound() {
        when(profileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        profileService.getProfileByUserId(999L);
    }

    @Test
    public void testSaveProfile_Success() {
        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any(JobSeekerProfile.class))).thenReturn(testProfile);

        ProfileRequest request = ProfileRequest.builder()
                .headline("Senior Java Developer")
                .summary("Updated summary")
                .currentEmploymentStatus("Employed")
                .build();

        profileService.saveProfile("kunal@test.com", request);

        verify(profileRepository, times(1)).save(any(JobSeekerProfile.class));
    }

    // ==================== Education Tests ====================

    @Test
    public void testAddEducation_Success() {
        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        EducationDto dto = EducationDto.builder()
                .institution("IIT Chennai")
                .degree("B.Tech")
                .fieldOfStudy("Computer Science")
                .startDate(LocalDate.of(2018, 8, 1))
                .endDate(LocalDate.of(2022, 5, 1))
                .grade("8.5")
                .build();

        profileService.addEducation("kunal@test.com", dto);

        verify(educationRepository, times(1)).save(any(Education.class));
    }

    @Test
    public void testUpdateEducation_Success() {
        Education existingEdu = Education.builder()
                .id(1L)
                .profile(testProfile)
                .institution("Old University")
                .build();

        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(educationRepository.findById(1L)).thenReturn(Optional.of(existingEdu));

        EducationDto dto = EducationDto.builder()
                .institution("Updated University")
                .degree("M.Tech")
                .build();

        profileService.updateEducation("kunal@test.com", 1L, dto);

        verify(educationRepository, times(1)).save(any(Education.class));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeleteEducation_NotFound() {
        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(educationRepository.findById(999L)).thenReturn(Optional.empty());

        profileService.deleteEducation("kunal@test.com", 999L);
    }

    @Test
    public void testDeleteEducation_Success() {
        Education edu = Education.builder().id(1L).profile(testProfile).build();

        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(educationRepository.findById(1L)).thenReturn(Optional.of(edu));

        profileService.deleteEducation("kunal@test.com", 1L);

        verify(educationRepository, times(1)).delete(edu);
    }

    // ==================== Experience Tests ====================

    @Test
    public void testAddExperience_Success() {
        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        ExperienceDto dto = ExperienceDto.builder()
                .company("Revature")
                .title("Software Engineer")
                .location("Hyderabad")
                .startDate(LocalDate.of(2022, 6, 1))
                .isCurrent(true)
                .description("Full stack development")
                .build();

        profileService.addExperience("kunal@test.com", dto);

        verify(experienceRepository, times(1)).save(any(Experience.class));
    }

    @Test(expected = UnauthorizedException.class)
    public void testDeleteExperience_Unauthorized() {
        JobSeekerProfile otherProfile = JobSeekerProfile.builder().id(99L).build();
        Experience exp = Experience.builder().id(1L).profile(otherProfile).build();

        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(exp));

        profileService.deleteExperience("kunal@test.com", 1L);
    }

    // ==================== Skills Tests ====================

    @Test
    public void testAddSkill_Success() {
        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        SkillDto dto = SkillDto.builder()
                .name("Spring Boot")
                .proficiency("Advanced")
                .build();

        profileService.addSkill("kunal@test.com", dto);

        verify(skillRepository, times(1)).save(any(Skill.class));
    }

    @Test
    public void testDeleteSkill_Success() {
        Skill skill = Skill.builder().id(1L).profile(testProfile).build();

        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));

        profileService.deleteSkill("kunal@test.com", 1L);

        verify(skillRepository, times(1)).delete(skill);
    }

    // ==================== Certification Tests ====================

    @Test
    public void testAddCertification_Success() {
        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        CertificationDto dto = CertificationDto.builder()
                .name("AWS Certified Developer")
                .issuingOrg("Amazon Web Services")
                .issueDate(LocalDate.of(2024, 1, 15))
                .expiryDate(LocalDate.of(2027, 1, 15))
                .credentialUrl("https://aws.amazon.com/cert/123")
                .build();

        profileService.addCertification("kunal@test.com", dto);

        verify(certificationRepository, times(1)).save(any(Certification.class));
    }

    @Test
    public void testDeleteCertification_Success() {
        Certification cert = Certification.builder().id(1L).profile(testProfile).build();

        when(userRepository.findByEmail("kunal@test.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(certificationRepository.findById(1L)).thenReturn(Optional.of(cert));

        profileService.deleteCertification("kunal@test.com", 1L);

        verify(certificationRepository, times(1)).delete(cert);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetOrCreateProfile_UserNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        profileService.getOrCreateProfile("unknown@test.com");
    }
}
