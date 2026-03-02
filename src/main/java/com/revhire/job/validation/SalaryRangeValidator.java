package com.revhire.job.validation;

import com.revhire.job.dto.JobRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class SalaryRangeValidator implements ConstraintValidator<ValidSalaryRange, JobRequest> {

    @Override
    public boolean isValid(JobRequest request, ConstraintValidatorContext context) {

        BigDecimal min = request.getSalaryMin();
        BigDecimal max = request.getSalaryMax();

        if (min == null || max == null) {
            return true; // Let field-level validation handle null cases
        }

        return min.compareTo(max) <= 0;
    }
}