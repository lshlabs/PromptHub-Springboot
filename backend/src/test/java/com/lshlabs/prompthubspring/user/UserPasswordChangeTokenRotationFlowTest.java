package com.lshlabs.prompthubspring.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lshlabs.prompthubspring.auth.AuthToken;
import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.auth.AuthTokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_pwd_rotate;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class UserPasswordChangeTokenRotationFlowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private AuthTokenRepository authTokenRepository;

    @BeforeEach
    void cleanUp() {
        authTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void passwordChange_revokesOldTokensAndIssuesNewTokens() throws Exception {
        String email = "pwd-change@example.com";
        String oldPassword = "OldPassword!123";
        String newPassword = "NewPassword!456";

        JsonNode registerJson = registerAndRead(email, oldPassword);
        String oldAccess = registerJson.path("token").asText();
        String oldRefresh = registerJson.path("refresh").asText();

        var changeResult = mockMvc.perform(patch("/api/auth/profile/password/")
                        .header("Authorization", "Token " + oldAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "current_password":"%s",
                                  "new_password":"%s",
                                  "new_password_confirm":"%s"
                                }
                                """.formatted(oldPassword, newPassword, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refresh").isString())
                .andReturn();

        JsonNode changeJson = objectMapper.readTree(changeResult.getResponse().getContentAsString());
        String newAccess = changeJson.path("token").asText();
        String newRefresh = changeJson.path("refresh").asText();

        mockMvc.perform(get("/api/auth/info/")
                        .header("Authorization", "Token " + oldAccess))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auth/info/")
                        .header("Authorization", "Token " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/auth/token/refresh/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh":"%s"}
                                """.formatted(oldRefresh)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/token/refresh/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh":"%s"}
                                """.formatted(newRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").isString())
                .andExpect(jsonPath("$.refresh").isString());

        AuthToken oldAccessToken = authTokenRepository.findByToken(oldAccess).orElse(null);
        AuthToken oldRefreshToken = authTokenRepository.findByToken(oldRefresh).orElse(null);
        AuthToken newAccessToken = authTokenRepository.findByToken(newAccess).orElse(null);
        AuthToken newRefreshToken = authTokenRepository.findByToken(newRefresh).orElse(null);

        assertNotNull(oldAccessToken);
        assertNotNull(oldRefreshToken);
        assertNotNull(newAccessToken);
        assertNotNull(newRefreshToken);
        assertTrue(oldAccessToken.getRevokedAt() != null);
        assertTrue(oldRefreshToken.getRevokedAt() != null);
        assertTrue(newAccessToken.getTokenType() == AuthTokenType.ACCESS);
        assertTrue(newRefreshToken.getTokenType() == AuthTokenType.REFRESH);
    }

    private JsonNode registerAndRead(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"%s",
                                  "password_confirm":"%s"
                                }
                                """.formatted(email, password, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
