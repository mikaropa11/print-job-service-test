package com.adobe.printservice.worker;

import com.adobe.printservice.service.JobService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jobs.worker.enabled", havingValue = "true", matchIfMissing = true)
public class JobWorker {

    private final JobService jobService;

    public JobWorker(JobService jobService) {
        this.jobService = jobService;
    }

    @Scheduled(fixedDelayString = "${jobs.worker.poll-interval:1000}")
    public void processNextQueuedJob() {
        jobService.claimNextQueuedJob().ifPresent(jobService::processClaimedJob);
    }
}
