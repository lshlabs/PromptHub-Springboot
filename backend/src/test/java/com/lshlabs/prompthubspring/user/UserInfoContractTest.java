package com.lshlabs.prompthubspring.user;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.auth.AuthToken;
import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.auth.AuthTokenType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_user_info_parity;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class UserInfoContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private AuthTokenRepository authTokenRepository;

    @AfterEach
    void tearDown() {
        authTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void userInfo_matchesCompatibilityContractFields() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("info@example.com");
        user.setPassword("encoded");
        user.setUsername("info-user");
        user.setProfileImage("/media/profile/info.png");
        user.setAvatarColor1("#AAAAAA");
        user.setAvatarColor2("#BBBBBB");
        user = userRepository.save(user);

        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setTokenType(AuthTokenType.ACCESS);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token = authTokenRepository.save(token);

        mockMvc.perform(get("/api/auth/info")
                        .header("Authorization", "Token " + token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("info@example.com"))
                .andExpect(jsonPath("$.username").value("info-user"))
                .andExpect(jsonPath("$.avatar_url").value("/media/profile/info.png"))
                .andExpect(jsonPath("$.avatar_color1").value("#AAAAAA"))
                .andExpect(jsonPath("$.avatar_color2").value("#BBBBBB"))
                .andExpect(jsonPath("$.created_at").isNotEmpty());
    }

    @Test
    void userInfo_returnsNullAvatarUrl_whenProfileImageMissing() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("info-null-avatar@example.com");
        user.setPassword("encoded");
        user.setUsername("info-null-avatar");
        user.setAvatarColor1("#CCCCCC");
        user.setAvatarColor2("#DDDDDD");
        user = userRepository.save(user);

        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setTokenType(AuthTokenType.ACCESS);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token = authTokenRepository.save(token);

        mockMvc.perform(get("/api/auth/info")
                        .header("Authorization", "Token " + token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar_url").value(nullValue()))
                .andExpect(jsonPath("$.avatar_color1").value("#CCCCCC"))
                .andExpect(jsonPath("$.avatar_color2").value("#DDDDDD"));
    }

    @Test
    void userInfo_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/info"))
                .andExpect(status().isForbidden());
    }
}
