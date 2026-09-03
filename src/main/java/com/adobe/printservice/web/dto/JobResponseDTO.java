package com.adobe.printservice.web.dto;

import com.adobe.printservice.model.JobStatus;

import java.time.Instant;

public class JobResponseDTO {

    private String id;

    private JobStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

}
