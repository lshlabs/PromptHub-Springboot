package com.lshlabs.prompthubspring.security;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import com.lshlabs.prompthubspring.user.UserSettingsRepository;
import com.lshlabs.prompthubspring.user.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_public_endpoint_parity;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Tag("contract")
@Tag("integration")
class PublicEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @BeforeEach
    void clean() {
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void dashboardAndUserSummary_arePublic_likeLegacyContract() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("public-summary@example.com");
        user.setPassword("encoded-password");
        user.setUsername("public-summary-user");
        user.setBio("bio");
        userRepository.save(user);

        mockMvc.perform(get("/api/stats/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/users/{username}/summary", "public-summary-user"))
                .andExpect(status().isOk());
    }

    @Test
    void userStats_staysProtected_likeLegacyContract() throws Exception {
        mockMvc.perform(get("/api/stats/user"))
                .andExpect(status().isForbidden());
    }
}
