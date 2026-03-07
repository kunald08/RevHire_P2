package com.revhire.employer.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.revhire.application.entity.Application;
import com.revhire.application.repository.ApplicationRepository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.employer.dto.*;
import com.revhire.employer.entity.ApplicantNote;
import com.revhire.employer.entity.Employer;
import com.revhire.employer.repository.ApplicantNoteRepository;
import com.revhire.employer.repository.ApplicantRepository;
import com.revhire.employer.repository.EmployerRepository;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import com.revhire.profile.entity.JobSeekerProfile;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.repository.ProfileRepository;
import com.revhire.profile.repository.ResumeRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;
    private final ProfileRepository profileRepository;
    private final ApplicantNoteRepository applicantNoteRepository; 
    private final EmployerRepository employerRepository;
    private final ResumeRepository resumeRepository;
    
    @Override
    public List<ApplicantRowDTO> getApplicantsByJob(Long jobId) {
        return applicationRepository.findByJobId(jobId).stream()
                .map(app -> new ApplicantRowDTO(
                        app.getId(),
                        app.getSeeker().getName(),
                        app.getStatus().name(),
                        app.getAppliedAt(),
                        app.getEmployerComment() != null ? app.getEmployerComment() : "-",
                        getNoteContent(app.getId())
                ))
                .collect(Collectors.toList());
    }
 // Inside ApplicantServiceImpl.java

    private ApplicantRowDTO mapToDTO(Application app) {
        // Fetch the note for this specific application
        // Ensure findByApplicationId exists in your ApplicantNoteRepository
        var note = applicantNoteRepository.findByApplicationId(app.getId()).orElse(null);
        
        return new ApplicantRowDTO(
                app.getId(),
                app.getSeeker().getName(),
                app.getStatus().name(),
                app.getAppliedAt(),
                app.getEmployerComment() != null ? app.getEmployerComment() : "-",
                // This is the crucial part:
                (note != null && note.getNote() != null && !note.getNote().isEmpty()) ? note.getNote() : "No notes yet"
        );
    }

    @Override
    public long getApplicantCount(Long jobId) {
        return applicationRepository.countByJobId(jobId);
    }

    @Override
    public String getJobTitle(Long jobId) {
        return jobRepository.findById(jobId).orElseThrow().getTitle();
    }

    @Override
    public Application getApplicationEntity(Long appId) {
        return applicationRepository.findById(appId).orElseThrow();
    }

    @Override
    public Page<Job> getEmployerJobsWithApplications(String email, String keyword, Pageable pageable) {
        // Ensure we handle empty keywords to avoid SQL LIKE issues
        String searchKeyword = (keyword == null || keyword.isEmpty()) ? "" : keyword.trim();
        return jobRepository.findByEmployerUserEmailAndTitleContainingIgnoreCase(email, searchKeyword, pageable);
    }

    @Override
    public void bulkUpdateStatus(List<Long> ids, String action, String comment) {
        List<Application> applications = applicationRepository.findAllById(ids);
        for (Application app : applications) {
            if ("SHORTLIST".equalsIgnoreCase(action)) {
                app.setStatus(ApplicationStatus.SHORTLISTED);
            } else if ("REJECT".equalsIgnoreCase(action)) {
                app.setStatus(ApplicationStatus.REJECTED);
            }
            app.setEmployerComment(comment);
        }
        applicationRepository.saveAll(applications);
    }
    
    @Override
	public ApplicantProfileDTO getApplicantProfile(Long appId) {
	    Application application = applicationRepository.findById(appId)
	            .orElseThrow(() -> new RuntimeException("Application not found"));
	    
	    Long seekerId = application.getSeeker().getId();
	    
	    // 1. Fetch DTO
	    ApplicantProfileDTO dto = profileRepository.findByUserId(seekerId)
	            .map(profile -> ApplicantProfileDTO.builder()
	                    .headline(profile.getHeadline())
	                    .summary(profile.getSummary())
	                    .profilePicture(profile.getProfilePictureUrl())
	                    .build())
	            .orElse(ApplicantProfileDTO.builder() 
	                    .headline("No profile headline available")
	                    .summary("No summary provided.")
	                    .profilePicture("/images/default-avatar.png")
	                    .build());
	
	    dto.setApplicationId(application.getId());
	    dto.setApplicantName(application.getSeeker().getName());
	    dto.setSeekerId(seekerId);
	    dto.setCoverLetter(application.getCoverLetter());
	    Resume resume = application.getResume();
	    if (resume != null) {
	        dto.setObjective(resume.getObjective() != null ? resume.getObjective() : "No objective provided.");
	        dto.setProjects(resume.getProjects() != null ? resume.getProjects() : "No projects listed.");
	    } else {
	        dto.setObjective("No resume linked.");
	        dto.setProjects("No projects listed.");
	    }
	    // Assuming your Application entity has a getResume() method:
	    if (application.getResume() != null && application.getResume().getFileData() != null && application.getResume().getFileData().length > 0) {
	        dto.setResumeId(application.getResume().getId());
	        dto.setHasFile(true); // File is present
	    } else {
	        dto.setResumeId(null);
	        dto.setHasFile(false); // No file or empty data
	    }
	    
	    return dto;
	}
    @Override
    @Transactional
    public void addNote(Long appId, String noteText, String employerEmail) {
        // 1. Fetch Application
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        // 2. Fetch Employer (You need to implement this findByEmail in your EmployerRepository)
        Employer employer = employerRepository.findByUserEmail(employerEmail)
                .orElseThrow(() -> new RuntimeException("Employer profile not found"));
        
        // 3. Build Note with BOTH Application and Employer
        ApplicantNote note = ApplicantNote.builder()
                .application(app)
                .employer(employer) // <--- THIS WAS MISSING
                .note(noteText)
                .build();
        
        applicantNoteRepository.save(note);
        applicantNoteRepository.flush(); 
    }

    @Override
    public void deleteNote(Long noteId) {
    	applicantNoteRepository.deleteById(noteId); // Use repository!
    }

    @Override
    @Transactional(readOnly = true) // Use readOnly for fetching
    public ApplicationNoteDTO getNoteForApplication(Long appId) {
        return applicantNoteRepository.findByApplicationId(appId)
                .map(n -> ApplicationNoteDTO.builder()
                    .id(n.getId())
                    .note(n.getNote())
                    .build())
                .orElse(ApplicationNoteDTO.builder().note("").build()); // Return empty note if none
    }
    
    @Override
    @Transactional
    public void updateNote(Long noteId, String newNote) {
        ApplicantNote note = applicantNoteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found with id: " + noteId));
        
        note.setNote(newNote);
        applicantNoteRepository.save(note);
    }
    
    @Override
    @Transactional
    public void saveOrUpdateNote(Long appId, String noteText, String employerEmail) {
        // Attempt to find existing note
        ApplicantNote note = applicantNoteRepository.findByApplicationId(appId)
                .orElse(null);

        if (note != null) {
            // Update existing
            note.setNote(noteText);
            applicantNoteRepository.save(note);
        } else {
            // Create new
            addNote(appId, noteText, employerEmail);
        }
    }
    
    @Override
    public Page<Job> getJobsWithPendingApplications(String email, String keyword, Pageable pageable) {
        List<ApplicationStatus> pendingStatuses = List.of(
            ApplicationStatus.APPLIED, 
            ApplicationStatus.UNDER_REVIEW
        );
        
        // Handle empty keyword
        String searchKeyword = (keyword == null || keyword.isEmpty()) ? "" : keyword.trim();
        
        // Pass everything to the repository
        return jobRepository.findPendingJobsByEmailAndStatusAndTitle(
                email, 
                pendingStatuses, 
                searchKeyword, 
                pageable
        );
    }
    @Override
    public List<ApplicantRowDTO> getFilteredApplicantsByJob(Long jobId, List<ApplicationStatus> statuses) {
        return applicationRepository.findByJobId(jobId).stream()
                .filter(app -> statuses.contains(app.getStatus())) // Filter by status
                .map(app -> new ApplicantRowDTO(
                        app.getId(),
                        app.getSeeker().getName(),
                        app.getStatus().name(),
                        app.getAppliedAt(),
                        app.getEmployerComment() != null ? app.getEmployerComment() : "-",
                       getNoteContent(app.getId())
                ))
                .collect(Collectors.toList());
    }
    @Override
    public long getPendingApplicantCount(Long jobId) {
        List<ApplicationStatus> pendingStatuses = List.of(
            ApplicationStatus.APPLIED, 
            ApplicationStatus.UNDER_REVIEW
        );
        return applicantRepository.countByJobIdAndStatusIn(jobId, pendingStatuses);
    }
    private String getNoteContent(Long appId) {
        return applicantNoteRepository.findByApplicationId(appId)
                .map(ApplicantNote::getNote) // Replace getNote() with your actual getter
                .filter(n -> !n.isEmpty())
                .orElse("No notes yet");
    }
    
    @Override
    public ResponseEntity<?> downloadApplicantResume(Long resumeId) {
        // 1. Fetch via your existing ResumeService
        Resume resume = resumeRepository.findById(resumeId)
                .filter(r -> r.getFileData() != null && r.getFileData().length > 0)
                .orElse(null);

        // 2. Handle the "missing file" case gracefully
        if (resume == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("Resume not found or no file uploaded.");
        }

        // 3. Prepare the download
        String contentType = "PDF".equals(resume.getFileType())
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + 
                        (resume.getFileName() != null ? resume.getFileName() : "resume.pdf") + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(resume.getFileData().length)
                .body(resume.getFileData());
    }
    @Override
    public List<ApplicantRowDTO> getFilteredApplicants(Long jobId, String status,String name, Integer minExp, 
                                                       String edu, String cert, String skills) {
        
        // 1. Fetch all applications for the job
        List<Application> apps = applicationRepository.findByJobId(jobId);

        return apps.stream()
            .filter(app -> {
                // Filter by Status
                if (status != null && !status.isEmpty() && !app.getStatus().name().equalsIgnoreCase(status)) {
                    return false;
                }
                if (name != null && !name.isEmpty()) {
                    String nameLower = name.toLowerCase();
                    if (app.getSeeker().getName() == null || !app.getSeeker().getName().toLowerCase().contains(nameLower)) {
                        return false;
                    }
                }
                Long seekerId = app.getSeeker().getId();
                JobSeekerProfile profile = profileRepository.findByUserId(seekerId).orElse(null);
                if (profile == null) return false;

                // Filter by Education (Partial match)
                if (edu != null && !edu.isEmpty()) {
                    boolean hasEdu = profile.getEducations().stream()
                            .anyMatch(e -> e.getDegree().toLowerCase().contains(edu.toLowerCase()));
                    if (!hasEdu) return false;
                }

                // Filter by Certification (Partial match)
                if (cert != null && !cert.isEmpty()) {
                    boolean hasCert = profile.getCertifications().stream()
                            .anyMatch(c -> c.getName().toLowerCase().contains(cert.toLowerCase()));
                    if (!hasCert) return false;
                }
             // Inside your getFilteredApplicants method filter block:
                if (minExp != null) {
                    // Assuming profile.getExperience() returns an Integer or double
                    // Ensure you handle cases where experience might be null in the profile
                    Integer candidateExp = profile.getTotalExperience(); // Adjust this to match your actual field name
                    if (candidateExp == null || candidateExp < minExp) {
                        return false;
                    }
                }
                // Filter by Skills (Must contain all comma-separated skills)
                if (skills != null && !skills.isEmpty()) {
                    String[] requiredSkills = skills.split(",");
                    for (String s : requiredSkills) {
                        boolean hasSkill = profile.getSkills().stream()
                                .anyMatch(sk -> sk.getName().toLowerCase().contains(s.trim().toLowerCase()));
                        if (!hasSkill) return false;
                    }
                }

                return true;
            })
            .map(this::mapToDTO) // Reuse your existing mapping logic
            .collect(Collectors.toList());
    }
    @Override
    public Resume getLatestResumeByProfileId(Long profileId) {
        return resumeRepository.findTopByProfileIdOrderByCreatedAtDesc(profileId)
                .orElse(null);
    }
}