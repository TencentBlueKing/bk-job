package com.tencent.bk.job;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "management.health.db.enabled=false",
    "management.health.redis.enabled=false"
})
@AutoConfigureMockMvc
class BkJobOpApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldAllowAnonymousSimpleHealthWhenActuatorCredentialsAreEmpty() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.details").doesNotExist())
            .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void shouldFailCloseProtectedActuatorWhenCredentialsAreEmpty() throws Exception {
        mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundForRemovedPaths() throws Exception {
        mockMvc.perform(get("/issueStatus")).andExpect(status().isNotFound());
        mockMvc.perform(post("/issueStatus")).andExpect(status().isNotFound());
        mockMvc.perform(get("/releaseStatus")).andExpect(status().isNotFound());
        mockMvc.perform(post("/releaseStatus")).andExpect(status().isNotFound());
        mockMvc.perform(get("/checkServiceDependency")).andExpect(status().isNotFound());
    }

}
