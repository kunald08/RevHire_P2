package com.revhire.job.validation;

import com.revhire.job.dto.JobRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExperienceRangeValidator implements ConstraintValidator<ValidExperienceRange, JobRequest> {

    @Override
    public boolean isValid(JobRequest request, ConstraintValidatorContext context) {

        Integer min = request.getExperienceMin();
        Integer max = request.getExperienceMax();

        if (min == null || max == null) {
            return true;
        }

        return min <= max;
    }
}