package com.revhire.job.scheduler;

import com.revhire.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that auto-closes jobs whose deadline has passed.
 * Runs every day at midnight.
 */
@Component
@RequiredArgsConstructor
public class JobExpiryScheduler {

    private static final Logger logger = LogManager.getLogger(JobExpiryScheduler.class);

    private final JobService jobService;

    /** Runs daily at 00:05 AM — closes any ACTIVE job past its deadline. */
    @Scheduled(cron = "0 5 0 * * *")
    public void autoCloseExpiredJobs() {
        logger.info("Running job expiry check...");
        int closed = jobService.closeExpiredJobs();
        logger.info("Job expiry check complete — {} job(s) auto-closed", closed);
    }
}
