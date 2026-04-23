package com.lshlabs.prompthubspring.user;

import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void profile_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedUserEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/profile/settings"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auth/profile/sessions"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auth/info"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/auth/profile").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/auth/profile/delete"))
                .andExpect(status().isForbidden());
    }
}
