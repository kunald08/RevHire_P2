package com.revhire.employer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.revhire.employer.entity.ApplicantNote;

import java.util.List;
import java.util.Optional;

public interface ApplicantNoteRepository extends JpaRepository<ApplicantNote, Long> {
	Optional<ApplicantNote> findByApplicationId(Long applicationId);
}