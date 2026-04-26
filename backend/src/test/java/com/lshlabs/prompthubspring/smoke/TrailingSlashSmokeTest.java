package com.lshlabs.prompthubspring.smoke;

import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("smoke")
class TrailingSlashSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void trailingSlashRoutes_areNotHandledInsideApplication() throws Exception {
        mockMvc.perform(get("/api/core/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts")
                        .param("page", "1")
                        .param("page_size", "5"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/core/trending/category-rankings"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/core/health/"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/posts/")
                        .param("page", "1")
                        .param("page_size", "5"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/core/trending/category-rankings/"))
                .andExpect(status().isNotFound());
    }
}
