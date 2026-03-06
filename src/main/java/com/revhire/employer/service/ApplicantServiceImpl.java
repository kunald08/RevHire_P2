package com.revhire.employer.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.revhire.application.entity.Application;
import com.revhire.application.repository.ApplicationRepository;
import com.revhire.common.enums.ApplicationStatus;
import com.revhire.employer.dto.ApplicantProfileDTO;
import com.revhire.employer.dto.ApplicantRowDTO;
import com.revhire.employer.repository.ApplicantRepository;
import com.revhire.employer.service.ApplicantService;
import com.revhire.job.entity.Job;
import com.revhire.job.repository.JobRepository;
import com.revhire.profile.entity.JobSeekerProfile;
import com.revhire.profile.repository.ProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;
    private final ProfileRepository profileRepository;

    @Override
    public List<ApplicantRowDTO> getApplicantsByJob(Long jobId) {
        return applicationRepository
                .findByJobId(jobId)
                .stream()
                .map(app -> new ApplicantRowDTO(
                        app.getId(),
                        app.getSeeker().getName(),
                        app.getStatus().name(),
                        app.getAppliedAt(),  // ✅ CORRECT FIELD
                        app.getEmployerComment() != null ? app.getEmployerComment() : "-",
                        "-" // ✅ Since you don't have reviewer notes entity
                ))
                .collect(Collectors.toList());
    }

    @Override
    public long getApplicantCount(Long jobId) {
        return applicationRepository.countByJobId(jobId);
    }

    @Override
    public String getJobTitle(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow()
                .getTitle();
    }

    
    @Override
    public Application getApplicationEntity(Long appId) {
        return applicationRepository.findById(appId)
                .orElseThrow();
    }

    @Override
    public Page<Job> getEmployerJobsWithApplications(
            String email,
            String keyword,
            Pageable pageable) {

        String searchKeyword =
                (keyword == null) ? "" : keyword.trim();

        return applicantRepository
                .findByEmployerUserEmailAndTitleContainingIgnoreCase(
                        email,
                        searchKeyword,
                        pageable
                );
    }

	@Override
	public void bulkUpdateStatus(List<Long> ids, String action,String comment) {
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

	    // Step 1: get application
	    Application application = applicationRepository.findById(appId)
	            .orElseThrow(() -> new RuntimeException("Application not found"));

	    // Step 2: get seeker id
	    Long seekerId = application.getSeeker().getId();

	    // Step 3: fetch job seeker profile
	    JobSeekerProfile profile = profileRepository.findByUserId(seekerId)
	            .orElseThrow(() -> new RuntimeException("Profile not found with userId: " + seekerId));

	    return ApplicantProfileDTO.builder()
	            .applicationId(application.getId())
	            .applicantName(application.getSeeker().getName())
	            .seekerId(application.getSeeker().getId())
	            .headline(profile.getHeadline())
	            .summary(profile.getSummary())
	            .profilePicture(profile.getProfilePictureUrl())
	            .coverLetter(application.getCoverLetter())
	            .build();
	}
}