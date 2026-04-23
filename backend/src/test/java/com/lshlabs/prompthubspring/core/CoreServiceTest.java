package com.lshlabs.prompthubspring.core;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.post.Category;
import com.lshlabs.prompthubspring.post.CategoryRepository;
import com.lshlabs.prompthubspring.post.Platform;
import com.lshlabs.prompthubspring.post.PlatformRepository;
import com.lshlabs.prompthubspring.post.Post;
import com.lshlabs.prompthubspring.post.PostService;
import com.lshlabs.prompthubspring.post.PostRepository;
import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import com.lshlabs.prompthubspring.user.UserSessionRepository;
import com.lshlabs.prompthubspring.user.UserSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class CoreServiceTest {

    @Autowired
    private CoreService coreService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private AuthTokenRepository authTokenRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        authTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        appUserRepository.deleteAll();
        Cache trendingCache = cacheManager.getCache("trending");
        if (trendingCache != null) {
            trendingCache.clear();
        }
    }

    @Test
    void search_supportsFiltersAndSatisfactionRange() {
        Platform platform = ensurePlatform();
        Category category = ensureCategory();
        AppUser author = saveUser("core_search_author");

        savePost(author, platform, category, "검색테스트 제목 A", "태그A", new BigDecimal("4.8"));
        savePost(author, platform, category, "검색테스트 제목 B", "태그B", new BigDecimal("3.2"));

        PostService.PostListResponse response = coreService.search(
                "검색테스트", "latest", 1, 20, platform.getId(), category.getId(),
                new BigDecimal("4.0"), new BigDecimal("5.0")
        );

        long totalCount = response.data().pagination().total_count();
        assertTrue(totalCount >= 1);
    }

    @Test
    void categoryRankings_populatesTrendingCache() {
        coreService.categoryRankings();

        Cache cache = cacheManager.getCache("trending");
        assertNotNull(cache);
        assertNotNull(cache.get("category-rankings"));
    }

    @Test
    void filterOptions_containsPlatformAndCategoryOptions() {
        CoreService.FilterOptionsResponse filters = coreService.filterOptions();

        assertNotNull(filters.platforms());
        assertNotNull(filters.categories());
        assertNotNull(filters.models_by_platform());
    }

    @Test
    void sortOptions_matchesCompatibilityContractShape() {
        CoreService.SortOptionsResponse response = coreService.sortOptions();
        assertNotNull(response.sort_options());
        assertTrue(response.sort_options().containsKey("latest"));
        assertTrue(response.sort_options().containsKey("popular"));
        assertTrue(response.sort_options().containsKey("views"));
        assertTrue("latest".equals(response.default_value()));
    }

    private AppUser saveUser(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser user = new AppUser();
        user.setUsername(prefix + "_" + suffix);
        user.setEmail(prefix + "_" + suffix + "@example.com");
        user.setPassword("encoded");
        return appUserRepository.save(user);
    }

    private Post savePost(AppUser author, Platform platform, Category category, String title, String tags,
            BigDecimal satisfaction) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setTitle(title);
        post.setPrompt("core prompt 본문입니다.");
        post.setAiResponse("core ai response 본문입니다.");
        post.setTags(tags);
        post.setSatisfaction(satisfaction);
        return postRepository.save(post);
    }

    private Platform ensurePlatform() {
        return platformRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .findFirst()
                .orElseGet(() -> {
                    Platform platform = new Platform();
                    platform.setName("core_platform_" + UUID.randomUUID().toString().substring(0, 8));
                    platform.setActive(true);
                    return platformRepository.save(platform);
                });
    }

    private Category ensureCategory() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .findFirst()
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName("core_category_" + UUID.randomUUID().toString().substring(0, 8));
                    return categoryRepository.save(category);
                });
    }
}
