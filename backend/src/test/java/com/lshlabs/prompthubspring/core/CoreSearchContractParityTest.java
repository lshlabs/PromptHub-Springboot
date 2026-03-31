package com.lshlabs.prompthubspring.core;

import com.jayway.jsonpath.JsonPath;
import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.post.*;
import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import com.lshlabs.prompthubspring.user.UserSessionRepository;
import com.lshlabs.prompthubspring.user.UserSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_core_search_parity;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CoreSearchContractParityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PostInteractionRepository postInteractionRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private AiModelRepository aiModelRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AuthTokenRepository authTokenRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    @AfterEach
    void tearDown() {
        postInteractionRepository.deleteAll();
        postRepository.deleteAll();
        aiModelRepository.deleteAll();
        platformRepository.deleteAll();
        categoryRepository.deleteAll();
        authTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    void coreSearch_supportsCompatibilitySearchTypeCsvFiltersAndSort() throws Exception {
        AppUser authorMatch = saveUser("core_writer_keyword");
        AppUser authorOther = saveUser("core_other");

        Platform p1 = savePlatform("OpenAI");
        Platform p2 = savePlatform("Anthropic");
        Category cNormal = saveCategory("일반");
        Category cEtc = saveCategory("기타");
        AiModel mNormal = saveModel(p1, "GPT");
        AiModel mEtc = saveModel(p1, "기타");

        Post titlePost = savePost(authorOther, p1, cNormal, mNormal, "Core Alpha title",
                "prompt body", "ai body", null, null,
                10, 2, 1, new BigDecimal("3.5"), Instant.now().minus(1, ChronoUnit.DAYS));
        Post contentPost = savePost(authorOther, p1, cNormal, mNormal, "Core Beta",
                "needle-content in prompt", "ai body", null, null,
                30, 5, 3, new BigDecimal("5.0"), Instant.now().minus(2, ChronoUnit.DAYS));
        Post authorPost = savePost(authorMatch, p2, cNormal, mNormal, "Core Gamma",
                "prompt body", "ai body", null, null,
                5, 1, 0, new BigDecimal("4.0"), Instant.now().minus(3, ChronoUnit.DAYS));
        Post etcCategoryPost = savePost(authorOther, p1, cEtc, mNormal, "Core Etc Category",
                "prompt body", "ai body", null, "custom-category",
                1, 0, 0, new BigDecimal("2.0"), Instant.now().minus(4, ChronoUnit.DAYS));
        Post etcModelPost = savePost(authorOther, p1, cNormal, mEtc, "Core Etc Model",
                "prompt body", "ai body", "custom-model", null,
                1, 0, 0, new BigDecimal("2.5"), Instant.now().minus(5, ChronoUnit.DAYS));

        MvcResult titleSearch = mockMvc.perform(get("/api/core/search/")
                        .param("q", "Alpha")
                        .param("search_type", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_page").value(1))
                .andReturn();
        List<Map<String, Object>> titleResults = JsonPath.read(titleSearch.getResponse().getContentAsString(), "$.results");
        assertEquals(1, titleResults.size());
        assertEquals(titlePost.getId().intValue(), ((Number) titleResults.get(0).get("id")).intValue());

        MvcResult contentSearch = mockMvc.perform(get("/api/core/search/")
                        .param("q", "needle-content")
                        .param("search_type", "content"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> contentResults = JsonPath.read(contentSearch.getResponse().getContentAsString(), "$.results");
        assertEquals(1, contentResults.size());
        assertEquals(contentPost.getId().intValue(), ((Number) contentResults.get(0).get("id")).intValue());

        MvcResult authorSearch = mockMvc.perform(get("/api/core/search/")
                        .param("q", "core_writer_keyword")
                        .param("search_type", "author"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> authorResults = JsonPath.read(authorSearch.getResponse().getContentAsString(), "$.results");
        assertEquals(1, authorResults.size());
        assertEquals(authorPost.getId().intValue(), ((Number) authorResults.get(0).get("id")).intValue());

        MvcResult titleContentSearch = mockMvc.perform(get("/api/core/search/")
                        .param("q", "needle-content")
                        .param("search_type", "title_content"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> titleContentResults = JsonPath.read(
                titleContentSearch.getResponse().getContentAsString(), "$.results");
        assertEquals(1, titleContentResults.size());
        assertEquals(contentPost.getId().intValue(), ((Number) titleContentResults.get(0).get("id")).intValue());

        MvcResult allSearch = mockMvc.perform(get("/api/core/search/")
                        .param("q", "core_writer_keyword")
                        .param("search_type", "all"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> allResults = JsonPath.read(allSearch.getResponse().getContentAsString(), "$.results");
        assertEquals(1, allResults.size());
        assertEquals(authorPost.getId().intValue(), ((Number) allResults.get(0).get("id")).intValue());

        MvcResult latestSort = mockMvc.perform(get("/api/core/search/")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> latestResults = JsonPath.read(latestSort.getResponse().getContentAsString(), "$.results");
        assertEquals(titlePost.getId().intValue(), ((Number) latestResults.get(0).get("id")).intValue());

        MvcResult oldestSort = mockMvc.perform(get("/api/core/search/")
                        .param("sort", "oldest"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> oldestResults = JsonPath.read(oldestSort.getResponse().getContentAsString(), "$.results");
        assertEquals(etcModelPost.getId().intValue(), ((Number) oldestResults.get(0).get("id")).intValue());

        MvcResult popularSort = mockMvc.perform(get("/api/core/search/")
                        .param("sort", "popular"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> popularResults = JsonPath.read(popularSort.getResponse().getContentAsString(), "$.results");
        assertEquals(contentPost.getId().intValue(), ((Number) popularResults.get(0).get("id")).intValue());

        MvcResult satisfactionSort = mockMvc.perform(get("/api/core/search/")
                        .param("sort", "satisfaction"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> satisfactionResults = JsonPath.read(
                satisfactionSort.getResponse().getContentAsString(), "$.results");
        assertEquals(contentPost.getId().intValue(), ((Number) satisfactionResults.get(0).get("id")).intValue());

        MvcResult viewsSort = mockMvc.perform(get("/api/core/search/")
                        .param("sort", "views"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> viewsResults = JsonPath.read(viewsSort.getResponse().getContentAsString(), "$.results");
        assertEquals(contentPost.getId().intValue(), ((Number) viewsResults.get(0).get("id")).intValue());

        MvcResult categoryFilter = mockMvc.perform(get("/api/core/search/")
                        .param("categories", cEtc.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> categoryResults = JsonPath.read(categoryFilter.getResponse().getContentAsString(), "$.results");
        assertEquals(1, categoryResults.size());
        assertEquals(etcCategoryPost.getId().intValue(), ((Number) categoryResults.get(0).get("id")).intValue());

        MvcResult multiCategoryFilter = mockMvc.perform(get("/api/core/search/")
                        .param("categories", cNormal.getId() + "," + cEtc.getId()))
                .andExpect(status().isOk())
                .andReturn();
        Integer multiCategoryTotalCount = JsonPath.read(
                multiCategoryFilter.getResponse().getContentAsString(), "$.total_count");
        assertTrue(multiCategoryTotalCount >= 5);

        MvcResult modelFilter = mockMvc.perform(get("/api/core/search/")
                        .param("models", mEtc.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> modelResults = JsonPath.read(modelFilter.getResponse().getContentAsString(), "$.results");
        assertEquals(1, modelResults.size());
        assertEquals(etcModelPost.getId().intValue(), ((Number) modelResults.get(0).get("id")).intValue());

        MvcResult multiModelFilter = mockMvc.perform(get("/api/core/search/")
                        .param("models", mNormal.getId() + "," + mEtc.getId()))
                .andExpect(status().isOk())
                .andReturn();
        Integer multiModelTotalCount = JsonPath.read(
                multiModelFilter.getResponse().getContentAsString(), "$.total_count");
        assertTrue(multiModelTotalCount >= 5);

        MvcResult platformFilter = mockMvc.perform(get("/api/core/search/")
                        .param("platforms", p2.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> platformResults = JsonPath.read(platformFilter.getResponse().getContentAsString(), "$.results");
        assertEquals(1, platformResults.size());
        assertEquals(authorPost.getId().intValue(), ((Number) platformResults.get(0).get("id")).intValue());

        // 하위 호환 alias: platform/category 단일 파라미터도 동작
        MvcResult singleAlias = mockMvc.perform(get("/api/core/search/")
                        .param("platform", p2.getId().toString())
                        .param("category", cNormal.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> aliasResults = JsonPath.read(singleAlias.getResponse().getContentAsString(), "$.results");
        assertTrue(aliasResults.stream().allMatch(row ->
                ((Number) row.get("platformId")).longValue() == p2.getId()
                        && ((Number) row.get("categoryId")).longValue() == cNormal.getId()));
    }

    private AppUser saveUser(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser user = new AppUser();
        user.setUsername(prefix + "_" + suffix);
        user.setEmail(prefix + "_" + suffix + "@example.com");
        user.setPassword("encoded");
        return appUserRepository.save(user);
    }

    private Platform savePlatform(String name) {
        Platform p = new Platform();
        p.setName(name + "_" + UUID.randomUUID().toString().substring(0, 8));
        p.setActive(true);
        return platformRepository.save(p);
    }

    private Category saveCategory(String name) {
        for (Category existing : categoryRepository.findAll()) {
            if (name.equals(existing.getName())) {
                return existing;
            }
        }
        Category c = new Category();
        c.setName(name);
        return categoryRepository.save(c);
    }

    private AiModel saveModel(Platform platform, String name) {
        AiModel model = new AiModel();
        model.setPlatform(platform);
        model.setName(name);
        model.setActive(true);
        model.setDeprecated(false);
        model.setSortOrder(1);
        return aiModelRepository.save(model);
    }

    private Post savePost(AppUser author, Platform platform, Category category, AiModel model,
            String title, String prompt, String aiResponse, String modelEtc, String categoryEtc,
            long views, long likes, long bookmarks, BigDecimal satisfaction, Instant createdAt) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setModel(model);
        post.setTitle(title);
        post.setPrompt(prompt);
        post.setAiResponse(aiResponse);
        post.setModelEtc(modelEtc);
        post.setCategoryEtc(categoryEtc);
        post.setTags("tag1,tag2");
        post.setViewCount(views);
        post.setLikeCount(likes);
        post.setBookmarkCount(bookmarks);
        post.setSatisfaction(satisfaction);
        Post saved = postRepository.save(post);
        ReflectionTestUtils.setField(saved, "createdAt", createdAt);
        return postRepository.save(saved);
    }
}
