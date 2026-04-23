package com.lshlabs.prompthubspring.post;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PostServiceValidationTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PlatformRepository platformRepository;
    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PostInteractionRepository interactionRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(
                postRepository,
                platformRepository,
                aiModelRepository,
                categoryRepository,
                interactionRepository
        );
    }

    @Test
    void create_throwsBadRequest_whenModelPlatformMismatch() {
        AppUser author = user(1L, "author");
        Platform platform = platform(1L, "OpenAI", true);
        Platform otherPlatform = platform(2L, "Anthropic", true);
        Category category = category(1L, "일반");
        AiModel model = model(10L, "gpt-4.1", otherPlatform, true, false);

        when(platformRepository.findById(1L)).thenReturn(Optional.of(platform));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(aiModelRepository.findById(10L)).thenReturn(Optional.of(model));

        ApiException ex = assertThrows(ApiException.class, () ->
                postService.create(author, request(1L, 10L, 1L, null, null))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("선택한 모델이 플랫폼과 일치하지 않습니다.", ex.getMessage());
    }

    @Test
    void create_throwsBadRequest_whenCategoryIsEtcButCategoryEtcMissing() {
        AppUser author = user(1L, "author");
        Platform platform = platform(1L, "OpenAI", true);
        Category etcCategory = category(9L, "기타");

        when(platformRepository.findById(1L)).thenReturn(Optional.of(platform));
        when(categoryRepository.findById(9L)).thenReturn(Optional.of(etcCategory));

        ApiException ex = assertThrows(ApiException.class, () ->
                postService.create(author, request(1L, null, 9L, "직접입력", null))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("기타 카테고리를 선택한 경우 category_etc를 입력해야 합니다.", ex.getMessage());
    }

    @Test
    void create_throwsBadRequest_whenModelAndModelEtcAreMissing() {
        AppUser author = user(1L, "author");
        Platform platform = platform(1L, "OpenAI", true);
        Category category = category(1L, "일반");

        when(platformRepository.findById(1L)).thenReturn(Optional.of(platform));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        ApiException ex = assertThrows(ApiException.class, () ->
                postService.create(author, request(1L, null, 1L, null, null))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("모델을 선택하거나 기타 모델명을 입력해야 합니다.", ex.getMessage());
    }

    @Test
    void create_throwsBadRequest_whenPlatformIsEtcButModelIsNotEtc() {
        AppUser author = user(1L, "author");
        Platform etcPlatform = platform(1L, "기타", true);
        Category category = category(1L, "일반");
        AiModel normalModel = model(10L, "gpt-4.1", etcPlatform, true, false);

        when(platformRepository.findById(1L)).thenReturn(Optional.of(etcPlatform));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(aiModelRepository.findById(10L)).thenReturn(Optional.of(normalModel));

        ApiException ex = assertThrows(ApiException.class, () ->
                postService.create(author, request(1L, 10L, 1L, null, null))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("플랫폼이 기타인 경우 기타 모델만 선택할 수 있습니다.", ex.getMessage());
    }

    @Test
    void create_throwsBadRequest_whenPlatformIsEtcAndModelEtcMissing() {
        AppUser author = user(1L, "author");
        Platform etcPlatform = platform(1L, "기타", true);
        Category category = category(1L, "일반");

        when(platformRepository.findById(1L)).thenReturn(Optional.of(etcPlatform));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        ApiException ex = assertThrows(ApiException.class, () ->
                postService.create(author, request(1L, null, 1L, null, null))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("플랫폼이 기타인 경우 기타 모델명을 입력해야 합니다.", ex.getMessage());
    }

    @Test
    void create_throwsBadRequest_whenPlatformIsEtcAndModelDetailProvided() {
        AppUser author = user(1L, "author");
        Platform etcPlatform = platform(1L, "기타", true);
        Category category = category(1L, "일반");

        when(platformRepository.findById(1L)).thenReturn(Optional.of(etcPlatform));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        ApiException ex = assertThrows(ApiException.class, () ->
                postService.create(author, request(1L, null, 1L, "직접입력", null, "custom-detail"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("플랫폼이 기타인 경우 상세 모델명을 사용할 수 없습니다.", ex.getMessage());
    }

    @Test
    void create_throwsBadRequest_whenModelIsEtcAndModelDetailProvided() {
        AppUser author = user(1L, "author");
        Platform platform = platform(1L, "OpenAI", true);
        Category category = category(1L, "일반");
        AiModel etcModel = model(10L, "기타", platform, true, false);

        when(platformRepository.findById(1L)).thenReturn(Optional.of(platform));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(aiModelRepository.findById(10L)).thenReturn(Optional.of(etcModel));

        ApiException ex = assertThrows(ApiException.class, () ->
                postService.create(author, request(1L, 10L, 1L, "직접입력", null, "custom-detail"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("'기타' 모델에서는 상세 모델명을 사용할 수 없습니다.", ex.getMessage());
    }

    @Test
    void create_succeeds_whenPlatformAndModelAreEtcWithModelEtc() {
        AppUser author = user(1L, "author");
        Platform etcPlatform = platform(1L, "기타", true);
        Category category = category(1L, "일반");
        AiModel etcModel = model(10L, "기타", etcPlatform, true, false);

        when(platformRepository.findById(1L)).thenReturn(Optional.of(etcPlatform));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(aiModelRepository.findById(10L)).thenReturn(Optional.of(etcModel));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1000L);
            return saved;
        });

        PostService.PostMutationResponse response = postService.create(author, request(1L, 10L, 1L, "custom-model", null));

        assertEquals("success", response.status());
    }

    @Test
    void update_throwsForbidden_whenNotAuthor() {
        AppUser editor = user(2L, "editor");
        AppUser author = user(1L, "author");

        Post post = new Post();
        ReflectionTestUtils.setField(post, "id", 100L);
        post.setAuthor(author);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        ApiException ex = assertThrows(ApiException.class, () ->
                postService.update(editor, 100L, request(1L, null, 1L, null, null))
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("게시글을 수정할 권한이 없습니다.", ex.getMessage());
    }

    @Test
    void create_succeeds_withValidPayload() {
        AppUser author = user(1L, "author");
        Platform platform = platform(1L, "OpenAI", true);
        Category category = category(1L, "일반");
        AiModel model = model(10L, "gpt-4.1", platform, true, false);

        when(platformRepository.findById(1L)).thenReturn(Optional.of(platform));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(aiModelRepository.findById(10L)).thenReturn(Optional.of(model));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 999L);
            return saved;
        });

        PostService.PostMutationResponse response = postService.create(author, request(1L, 10L, 1L, null, null));

        assertEquals("success", response.status());
        assertEquals("게시글이 성공적으로 생성되었습니다.", response.message());
    }

    @Test
    void create_throwsBadRequest_whenSatisfactionIsNotHalfStep() {
        AppUser author = user(1L, "author");
        Platform platform = platform(1L, "OpenAI", true);
        Category category = category(1L, "일반");
        AiModel model = model(10L, "gpt-4.1", platform, true, false);

        when(platformRepository.findById(1L)).thenReturn(Optional.of(platform));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(aiModelRepository.findById(10L)).thenReturn(Optional.of(model));

        ApiException ex = assertThrows(ApiException.class, () ->
                postService.create(author, request(1L, 10L, 1L, null, null, null, new BigDecimal("4.3")))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("만족도는 0.5점 단위로 입력해야 합니다.", ex.getMessage());
    }

    private PostUpsertRequest request(Long platformId, Long modelId, Long categoryId, String modelEtc, String categoryEtc) {
        return request(platformId, modelId, categoryId, modelEtc, categoryEtc, null);
    }

    private PostUpsertRequest request(Long platformId, Long modelId, Long categoryId, String modelEtc,
            String categoryEtc, String modelDetail) {
        return request(platformId, modelId, categoryId, modelEtc, categoryEtc, modelDetail, new BigDecimal("4.5"));
    }

    private PostUpsertRequest request(Long platformId, Long modelId, Long categoryId, String modelEtc,
            String categoryEtc, String modelDetail, BigDecimal satisfaction) {
        return new PostUpsertRequest(
                "유효한 제목입니다",
                platformId,
                modelId,
                modelEtc,
                modelDetail,
                categoryId,
                categoryEtc,
                List.of("tag1", "tag2"),
                satisfaction,
                "충분히 긴 prompt 내용입니다.",
                "충분히 긴 ai_response 내용입니다.",
                "opinion"
        );
    }

    private Platform platform(Long id, String name, boolean active) {
        Platform platform = new Platform();
        ReflectionTestUtils.setField(platform, "id", id);
        platform.setName(name);
        platform.setActive(active);
        return platform;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", id);
        category.setName(name);
        return category;
    }

    private AiModel model(Long id, String name, Platform platform, boolean active, boolean deprecated) {
        AiModel model = new AiModel();
        ReflectionTestUtils.setField(model, "id", id);
        model.setName(name);
        model.setPlatform(platform);
        model.setActive(active);
        model.setDeprecated(deprecated);
        return model;
    }

    private AppUser user(Long id, String username) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("encoded");
        return user;
    }
}
