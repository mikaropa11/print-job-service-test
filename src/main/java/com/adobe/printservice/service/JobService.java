package com.adobe.printservice.service;

import com.adobe.printservice.exception.UnknownTemplateException;
import com.adobe.printservice.exception.TransientRenderingException;
import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.render.Renderer;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.web.dto.SubmitJobRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.List;


@Service
public class JobService {

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;
    private final Renderer renderer;
    private final int maxRetries;
    private final Duration retryBackoff;

    public JobService(
            JobRepository jobRepository,
            RenderTemplateRepository renderTemplateRepository,
            Renderer renderer,
            @Value("${jobs.retry.max-retries}") int maxRetries,
            @Value("${jobs.retry.backoff}") Duration retryBackoff
    ) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
        this.renderer = renderer;
        this.maxRetries = maxRetries;
        this.retryBackoff = retryBackoff;
    }

    @Transactional
    public Job submit(SubmitJobRequest request) {
        if (!renderTemplateRepository.existsById(request.getTemplateId())) {
            throw new UnknownTemplateException(request.getTemplateId());
        }

        Instant now = Instant.now();
        Job job = new Job();
        job.setTemplateId(request.getTemplateId());
        job.setParameters(request.getParameters());
        job.setStatus(JobStatus.QUEUED);
        job.setAttempts(0);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setNextAttemptAt(now);
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Job get(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }

    @Transactional(readOnly = true)
    public List<Job> list(JobStatus status) {
        if (status == null) {
            return jobRepository.findAll();
        }
        return jobRepository.findAllByStatus(status);
    }

    @Transactional
    public Optional<String> claimNextQueuedJob() {
        Instant now = Instant.now();
        return jobRepository.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(JobStatus.QUEUED, now)
                .filter(job -> jobRepository.claimQueuedJob(
                        job.getId(), JobStatus.QUEUED, JobStatus.PROCESSING, now) == 1)
                .map(Job::getId);
    }

    public void processClaimedJob(String jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Claimed job no longer exists: " + jobId));

        try {
            markDone(job, renderer.render(job));
        } catch (TransientRenderingException exception) {
            scheduleRetryOrMarkFailed(job, exception.getMessage());
        } catch (RuntimeException exception) {
            markFailed(job, exception.getMessage());
        }
    }

    private void markDone(Job job, String resultContent) {
        job.setStatus(JobStatus.DONE);
        job.setResultContent(resultContent);
        job.setErrorMessage(null);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }

    private void scheduleRetryOrMarkFailed(Job job, String errorMessage) {
        Instant now = Instant.now();
        job.setErrorMessage(errorMessage);
        job.setUpdatedAt(now);

        if (job.getAttempts() <= maxRetries) {
            job.setStatus(JobStatus.QUEUED);
            job.setNextAttemptAt(now.plus(retryBackoff));
        } else {
            job.setStatus(JobStatus.FAILED);
        }
        jobRepository.save(job);
    }

    private void markFailed(Job job, String errorMessage) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }
}
