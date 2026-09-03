package com.adobe.printservice.worker;

import com.adobe.printservice.service.JobService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobWorkerTest {

    private final JobService jobService = mock(JobService.class);
    private final JobWorker jobWorker = new JobWorker(jobService);

    @Test
    void processNextQueuedJob_processesClaimedJob() {
        when(jobService.claimNextQueuedJob()).thenReturn(Optional.of("job-1"));

        jobWorker.processNextQueuedJob();

        verify(jobService).processClaimedJob("job-1");
    }

    @Test
    void processNextQueuedJob_doesNothingWhenNoJobIsAvailable() {
        when(jobService.claimNextQueuedJob()).thenReturn(Optional.empty());

        jobWorker.processNextQueuedJob();

        verify(jobService, never()).processClaimedJob(anyString());
    }
}
