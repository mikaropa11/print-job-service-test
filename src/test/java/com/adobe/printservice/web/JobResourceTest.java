package com.adobe.printservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class JobResourceTest {

    private static final String INVOICE_TEMPLATE_ID = "b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void submitJob_withKnownTemplate_returnsQueuedJob() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"%s","parameters":{"customer":"Ada"}}
                                """.formatted(INVOICE_TEMPLATE_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void submitJob_withUnknownTemplate_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"unknown-template","parameters":{}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown template: unknown-template"));
    }
}
