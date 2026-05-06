package com.lshlabs.prompthubspring.smoke;

import org.junit.jupiter.api.Tag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("smoke")
class ReleaseGateSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicCriticalApis_smokeGate() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"release_gate_user@example.com",
                                  "password":"P@ssword123!",
                                  "password_confirm":"P@ssword123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String username = registerJson.path("user").path("username").asText();

        mockMvc.perform(get("/api/core/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/platforms"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/core/trending/category-rankings"))
                .andExpect(status().isOk());

        // 레거시 기준에서 대시보드 요약 API는 로그인 없이 접근 가능해야 한다.
        mockMvc.perform(get("/api/stats/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/users/{username}/summary", username))
                .andExpect(status().isOk());
    }
}
