package com.adobe.printservice.service;

import com.adobe.printservice.exception.TransientRenderingException;
import com.adobe.printservice.exception.UnknownTemplateException;
import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.render.Renderer;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.web.dto.SubmitJobRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceTest {

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final RenderTemplateRepository renderTemplateRepository = mock(RenderTemplateRepository.class);
    private final Renderer renderer = mock(Renderer.class);
    private final JobService jobService = new JobService(
            jobRepository,
            renderTemplateRepository,
            renderer,
            3,
            Duration.ofSeconds(30)
    );

    @Test
    void submit_withKnownTemplate_persistsQueuedJob() {
        SubmitJobRequest request = new SubmitJobRequest();
        request.setTemplateId("template-1");
        request.setParameters(Map.of("customer", "Ada"));
        when(renderTemplateRepository.existsById("template-1")).thenReturn(true);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job submittedJob = jobService.submit(request);

        assertThat(submittedJob.getTemplateId()).isEqualTo("template-1");
        assertThat(submittedJob.getParameters()).containsEntry("customer", "Ada");
        assertThat(submittedJob.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(submittedJob.getAttempts()).isZero();
        assertThat(submittedJob.getCreatedAt()).isEqualTo(submittedJob.getUpdatedAt());
        assertThat(submittedJob.getNextAttemptAt()).isEqualTo(submittedJob.getCreatedAt());
        verify(jobRepository).save(submittedJob);
    }

    @Test
    void submit_withUnknownTemplate_throwsExceptionAndDoesNotPersistJob() {
        SubmitJobRequest request = new SubmitJobRequest();
        request.setTemplateId("unknown-template");
        request.setParameters(Map.of());
        when(renderTemplateRepository.existsById("unknown-template")).thenReturn(false);

        assertThatThrownBy(() -> jobService.submit(request))
                .isInstanceOf(UnknownTemplateException.class)
                .hasMessage("Unknown template: unknown-template");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void processClaimedJob_requeuesTransientFailureWithBackoff() {
        Job job = processingJob("job-1", 1);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(renderer.render(job)).thenThrow(new TransientRenderingException("Renderer temporarily unavailable"));

        Instant beforeProcessing = Instant.now();
        jobService.processClaimedJob(job.getId());

        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(job.getErrorMessage()).isEqualTo("Renderer temporarily unavailable");
        assertThat(job.getNextAttemptAt()).isAfterOrEqualTo(beforeProcessing.plusSeconds(30));
        verify(jobRepository).save(job);
    }

    @Test
    void claimNextQueuedJob_returnsJobIdWhenThisWorkerClaimsIt() {
        Job job = processingJob("job-claim", 0);
        job.setStatus(JobStatus.QUEUED);
        job.setNextAttemptAt(Instant.now());
        when(jobRepository.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(JobStatus.QUEUED), any(Instant.class))).thenReturn(Optional.of(job));
        when(jobRepository.claimQueuedJob(
                eq(job.getId()), eq(JobStatus.QUEUED), eq(JobStatus.PROCESSING), any(Instant.class)))
                .thenReturn(1);

        assertThat(jobService.claimNextQueuedJob()).contains(job.getId());
    }

    @Test
    void claimNextQueuedJob_returnsEmptyWhenAnotherWorkerClaimsItFirst() {
        Job job = processingJob("job-race", 0);
        job.setStatus(JobStatus.QUEUED);
        job.setNextAttemptAt(Instant.now());
        when(jobRepository.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(JobStatus.QUEUED), any(Instant.class))).thenReturn(Optional.of(job));
        when(jobRepository.claimQueuedJob(
                eq(job.getId()), eq(JobStatus.QUEUED), eq(JobStatus.PROCESSING), any(Instant.class)))
                .thenReturn(0);

        assertThat(jobService.claimNextQueuedJob()).isEmpty();
    }

    @Test
    void processClaimedJob_marksJobFailedAfterRetriesAreExhausted() {
        Job job = processingJob("job-2", 4);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(renderer.render(job)).thenThrow(new TransientRenderingException("Renderer temporarily unavailable"));

        jobService.processClaimedJob(job.getId());

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Renderer temporarily unavailable");
        verify(jobRepository).save(job);
    }

    @Test
    void processClaimedJob_marksJobDoneWhenRenderingSucceeds() {
        Job job = processingJob("job-3", 1);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(renderer.render(job)).thenReturn("Rendered document");

        jobService.processClaimedJob(job.getId());

        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(job.getResultContent()).isEqualTo("Rendered document");
        assertThat(job.getErrorMessage()).isNull();
        verify(jobRepository).save(job);
    }

    @Test
    void processClaimedJob_marksJobFailedWithoutRetryForUnexpectedError() {
        Job job = processingJob("job-4", 1);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(renderer.render(job)).thenThrow(new IllegalStateException("Invalid render data"));

        jobService.processClaimedJob(job.getId());

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Invalid render data");
        verify(jobRepository).save(job);
    }

    private Job processingJob(String id, int attempts) {
        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.PROCESSING);
        job.setAttempts(attempts);
        job.setParameters(Map.of());
        return job;
    }
}
