package com.revhire.job.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = { ExperienceRangeValidator.class })
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidExperienceRange {

    String message() default "Minimum experience must be less than or equal to maximum experience";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}