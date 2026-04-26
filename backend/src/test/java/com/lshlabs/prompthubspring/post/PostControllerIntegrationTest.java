package com.lshlabs.prompthubspring.post;

import org.junit.jupiter.api.Tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.lshlabs.prompthubspring.auth.AuthToken;
import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.auth.AuthTokenType;
import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import com.lshlabs.prompthubspring.user.UserSessionRepository;
import com.lshlabs.prompthubspring.user.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_post_integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Tag("integration")
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private AuthTokenRepository authTokenRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostInteractionRepository postInteractionRepo;
    @Autowired
    private AiModelRepository aiModelRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @BeforeEach
    void cleanUp() {
        postInteractionRepo.deleteAll();
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
    void crud_fullFlow_and_contractFields() throws Exception {
        AppUser author = saveUser("author");
        String authorToken = issueAccessToken(author);
        Platform platform = savePlatform("OpenAI");
        Category category = saveCategory("일반");
        AiModel model = saveModel(platform, "gpt-4.1");

        Map<String, Object> createPayload = payload(platform.getId(), model.getId(), category.getId(), "초기 제목");

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value("초기 제목"))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data", hasKey("prompt")))
                .andExpect(jsonPath("$.data", hasKey("aiResponse")))
                .andReturn();

        Long postId = ((Number) JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.results").isArray())
                .andExpect(jsonPath("$.data.pagination.current_page").value(1))
                .andExpect(jsonPath("$.data.pagination.total_count").value(1))
                .andExpect(jsonPath("$.data.results[0].id").value(postId))
                .andExpect(jsonPath("$.data.results[0]", hasKey("title")))
                .andExpect(jsonPath("$.data.results[0]", hasKey("platformId")))
                .andExpect(jsonPath("$.data.results[0]", hasKey("modelId")))
                .andExpect(jsonPath("$.data.results[0]", hasKey("categoryId")))
                .andExpect(jsonPath("$.data.results[0].relativeTime", not(is(emptyString()))));

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(postId))
                .andExpect(jsonPath("$.data", hasKey("ai_response")))
                .andExpect(jsonPath("$.data", hasKey("additional_opinion")))
                .andExpect(jsonPath("$.data", hasKey("isAuthor")))
                .andExpect(jsonPath("$.data.relativeTime", not(is(emptyString()))));

        Map<String, Object> updatePayload = payload(platform.getId(), model.getId(), category.getId(), "수정 제목");
        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.title").value("수정 제목"));

        mockMvc.perform(delete("/api/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_and_delete_forbidden_for_nonAuthor() throws Exception {
        AppUser author = saveUser("author");
        AppUser other = saveUser("other");
        String authorToken = issueAccessToken(author);
        String otherToken = issueAccessToken(other);

        Platform platform = savePlatform("Anthropic");
        Category category = saveCategory("일반");
        AiModel model = saveModel(platform, "claude-3.5");
        Long postId = createPost(authorToken, payload(platform.getId(), model.getId(), category.getId(), "작성자 글"));

        mockMvc.perform(patch("/api/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload(platform.getId(), model.getId(), category.getId(), "남의 글 수정"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("게시글을 수정할 권한이 없습니다."));

        mockMvc.perform(delete("/api/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("게시글을 삭제할 권한이 없습니다."));
    }

    @Test
    void create_rejects_invalidCombinations_fromDtoAndServiceValidation() throws Exception {
        AppUser author = saveUser("author");
        String authorToken = issueAccessToken(author);
        Platform googlePlatform = savePlatform("Google");
        Platform openAiPlatform = savePlatform("OpenAI");
        Category category = saveCategory("일반");
        Category etcCategory = saveCategory("기타");
        AiModel modelOnOpenAi = saveModel(openAiPlatform, "gpt-4.1");

        Map<String, Object> mismatchPayload = payload(googlePlatform.getId(), modelOnOpenAi.getId(), etcCategory.getId(), "검증 글");
        mismatchPayload.put("category_etc", "직접입력");
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("선택한 모델이 플랫폼과 일치하지 않습니다."));

        Map<String, Object> missingCategoryEtc = payload(googlePlatform.getId(), null, etcCategory.getId(), "검증 글2");
        missingCategoryEtc.put("model_etc", "직접입력");
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingCategoryEtc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("기타 카테고리를 선택한 경우 category_etc를 입력해야 합니다."));

        Map<String, Object> dtoInvalidPayload = payload(googlePlatform.getId(), null, etcCategory.getId(), "bad");
        dtoInvalidPayload.put("category_etc", "직접입력");
        dtoInvalidPayload.put("satisfaction", new BigDecimal("5.5"));
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효성 검사 실패"));

        Map<String, Object> invalidHalfStepPayload = payload(googlePlatform.getId(), modelOnOpenAi.getId(), etcCategory.getId(), "0.5 단위 위반");
        invalidHalfStepPayload.put("category_etc", "직접입력");
        invalidHalfStepPayload.put("platform", openAiPlatform.getId());
        invalidHalfStepPayload.put("satisfaction", new BigDecimal("4.3"));
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidHalfStepPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("만족도는 0.5점 단위로 입력해야 합니다."));

        Platform etcPlatform = savePlatformExact("기타");
        AiModel etcModel = saveModelExact(etcPlatform, "기타");
        AiModel nonEtcOnEtcPlatform = saveModelExact(etcPlatform, "gpt-etc-platform");

        Map<String, Object> missingModelAndEtc = payload(googlePlatform.getId(), null, category.getId(), "모델 없음");
        missingModelAndEtc.put("model_etc", "");
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingModelAndEtc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("모델을 선택하거나 기타 모델명을 입력해야 합니다."));

        Map<String, Object> etcPlatformNonEtcModel = payload(etcPlatform.getId(), nonEtcOnEtcPlatform.getId(),
                category.getId(), "기타 플랫폼 비기타 모델");
        etcPlatformNonEtcModel.put("model_etc", "");
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(etcPlatformNonEtcModel)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("플랫폼이 기타인 경우 기타 모델만 선택할 수 있습니다."));

        Map<String, Object> missingEtcNamePayload = payload(etcPlatform.getId(), etcModel.getId(),
                category.getId(), "기타 플랫폼 model_etc 누락");
        missingEtcNamePayload.put("model_etc", "");
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingEtcNamePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("플랫폼이 기타인 경우 기타 모델명을 입력해야 합니다."));

        Map<String, Object> etcPlatformWithDetail = payload(etcPlatform.getId(), etcModel.getId(),
                category.getId(), "기타 플랫폼 상세모델");
        etcPlatformWithDetail.put("model_etc", "직접입력");
        etcPlatformWithDetail.put("model_detail", "detail");
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(etcPlatformWithDetail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("플랫폼이 기타인 경우 상세 모델명을 사용할 수 없습니다."));

        Platform normalPlatform = savePlatform("Anthropic");
        AiModel etcModelOnNormal = saveModelExact(normalPlatform, "기타");
        Map<String, Object> etcModelWithDetail = payload(normalPlatform.getId(), etcModelOnNormal.getId(),
                category.getId(), "기타 모델 상세");
        etcModelWithDetail.put("model_etc", "직접입력");
        etcModelWithDetail.put("model_detail", "detail");
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(etcModelWithDetail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'기타' 모델에서는 상세 모델명을 사용할 수 없습니다."));

        Map<String, Object> allowedEtcCombination = payload(etcPlatform.getId(), etcModel.getId(),
                category.getId(), "허용 조합");
        allowedEtcCombination.put("model_etc", "커스텀 모델");
        allowedEtcCombination.put("model_detail", "");
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allowedEtcCombination)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void suggestModels_appliesLegacyScoring_andReturnsSlugContract() throws Exception {
        Platform openai = savePlatformExact("OpenAI");
        openai.setSlug("openai");
        platformRepository.save(openai);

        Platform anthropic = savePlatformExact("Anthropic");
        anthropic.setSlug("anthropic");
        platformRepository.save(anthropic);

        saveModelWithSlug(openai, "GPT-5.2", "gpt-5.2", 2);
        saveModelWithSlug(openai, "General Assistant", "assistant-core", 3);
        saveModelWithSlug(anthropic, "Claude Sonnet 4.5", "claude-sonnet-4.5", 1);

        mockMvc.perform(get("/api/posts/models/suggest")
                        .param("query", "gpt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.query").value("gpt"))
                .andExpect(jsonPath("$.data.suggestions[0].name").value("GPT-5.2"))
                .andExpect(jsonPath("$.data.suggestions[0].slug").value("gpt-5.2"))
                .andExpect(jsonPath("$.data.suggestions[0].platform.slug").value("openai"))
                .andExpect(jsonPath("$.data.suggestions[0].platform.id", is(openai.getId().intValue())));

        mockMvc.perform(get("/api/posts/models/suggest")
                        .param("query", "anth")
                        .param("platform_id", anthropic.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.suggestions[0].platform.name").value("Anthropic"))
                .andExpect(jsonPath("$.data.suggestions[0].platform.slug").value("anthropic"));
    }

    @Test
    void canonicalUris_work_and_legacyAliases_areRemoved() throws Exception {
        AppUser author = saveUser("ctrl02_author");
        String authorToken = issueAccessToken(author);
        Platform platform = savePlatform("OpenAI");
        Category category = saveCategory("일반");
        AiModel model = saveModel(platform, "gpt-4.1");

        Map<String, Object> createPayload = payload(platform.getId(), model.getId(), category.getId(), "표준 URI 생성");

        MvcResult canonicalCreate = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        Long canonicalPostId = ((Number) JsonPath.read(canonicalCreate.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(patch("/api/posts/{postId}", canonicalPostId)
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload(platform.getId(), model.getId(), category.getId(), "표준 URI 수정"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/posts/{postId}", canonicalPostId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/posts/create")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload(platform.getId(), model.getId(), category.getId(), "레거시 URI 생성"))))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch("/api/posts/{postId}/update", canonicalPostId)
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload(platform.getId(), model.getId(), category.getId(), "레거시 URI 수정"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/posts/{postId}/delete", canonicalPostId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void selfLikeAndBookmark_followCompatibilityContract_messageAndNoCountIncrease() throws Exception {
        AppUser author = saveUser("self_action_author");
        AppUser other = saveUser("self_action_other");
        String authorToken = issueAccessToken(author);
        String otherToken = issueAccessToken(other);
        Platform platform = savePlatform("OpenAI");
        Category category = saveCategory("일반");
        AiModel model = saveModel(platform, "gpt-4.1");

        Long postId = createPost(authorToken, payload(platform.getId(), model.getId(), category.getId(), "자기글 반응 검증"));

        mockMvc.perform(post("/api/posts/{postId}/like", postId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("자신의 게시글에는 좋아요를 누를 수 없습니다."))
                .andExpect(jsonPath("$.data.is_liked").value(false))
                .andExpect(jsonPath("$.data.like_count").value(0));

        mockMvc.perform(post("/api/posts/{postId}/bookmark", postId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("자신의 게시글에는 북마크를 할 수 없습니다."))
                .andExpect(jsonPath("$.data.is_bookmarked").value(false))
                .andExpect(jsonPath("$.data.bookmark_count").value(0));

        mockMvc.perform(post("/api/posts/{postId}/like", postId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data.is_liked").value(true))
                .andExpect(jsonPath("$.data.like_count").value(1));
    }

    @Test
    void create_enforcesLegacyTagRules_and_preservesDuplicateTags() throws Exception {
        AppUser author = saveUser("tag_rule_author");
        String authorToken = issueAccessToken(author);
        Platform platform = savePlatform("OpenAI");
        Category category = saveCategory("일반");
        AiModel model = saveModel(platform, "gpt-4.1");

        Map<String, Object> overLimitTagsPayload = payload(platform.getId(), model.getId(), category.getId(), "태그 초과");
        overLimitTagsPayload.put("tags", List.of("t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10", "t11"));
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overLimitTagsPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효성 검사 실패"))
                .andExpect(jsonPath("$.errors.tags[0]").value("태그는 최대 10개까지 입력할 수 있습니다."));

        Map<String, Object> blankTagPayload = payload(platform.getId(), model.getId(), category.getId(), "빈 태그 검증");
        blankTagPayload.put("tags", List.of("valid", "   "));
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankTagPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효성 검사 실패"))
                .andExpect(jsonPath("$.errors['tags[1]'][0]").value("빈 태그는 입력할 수 없습니다."));

        Map<String, Object> duplicateTagsPayload = payload(platform.getId(), model.getId(), category.getId(), "중복 태그 유지");
        duplicateTagsPayload.put("tags", List.of("dup", "dup", "unique"));
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateTagsPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.tags[0]").value("dup"))
                .andExpect(jsonPath("$.data.tags[1]").value("dup"))
                .andExpect(jsonPath("$.data.tags[2]").value("unique"));
    }

    @Test
    void postsList_supportsCompatibilityQueryContract_sortSearchFilterExclude() throws Exception {
        AppUser authorMatch = saveUser("writer_keyword");
        AppUser authorOther = saveUser("other");

        Platform openAiPlatform = savePlatformExact("OpenAI");
        Platform anthropicPlatform = savePlatformExact("Anthropic");
        Category generalCategory = saveCategory("일반");
        Category etcCategory = saveCategory("기타");
        AiModel defaultModel = saveModelExact(openAiPlatform, "GPT");
        AiModel etcModel = saveModelExact(openAiPlatform, "기타");

        Post titlePost = savePostDirect(authorOther, openAiPlatform, generalCategory, defaultModel, "Alpha title post",
                "prompt body", "ai body", "opinion", null, null,
                10, 2, 1, new BigDecimal("3.5"), Instant.now().minus(1, ChronoUnit.DAYS));
        Post contentPost = savePostDirect(authorOther, openAiPlatform, generalCategory, defaultModel, "Beta",
                "needle-content in prompt", "ai body", "opinion", null, null,
                30, 5, 3, new BigDecimal("5.0"), Instant.now().minus(2, ChronoUnit.DAYS));
        Post authorPost = savePostDirect(authorMatch, anthropicPlatform, generalCategory, defaultModel, "Gamma",
                "prompt body", "ai body", "opinion", null, null,
                5, 1, 0, new BigDecimal("4.0"), Instant.now().minus(3, ChronoUnit.DAYS));
        Post etcCategoryPost = savePostDirect(authorOther, openAiPlatform, etcCategory, defaultModel, "Etc Category",
                "prompt body", "ai body", "opinion", null, "custom-category",
                1, 0, 0, new BigDecimal("2.0"), Instant.now().minus(4, ChronoUnit.DAYS));
        Post etcModelPost = savePostDirect(authorOther, openAiPlatform, generalCategory, etcModel, "Etc Model",
                "prompt body", "ai body", "opinion", "custom-model", null,
                1, 0, 0, new BigDecimal("2.5"), Instant.now().minus(5, ChronoUnit.DAYS));

        MvcResult titleSearch = mockMvc.perform(get("/api/posts")
                        .param("search", "Alpha")
                        .param("search_type", "title"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> titleResults = JsonPath.read(titleSearch.getResponse().getContentAsString(), "$.data.results");
        assertEquals(1, titleResults.size());
        assertEquals(titlePost.getId().intValue(), ((Number) titleResults.get(0).get("id")).intValue());

        MvcResult contentSearch = mockMvc.perform(get("/api/posts")
                        .param("search", "needle-content")
                        .param("search_type", "content"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> contentResults = JsonPath.read(contentSearch.getResponse().getContentAsString(), "$.data.results");
        assertEquals(1, contentResults.size());
        assertEquals(contentPost.getId().intValue(), ((Number) contentResults.get(0).get("id")).intValue());

        MvcResult authorSearch = mockMvc.perform(get("/api/posts")
                        .param("search", "writer_keyword")
                        .param("search_type", "author"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> authorResults = JsonPath.read(authorSearch.getResponse().getContentAsString(), "$.data.results");
        assertEquals(1, authorResults.size());
        assertEquals(authorPost.getId().intValue(), ((Number) authorResults.get(0).get("id")).intValue());

        MvcResult popularSort = mockMvc.perform(get("/api/posts")
                        .param("sort_by", "popular"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> popularResults = JsonPath.read(popularSort.getResponse().getContentAsString(), "$.data.results");
        assertEquals(contentPost.getId().intValue(), ((Number) popularResults.get(0).get("id")).intValue());

        MvcResult satisfactionSort = mockMvc.perform(get("/api/posts")
                        .param("sort_by", "satisfaction"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> satisfactionResults = JsonPath.read(satisfactionSort.getResponse().getContentAsString(), "$.data.results");
        assertEquals(contentPost.getId().intValue(), ((Number) satisfactionResults.get(0).get("id")).intValue());

        MvcResult categoryFilter = mockMvc.perform(get("/api/posts")
                        .param("categories", etcCategory.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> categoryResults = JsonPath.read(categoryFilter.getResponse().getContentAsString(), "$.data.results");
        assertEquals(1, categoryResults.size());
        assertEquals(etcCategoryPost.getId().intValue(), ((Number) categoryResults.get(0).get("id")).intValue());

        MvcResult modelFilter = mockMvc.perform(get("/api/posts")
                        .param("models", etcModel.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> modelResults = JsonPath.read(modelFilter.getResponse().getContentAsString(), "$.data.results");
        assertEquals(1, modelResults.size());
        assertEquals(etcModelPost.getId().intValue(), ((Number) modelResults.get(0).get("id")).intValue());

        MvcResult platformFilter = mockMvc.perform(get("/api/posts")
                        .param("platforms", anthropicPlatform.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> platformResults = JsonPath.read(platformFilter.getResponse().getContentAsString(), "$.data.results");
        assertEquals(1, platformResults.size());
        assertEquals(authorPost.getId().intValue(), ((Number) platformResults.get(0).get("id")).intValue());

        MvcResult excludeResult = mockMvc.perform(get("/api/posts")
                        .param("exclude_id", titlePost.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> excludeResults = JsonPath.read(excludeResult.getResponse().getContentAsString(), "$.data.results");
        assertTrue(excludeResults.stream().noneMatch(row ->
                ((Number) row.get("id")).longValue() == titlePost.getId()));
    }

    private Long createPost(String token, Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private Map<String, Object> payload(Long platformId, Long modelId, Long categoryId, String title) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("title", title.length() < 5 ? title + "_____" : title);
        map.put("platform", platformId);
        map.put("model", modelId);
        map.put("model_etc", "");
        map.put("model_detail", "");
        map.put("category", categoryId);
        map.put("category_etc", "");
        map.put("tags", List.of("integration", "contract"));
        map.put("satisfaction", new BigDecimal("4.5"));
        map.put("prompt", "섹션4 통합 테스트용 충분히 긴 prompt 내용입니다.");
        map.put("ai_response", "섹션4 통합 테스트용 충분히 긴 ai_response 내용입니다.");
        map.put("additional_opinion", "테스트 의견");
        return map;
    }

    private AppUser saveUser(String base) {
        String unique = base + "_" + UUID.randomUUID().toString().substring(0, 8);
        AppUser user = new AppUser();
        user.setEmail(unique + "@example.com");
        user.setUsername(unique);
        user.setPassword("encoded-password");
        user.setAvatarColor1("#111111");
        user.setAvatarColor2("#222222");
        return appUserRepository.save(user);
    }

    private String issueAccessToken(AppUser user) {
        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setTokenType(AuthTokenType.ACCESS);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        return authTokenRepository.save(token).getToken();
    }

    private Platform savePlatform(String name) {
        Platform platform = new Platform();
        platform.setName(name + "_" + UUID.randomUUID().toString().substring(0, 8));
        platform.setSlug(platform.getName().toLowerCase().replace(' ', '-'));
        platform.setActive(true);
        return platformRepository.save(platform);
    }

    private Platform savePlatformExact(String name) {
        Platform platform = new Platform();
        platform.setName(name);
        platform.setSlug(name.toLowerCase().replace(' ', '-'));
        platform.setActive(true);
        return platformRepository.save(platform);
    }

    private Category saveCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    private AiModel saveModel(Platform platform, String name) {
        AiModel model = new AiModel();
        model.setPlatform(platform);
        model.setName(name + "_" + UUID.randomUUID().toString().substring(0, 8));
        model.setSlug(model.getName().toLowerCase().replace(' ', '-'));
        model.setActive(true);
        model.setDeprecated(false);
        model.setSortOrder(1);
        return aiModelRepository.save(model);
    }

    private AiModel saveModelExact(Platform platform, String name) {
        AiModel model = new AiModel();
        model.setPlatform(platform);
        model.setName(name);
        model.setSlug(name.toLowerCase().replace(' ', '-'));
        model.setActive(true);
        model.setDeprecated(false);
        model.setSortOrder(1);
        return aiModelRepository.save(model);
    }

    private AiModel saveModelWithSlug(Platform platform, String name, String slug, int sortOrder) {
        AiModel model = new AiModel();
        model.setPlatform(platform);
        model.setName(name);
        model.setSlug(slug);
        model.setActive(true);
        model.setDeprecated(false);
        model.setSortOrder(sortOrder);
        return aiModelRepository.save(model);
    }

    private Post savePostDirect(AppUser author, Platform platform, Category category, AiModel model,
            String title, String prompt, String aiResponse, String additionalOpinion,
            String modelEtc, String categoryEtc,
            long views, long likes, long bookmarks, BigDecimal satisfaction, Instant createdAt) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setModel(model);
        post.setTitle(title);
        post.setPrompt(prompt);
        post.setAiResponse(aiResponse);
        post.setAdditionalOpinion(additionalOpinion);
        post.setModelEtc(modelEtc);
        post.setCategoryEtc(categoryEtc);
        post.setTags(java.util.List.of("tag1", "tag2"));
        post.setViewCount(views);
        post.setLikeCount(likes);
        post.setBookmarkCount(bookmarks);
        post.setSatisfaction(satisfaction);
        Post saved = postRepository.save(post);
        ReflectionTestUtils.setField(saved, "createdAt", createdAt);
        return postRepository.save(saved);
    }
}
