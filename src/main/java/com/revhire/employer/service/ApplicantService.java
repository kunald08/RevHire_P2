package com.revhire.employer.service;

import com.revhire.application.entity.Application;
import com.revhire.employer.dto.ApplicantProfileDTO;
import com.revhire.employer.dto.ApplicantRowDTO;
import com.revhire.job.entity.Job;
import com.revhire.employer.dto.ApplicationNoteDTO;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicantService {
    
    /**
     * Retrieves a paginated list of jobs for a specific employer email,
     * optionally filtered by a keyword in the job title.
     */
    Page<Job> getEmployerJobsWithApplications(
            String email, 
            String keyword, 
            Pageable pageable
    );
    List<ApplicantRowDTO> getApplicantsByJob(Long jobId);

    long getApplicantCount(Long jobId);

    String getJobTitle(Long jobId);
    void bulkUpdateStatus(List<Long> applicationIds, String action,String comment);

    Application getApplicationEntity(Long appId);
    
    ApplicantProfileDTO getApplicantProfile(Long appId);
    void addNote(Long appId, String note, String employerEmail);
    void deleteNote(Long noteId);
    void updateNote(Long noteId, String newNote);
    ApplicationNoteDTO getNoteForApplication(Long appId);
    void saveOrUpdateNote(Long appId, String noteText, String employerEmail);
}