package com.adobe.printservice.exception;

import com.adobe.printservice.model.JobStatus;

public class JobResultUnavailableException extends RuntimeException {

    private final JobStatus jobStatus;

    public JobResultUnavailableException(JobStatus jobStatus) {
        super(jobStatus == JobStatus.FAILED
                ? "The job failed and has no result"
                : "The job result is not available yet");
        this.jobStatus = jobStatus;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }
}
