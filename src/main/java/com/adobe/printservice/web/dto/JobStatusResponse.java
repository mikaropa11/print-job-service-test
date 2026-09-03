package com.adobe.printservice.web.dto;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;

public class JobStatusResponse {

    private JobStatus status;
    private int attempts;
    private boolean resultAvailable;
    private String errorMessage;

    public static JobStatusResponse from(Job job) {
        JobStatusResponse response = new JobStatusResponse();
        response.status = job.getStatus();
        response.attempts = job.getAttempts();
        response.resultAvailable = job.getStatus() == JobStatus.DONE && job.getResultContent() != null;
        response.errorMessage = job.getStatus() == JobStatus.FAILED ? job.getErrorMessage() : null;
        return response;
    }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public boolean isResultAvailable() { return resultAvailable; }
    public void setResultAvailable(boolean resultAvailable) { this.resultAvailable = resultAvailable; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
