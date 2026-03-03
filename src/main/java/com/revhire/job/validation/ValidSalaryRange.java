package com.revhire.job.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SalaryRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSalaryRange {

    String message() default "Minimum salary must be less than or equal to maximum salary";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}