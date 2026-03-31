package com.lshlabs.prompthubspring.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lshlabs.prompthubspring.auth.AuthToken;
import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.auth.AuthTokenType;
import com.lshlabs.prompthubspring.post.PostInteractionRepository;
import com.lshlabs.prompthubspring.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_user_flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class UserControllerFlowTest {

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

    @Autowired
    private PostInteractionRepository postInteractionRepository;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void cleanUp() {
        postInteractionRepository.deleteAll();
        postRepository.deleteAll();
        authTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authenticatedProfileSettingsInfoAndSessionsFlow_works() throws Exception {
        AppUser me = createUser("me@example.com", "me");
        me.setProfileImage("/media/profile/me.png");
        userRepository.save(me);
        String accessToken = issueAccessToken(me);
        createActiveSession(me, "session-me-1", Instant.parse("2026-03-01T00:00:00Z"));
        createActiveSession(me, "session-me-2", Instant.parse("2026-03-02T00:00:00Z"));

        mockMvc.perform(get("/api/auth/profile/")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("me"))
                .andExpect(jsonPath("$.settings").exists())
                .andExpect(jsonPath("$.profile_completeness").exists());

        mockMvc.perform(patch("/api/auth/profile/")
                        .header("Authorization", "Token " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "me-updated",
                                "bio", "hello",
                                "location", "Seoul",
                                "github_handle", "mehub"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("프로필이 업데이트되었습니다."))
                .andExpect(jsonPath("$.user.username").value("me-updated"))
                .andExpect(jsonPath("$.user.github_handle").value("mehub"));

        mockMvc.perform(patch("/api/auth/profile/settings/")
                        .header("Authorization", "Token " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "public_profile", false,
                                "data_sharing", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.public_profile").value(false))
                .andExpect(jsonPath("$.data_sharing").value(true));

        mockMvc.perform(get("/api/auth/info/")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(me.getId()))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.username").value("me-updated"))
                .andExpect(jsonPath("$.avatar_url").value("/media/profile/me.png"))
                .andExpect(jsonPath("$.avatar_color1").value("#111111"))
                .andExpect(jsonPath("$.avatar_color2").value("#222222"))
                .andExpect(jsonPath("$.created_at").isNotEmpty());

        mockMvc.perform(get("/api/auth/profile/sessions/")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("session-me-2"))
                .andExpect(jsonPath("$[1].key").value("session-me-1"));
    }

    @Test
    void deletingOtherUsersSession_isRejected() throws Exception {
        AppUser me = createUser("me@example.com", "me");
        AppUser other = createUser("other@example.com", "other");
        String accessToken = issueAccessToken(me);
        createActiveSession(other, "other-session-1", Instant.now());

        mockMvc.perform(delete("/api/auth/profile/sessions/")
                        .header("Authorization", "Token " + accessToken)
                        .param("key", "other-session-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 세션을 찾을 수 없습니다."));
    }

    @Test
    void summary_returnsFrontendContractFields() throws Exception {
        AppUser me = createUser("me@example.com", "me");
        String accessToken = issueAccessToken(me);

        mockMvc.perform(get("/api/auth/users/me/summary/")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("username")))
                .andExpect(jsonPath("$", hasKey("post_count")))
                .andExpect(jsonPath("$", hasKey("total_views")))
                .andExpect(jsonPath("$", hasKey("total_likes_received")))
                .andExpect(jsonPath("$", hasKey("total_bookmarks_received")));
    }

    @Test
    void deleteAccount_removesUserAndBlocksFollowUpProfileAccess() throws Exception {
        AppUser me = createUser("me@example.com", "me");
        String accessToken = issueAccessToken(me);
        createActiveSession(me, "session-me-1", Instant.now());
        createSettings(me);

        mockMvc.perform(delete("/api/auth/profile/delete/")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("계정이 삭제되었습니다."));

        mockMvc.perform(get("/api/auth/profile/")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void endOtherSessions_matchesCompatibilityMessageAndCount() throws Exception {
        AppUser me = createUser("multi@example.com", "multi");
        String accessToken = issueAccessToken(me);
        createActiveSession(me, "session-me-1", Instant.parse("2026-03-01T00:00:00Z"));
        createActiveSession(me, "session-me-2", Instant.parse("2026-03-02T00:00:00Z"));
        createActiveSession(me, "session-me-3", Instant.parse("2026-03-03T00:00:00Z"));

        mockMvc.perform(delete("/api/auth/profile/sessions")
                        .header("Authorization", "Token " + accessToken)
                        .header("X-Session-Key", "session-me-3")
                        .param("all", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("다른 모든 세션을 종료했습니다."))
                .andExpect(jsonPath("$.count").value(2));

        mockMvc.perform(get("/api/auth/profile/sessions")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("session-me-3"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    private AppUser createUser(String email, String username) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setUsername(username);
        user.setAvatarColor1("#111111");
        user.setAvatarColor2("#222222");
        return userRepository.save(user);
    }

    private void createSettings(AppUser user) {
        UserSettings settings = new UserSettings();
        settings.setUser(user);
        userSettingsRepository.save(settings);
    }

    private void createActiveSession(AppUser user, String sessionKey, Instant lastActive) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionKey(sessionKey);
        session.setUserAgent("JUnit");
        session.setIpAddress("127.0.0.1");
        session.setDevice("Mac");
        session.setBrowser("JUnit");
        session.setOs("macOS");
        session.setLastActive(lastActive);
        userSessionRepository.save(session);
    }

    private String issueAccessToken(AppUser user) {
        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setTokenType(AuthTokenType.ACCESS);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        return authTokenRepository.save(token).getToken();
    }
}
