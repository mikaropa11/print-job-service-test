package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.service.JobService;
import com.adobe.printservice.web.dto.JobResponseDTO;
import com.adobe.printservice.web.dto.SubmitJobRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/jobs")
public class JobResource {

    private final JobService jobService;

    public JobResource(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponseDTO> submitJob(@Valid @RequestBody SubmitJobRequest submitJobRequest) {
        Job job = jobService.submit(submitJobRequest);
        JobResponseDTO response = toResponse(job);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    private JobResponseDTO toResponse(Job job) {
        JobResponseDTO response = new JobResponseDTO();
        response.setId(job.getId());
        response.setStatus(job.getStatus());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        return response;
    }
}
