package com.tencent.bk.job;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BkJobOpApplicationTests {

    private static final String REMOVED_ISSUE_STATUS_PATH = "/" + "issueStatus";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldReturnNotFoundForRemovedIssueStatusPath() throws Exception {
        mockMvc.perform(get(REMOVED_ISSUE_STATUS_PATH)).andExpect(status().isNotFound());
        mockMvc.perform(post(REMOVED_ISSUE_STATUS_PATH)).andExpect(status().isNotFound());
    }

}
