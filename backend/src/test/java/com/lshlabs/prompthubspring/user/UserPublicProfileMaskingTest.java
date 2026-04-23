package com.lshlabs.prompthubspring.user;

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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_public_profile;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class UserPublicProfileMaskingTest {

    @Autowired
    private MockMvc mockMvc;
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
    void summary_masksBio_whenPublicProfileDisabled_and_exposesWhenEnabledOrMissing() throws Exception {
        AppUser privateUser = createUser("private@example.com", "private-user", "private bio");
        createSettings(privateUser, false);
        String accessToken = issueAccessToken(privateUser);

        mockMvc.perform(get("/api/auth/users/{username}/summary", "private-user")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("private-user"))
                .andExpect(jsonPath("$.bio").isEmpty())
                .andExpect(jsonPath("$.post_count").exists())
                .andExpect(jsonPath("$.total_views").exists())
                .andExpect(jsonPath("$.total_likes_received").exists())
                .andExpect(jsonPath("$.total_bookmarks_received").exists());

        AppUser publicUser = createUser("public@example.com", "public-user", "public bio");
        createSettings(publicUser, true);

        mockMvc.perform(get("/api/auth/users/{username}/summary", "public-user")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("public-user"))
                .andExpect(jsonPath("$.bio").value("public bio"));

        AppUser defaultUser = createUser("default@example.com", "default-user", "default bio");
        mockMvc.perform(get("/api/auth/users/{username}/summary", "default-user")
                        .header("Authorization", "Token " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("default-user"))
                .andExpect(jsonPath("$.bio").value("default bio"));
    }

    private AppUser createUser(String email, String username, String bio) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setUsername(username);
        user.setBio(bio);
        user.setAvatarColor1("#111111");
        user.setAvatarColor2("#222222");
        return userRepository.save(user);
    }

    private void createSettings(AppUser user, boolean publicProfile) {
        UserSettings settings = new UserSettings();
        settings.setUser(user);
        settings.setPublicProfile(publicProfile);
        userSettingsRepository.save(settings);
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
