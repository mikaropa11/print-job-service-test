package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobResourceTest {

    private static final String INVOICE_TEMPLATE_ID = "b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void removeJobs() {
        jobRepository.deleteAll();
    }

    @Test
    void submitJob_withKnownTemplate_persistsQueuedJobAndReturnsCreatedResponse() throws Exception {
        String responseBody = mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"%s","parameters":{"customer":"Ada"}}
                                """.formatted(INVOICE_TEMPLATE_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = objectMapper.readTree(responseBody).get("id").asText();
        Job persistedJob = jobRepository.findById(jobId).orElseThrow();
        assertThat(persistedJob.getTemplateId()).isEqualTo(INVOICE_TEMPLATE_ID);
        assertThat(persistedJob.getParameters()).containsEntry("customer", "Ada");
        assertThat(persistedJob.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(persistedJob.getAttempts()).isZero();
    }

    @Test
    void submitJob_withUnknownTemplate_returnsBadRequestAndDoesNotPersistJob() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"unknown-template","parameters":{}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown template: unknown-template"));

        assertThat(jobRepository.count()).isZero();
    }

    @Test
    void submitJob_withoutTemplateId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parameters":{}}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(jobRepository.count()).isZero();
    }

    @Test
    void submitJob_withBlankTemplateId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"   ","parameters":{}}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(jobRepository.count()).isZero();
    }

    @Test
    void submitJob_withoutParameters_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"%s"}
                                """.formatted(INVOICE_TEMPLATE_ID)))
                .andExpect(status().isBadRequest());

        assertThat(jobRepository.count()).isZero();
    }

    @Test
    void getJobStatus_existingJob_returnsCurrentStatus() throws Exception {
        String responseBody = mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"%s","parameters":{}}
                                """.formatted(INVOICE_TEMPLATE_ID)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String jobId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(get("/jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.attempts").value(0))
                .andExpect(jsonPath("$.resultAvailable").value(false))
                .andExpect(jsonPath("$.errorMessage").value(nullValue()));
    }

    @Test
    void getJobStatus_missingJob_returnsNotFound() throws Exception {
        mockMvc.perform(get("/jobs/{id}", "missing-job"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Job not found: missing-job"));
    }

    @Test
    void getJobStatus_failedJob_includesAttemptsAndError() throws Exception {
        Job job = persistedJob("failed-status-job", JobStatus.FAILED);
        job.setAttempts(4);
        job.setErrorMessage("Rendering failed");
        jobRepository.save(job);

        mockMvc.perform(get("/jobs/{id}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.attempts").value(4))
                .andExpect(jsonPath("$.resultAvailable").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Rendering failed"));
    }

    @Test
    void getJobStatus_doneJob_reportsResultAvailable() throws Exception {
        Job job = persistedJob("done-status-job", JobStatus.DONE);
        job.setResultContent("Rendered document");
        jobRepository.save(job);

        mockMvc.perform(get("/jobs/{id}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.resultAvailable").value(true))
                .andExpect(jsonPath("$.errorMessage").value(nullValue()));
    }

    @Test
    void listJobs_withoutStatus_returnsAllJobs() throws Exception {
        submitJob();
        submitJob();

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listJobs_withStatus_returnsMatchingJobs() throws Exception {
        submitJob();

        mockMvc.perform(get("/jobs").param("status", "QUEUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("QUEUED"));
    }

    @Test
    void listJobs_withInvalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/jobs").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Invalid status. Expected one of: QUEUED, PROCESSING, DONE, FAILED"));
    }

    @Test
    void getResult_doneJob_returnsResultResponse() throws Exception {
        Job job = persistedJob("done-job", JobStatus.DONE);
        job.setResultContent("Rendered document");
        jobRepository.save(job);

        mockMvc.perform(get("/jobs/{id}/result", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Rendered document"));
    }

    @Test
    void getResult_queuedJob_returnsConflict() throws Exception {
        Job job = persistedJob("queued-job", JobStatus.QUEUED);
        jobRepository.save(job);

        mockMvc.perform(get("/jobs/{id}/result", job.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The job result is not available yet"));
    }

    @Test
    void getResult_processingJob_returnsConflict() throws Exception {
        Job job = persistedJob("processing-job", JobStatus.PROCESSING);
        jobRepository.save(job);

        mockMvc.perform(get("/jobs/{id}/result", job.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void getResult_failedJob_returnsUnprocessableContent() throws Exception {
        Job job = persistedJob("failed-job", JobStatus.FAILED);
        job.setErrorMessage("Rendering failed");
        jobRepository.save(job);

        mockMvc.perform(get("/jobs/{id}/result", job.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("The job failed and has no result"));
    }

    @Test
    void getResult_missingJob_returnsNotFound() throws Exception {
        mockMvc.perform(get("/jobs/{id}/result", "missing-job"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Job not found: missing-job"));
    }

    private Job persistedJob(String id, JobStatus status) {
        Instant now = Instant.now();
        Job job = new Job();
        job.setId(id);
        job.setTemplateId(INVOICE_TEMPLATE_ID);
        job.setParameters(Map.of());
        job.setStatus(status);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setNextAttemptAt(now);
        return job;
    }

    private void submitJob() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"%s","parameters":{}}
                                """.formatted(INVOICE_TEMPLATE_ID)))
                .andExpect(status().isCreated());
    }
}
