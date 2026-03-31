package com.lshlabs.prompthubspring.core;

import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.post.*;
import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import com.lshlabs.prompthubspring.user.UserSessionRepository;
import com.lshlabs.prompthubspring.user.UserSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CoreTrendingModelParityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TrendingRankingRepository trendingRankingRepository;
    @Autowired
    private TrendingCategoryRepository trendingCategoryRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private AiModelRepository aiModelRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private PostInteractionRepository postInteractionRepository;
    @Autowired
    private AuthTokenRepository authTokenRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;

    private String exactOnTrendingName;
    private String exactOffTrendingName;

    @BeforeEach
    void setUp() {
        AppUser author = saveUser("trending_author");
        Platform platform = savePlatform("Anthropic_" + UUID.randomUUID().toString().substring(0, 8));
        Category category = saveCategory("코딩/프로그래밍");
        AiModel relatedModel = saveModel(platform, "Claude Sonnet 4.5");
        AiModel otherModel = saveModel(platform, "Claude Opus 4.5");

        savePost(author, platform, category, relatedModel, "exact-on-hit-detail", "sonnet45-release", null);
        savePost(author, platform, category, relatedModel, "exact-on-hit-etc", null, "SONNET45 experimental");
        savePost(author, platform, category, relatedModel, "exact-on-miss", "opus45", null);
        savePost(author, platform, category, otherModel, "other-model-miss", "sonnet45-release", null);

        TrendingCategoryEntity trendingCategory = new TrendingCategoryEntity();
        trendingCategory.setName("테스트카테고리");
        trendingCategory.setTitle("테스트 카테고리");
        trendingCategory.setSubtitle("테스트 카테고리 subtitle");
        trendingCategory.setIconName("TestIcon");
        trendingCategory.setOrderNum(1);
        trendingCategory.setActive(true);
        trendingCategory = trendingCategoryRepository.save(trendingCategory);

        exactOnTrendingName = "MODEL_PARITY_EXACT_ON";
        TrendingRankingEntity exactOn = new TrendingRankingEntity();
        exactOn.setCategory(trendingCategory);
        exactOn.setRank(1);
        exactOn.setName(exactOnTrendingName);
        exactOn.setScore("82.0");
        exactOn.setProvider("Anthropic");
        exactOn.setRelatedModel(relatedModel);
        exactOn.setUseExactMatching(true);
        exactOn.setModelDetailContains("Sonnet-4_5");
        exactOn.setModelEtcContains("Sonnet-4_5");
        exactOn.setActive(true);
        trendingRankingRepository.save(exactOn);

        exactOffTrendingName = "MODEL_PARITY_EXACT_OFF";
        TrendingRankingEntity exactOff = new TrendingRankingEntity();
        exactOff.setCategory(trendingCategory);
        exactOff.setRank(2);
        exactOff.setName(exactOffTrendingName);
        exactOff.setScore("81.0");
        exactOff.setProvider("Anthropic");
        exactOff.setRelatedModel(relatedModel);
        exactOff.setUseExactMatching(false);
        exactOff.setModelDetailContains("Sonnet-4_5");
        exactOff.setModelEtcContains("Sonnet-4_5");
        exactOff.setActive(true);
        trendingRankingRepository.save(exactOff);
    }

    @AfterEach
    void tearDown() {
        postInteractionRepository.deleteAll();
        postRepository.deleteAll();
        trendingRankingRepository.deleteAll();
        trendingCategoryRepository.deleteAll();
        aiModelRepository.deleteAll();
        categoryRepository.deleteAll();
        platformRepository.deleteAll();
        authTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void trendingModelInfo_returnsRankingDataWithoutPlaceholders() throws Exception {
        mockMvc.perform(get("/api/core/trending/model/{modelName}/info/", exactOnTrendingName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.trending_name").value(exactOnTrendingName))
                .andExpect(jsonPath("$.data.provider").value("Anthropic"))
                .andExpect(jsonPath("$.data.score").value("82.0"))
                .andExpect(jsonPath("$.data.rank").value(1))
                .andExpect(jsonPath("$.data.category.name").value("테스트카테고리"))
                .andExpect(jsonPath("$.data.category.title").value("테스트 카테고리"))
                .andExpect(jsonPath("$.data.related_model.exact_matching").value(true))
                .andExpect(jsonPath("$.data.related_model.model_detail_filter").value("Sonnet-4_5"))
                .andExpect(jsonPath("$.data.related_model.model_etc_filter").value("Sonnet-4_5"))
                .andExpect(jsonPath("$.data.related_posts_count").value(2));
    }

    @Test
    void trendingModelPosts_appliesExactMatchingWhenEnabled() throws Exception {
        mockMvc.perform(get("/api/core/trending/model/{modelName}/posts/", exactOnTrendingName)
                        .param("page", "1")
                        .param("page_size", "20")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_count").value(2))
                .andExpect(jsonPath("$.trending_model.trending_name").value(exactOnTrendingName))
                .andExpect(jsonPath("$.results[*].title", containsInAnyOrder("exact-on-hit-detail", "exact-on-hit-etc")));
    }

    @Test
    void trendingModelPosts_returnsAllRelatedModelPostsWhenExactMatchingDisabled() throws Exception {
        mockMvc.perform(get("/api/core/trending/model/{modelName}/posts/", exactOffTrendingName)
                        .param("page", "1")
                        .param("page_size", "20")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_count").value(3))
                .andExpect(jsonPath("$.trending_model.trending_name").value(exactOffTrendingName))
                .andExpect(jsonPath("$.results[*].title", containsInAnyOrder(
                        "exact-on-hit-detail", "exact-on-hit-etc", "exact-on-miss"
                )));
    }

    @Test
    void trendingModelEndpoints_returnLegacyCompatible404ContractsWhenModelMissing() throws Exception {
        mockMvc.perform(get("/api/core/trending/model/{modelName}/posts/", "NOT_FOUND_MODEL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("해당 트렌딩 모델을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/core/trending/model/{modelName}/info/", "NOT_FOUND_MODEL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("해당 트렌딩 모델을 찾을 수 없습니다."));
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
        Platform platform = new Platform();
        platform.setName(name);
        platform.setActive(true);
        return platformRepository.save(platform);
    }

    private Category saveCategory(String name) {
        Category category = new Category();
        category.setName(name + "_" + UUID.randomUUID().toString().substring(0, 8));
        return categoryRepository.save(category);
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

    private void savePost(AppUser author, Platform platform, Category category, AiModel model,
            String title, String modelDetail, String modelEtc) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setModel(model);
        post.setCategory(category);
        post.setTitle(title);
        post.setPrompt("prompt");
        post.setAiResponse("ai-response");
        post.setModelDetail(modelDetail);
        post.setModelEtc(modelEtc);
        postRepository.save(post);
    }
}
