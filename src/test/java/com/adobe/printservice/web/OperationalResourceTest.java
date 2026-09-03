package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationalResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @AfterEach
    void removeJobs() {
        jobRepository.deleteAll();
    }

    @Test
    void live_returnsUp() throws Exception {
        mockMvc.perform(get("/health/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void ready_whenDatabaseIsAvailable_returnsReady() throws Exception {
        mockMvc.perform(get("/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void metrics_returnsCountsForEveryStatus() throws Exception {
        jobRepository.save(job("queued-job", JobStatus.QUEUED));
        jobRepository.save(job("done-job", JobStatus.DONE));
        jobRepository.save(job("failed-job", JobStatus.FAILED));

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.QUEUED").value(1))
                .andExpect(jsonPath("$.PROCESSING").value(0))
                .andExpect(jsonPath("$.DONE").value(1))
                .andExpect(jsonPath("$.FAILED").value(1));
    }

    private Job job(String id, JobStatus status) {
        Instant now = Instant.now();
        Job job = new Job();
        job.setId(id);
        job.setTemplateId("template-1");
        job.setParameters(Map.of());
        job.setStatus(status);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setNextAttemptAt(now);
        return job;
    }
}
