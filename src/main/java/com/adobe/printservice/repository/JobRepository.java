package com.adobe.printservice.repository;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findAllByStatus(JobStatus status);

    Optional<Job> findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(JobStatus status, Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Job job
            set job.status = :processingStatus,
                job.attempts = job.attempts + 1,
                job.updatedAt = :now
            where job.id = :id
              and job.status = :queuedStatus
              and job.nextAttemptAt <= :now
            """)
    int claimQueuedJob(
            @Param("id") String id,
            @Param("queuedStatus") JobStatus queuedStatus,
            @Param("processingStatus") JobStatus processingStatus,
            @Param("now") Instant now
    );
}
