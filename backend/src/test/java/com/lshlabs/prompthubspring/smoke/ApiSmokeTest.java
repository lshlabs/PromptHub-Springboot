package com.lshlabs.prompthubspring.smoke;

import org.junit.jupiter.api.Tag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lshlabs.prompthubspring.post.AiModel;
import com.lshlabs.prompthubspring.post.AiModelRepository;
import com.lshlabs.prompthubspring.post.Category;
import com.lshlabs.prompthubspring.post.CategoryRepository;
import com.lshlabs.prompthubspring.post.Platform;
import com.lshlabs.prompthubspring.post.PlatformRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("smoke")
class ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AiModelRepository aiModelRepository;

    @Test
    void criticalFrontendFlow_login_list_detail_like_bookmark_smoke() throws Exception {
        String writerEmail = "writer_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String writerPassword = "P@ssword123!";
        String writerToken = registerAndGetToken(writerEmail, writerPassword);

        String viewerEmail = "viewer_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String viewerPassword = "P@ssword123!";
        String viewerToken = registerAndGetToken(viewerEmail, viewerPassword);

        Platform platform = platformRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .findFirst()
                .orElseGet(this::createSmokePlatform);
        Category category = categoryRepository.findAllByOrderByNameAsc().stream()
                .findFirst()
                .orElseGet(this::createSmokeCategory);
        AiModel model = aiModelRepository
                .findDisplayableByPlatformId(platform.getId())
                .stream().findFirst().orElseGet(() -> createSmokeModel(platform));

        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("title", "API Smoke 게시글");
        createPayload.put("platform", platform.getId());
        createPayload.put("model", model == null ? null : model.getId());
        createPayload.put("category", category.getId());
        createPayload.put("tags", java.util.List.of("smoke", "api"));
        createPayload.put("satisfaction", 4.5);
        createPayload.put("prompt", "api smoke prompt 본문입니다.");
        createPayload.put("ai_response", "api smoke ai response 본문입니다.");
        createPayload.put("additional_opinion", "api smoke additional opinion");

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + writerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long postId = created.path("data").path("id").asLong(-1);
        if (postId <= 0) {
            postId = created.path("id").asLong(-1);
        }

        MvcResult listResult = mockMvc.perform(get("/api/posts")
                        .param("page", "1")
                        .param("page_size", "20"))
                .andExpect(status().isOk())
                .andReturn();
        if (postId <= 0) {
            JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
            postId = listJson.path("data").path("results").path(0).path("id").asLong(-1);
        }
        assertTrue(postId > 0);

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk());

        MvcResult likeResult = mockMvc.perform(post("/api/posts/{id}/like", postId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode likeJson = objectMapper.readTree(likeResult.getResponse().getContentAsString());
        assertTrue(likeJson.path("data").path("is_liked").asBoolean());

        MvcResult bookmarkResult = mockMvc.perform(post("/api/posts/{id}/bookmark", postId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode bookmarkJson = objectMapper.readTree(bookmarkResult.getResponse().getContentAsString());
        assertTrue(bookmarkJson.path("data").path("is_bookmarked").asBoolean());

        mockMvc.perform(get("/api/posts/liked-posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/bookmarked-posts")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stats/dashboard")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenFlow_smoke() throws Exception {
        String email = "refresh_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String password = "P@ssword123!";
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
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

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String refresh = registerJson.path("refresh").asText();
        assertNotNull(refresh);
        assertTrue(!refresh.isBlank());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh":"%s"}
                                """.formatted(refresh)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        assertTrue(refreshJson.path("access").asText().length() > 10);
        assertTrue(refreshJson.path("refresh").asText().length() > 10);
        assertNotEquals(refresh, refreshJson.path("refresh").asText(), "refresh rotation should return a new token");
    }

    private String registerAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
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

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = json.path("token").asText();
        assertNotNull(token);
        assertTrue(!token.isBlank());
        return token;
    }

    private Platform createSmokePlatform() {
        Platform platform = new Platform();
        platform.setName("SmokePlatform-" + UUID.randomUUID().toString().substring(0, 8));
        platform.setSlug("smoke-platform");
        platform.setActive(true);
        return platformRepository.save(platform);
    }

    private Category createSmokeCategory() {
        Category category = new Category();
        category.setName("SmokeCategory-" + UUID.randomUUID().toString().substring(0, 8));
        return categoryRepository.save(category);
    }

    private AiModel createSmokeModel(Platform platform) {
        AiModel model = new AiModel();
        model.setPlatform(platform);
        model.setName("SmokeModel-" + UUID.randomUUID().toString().substring(0, 8));
        model.setSlug("smoke-model");
        model.setActive(true);
        model.setDeprecated(false);
        model.setSortOrder(1);
        return aiModelRepository.save(model);
    }
}
