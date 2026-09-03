package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.web.dto.SubmitJobRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;

    public JobService(JobRepository jobRepository, RenderTemplateRepository renderTemplateRepository) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
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
        return jobRepository.save(job);
    }
}
