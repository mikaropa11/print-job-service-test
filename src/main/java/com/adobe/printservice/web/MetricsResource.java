package com.adobe.printservice.web;

import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsResource {

    private final JobRepository jobRepository;

    public MetricsResource(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public Map<JobStatus, Long> jobCountsByStatus() {
        EnumMap<JobStatus, Long> counts = new EnumMap<>(JobStatus.class);
        for (JobStatus status : JobStatus.values()) {
            counts.put(status, 0L);
        }
        jobRepository.findAll().forEach(job -> counts.compute(job.getStatus(), (status, count) -> count + 1));
        return counts;
    }
}
