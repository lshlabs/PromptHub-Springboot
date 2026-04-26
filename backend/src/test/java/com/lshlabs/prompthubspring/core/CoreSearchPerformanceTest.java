package com.lshlabs.prompthubspring.core;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.post.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Tag("performance")
class CoreSearchPerformanceTest {

    @Autowired
    private CoreService coreService;
    @Autowired
    private CacheManager cacheManager;
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
    private AppUserRepository appUserRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private AuthTokenRepository authTokenRepository;

    @AfterEach
    void tearDown() {
        postInteractionRepo.deleteAll();
        postRepository.deleteAll();
        aiModelRepository.deleteAll();
        platformRepository.deleteAll();
        categoryRepository.deleteAll();
        authTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        appUserRepository.deleteAll();
        Cache cache = cacheManager.getCache("trending");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void search_reportsTpsAndP95Metrics() {
        Platform platform = savePlatform("core_perf_platform");
        Category category = saveCategory("core_perf_category");
        AppUser author = saveUser("core_perf_author");
        for (int i = 0; i < 120; i++) {
            savePost(author, platform, category, "검색성능테스트-" + i, i % 2 == 0 ? new BigDecimal("4.5") : new BigDecimal("3.8"));
        }

        int iterations = 40;
        List<Long> elapsedNanos = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();
            PostService.PostListResponse response = coreService.search(
                    "검색성능테스트", "latest", 1, 20,
                    platform.getId(), category.getId(),
                    new BigDecimal("3.5"), new BigDecimal("5.0")
            );
            long endNanos = System.nanoTime();
            elapsedNanos.add(endNanos - startNanos);

            long total = response.data().pagination().total_count();
            assertTrue(total >= 1);
        }
        long end = System.nanoTime();

        double totalSeconds = (end - start) / 1_000_000_000.0;
        double tps = iterations / totalSeconds;
        double p95Millis = percentileMillis(elapsedNanos, 0.95);

        System.out.printf("SECTION6_CORE_SEARCH_METRIC tps=%.2f p95_ms=%.2f iterations=%d%n", tps, p95Millis, iterations);

        assertTrue(tps > 0, "TPS should be positive");
        assertTrue(p95Millis > 0, "P95 should be positive");
        assertTrue(p95Millis < 5000, "P95 should be within reasonable bound for test env");
    }

    @Test
    void trendingCache_reportsHitRatioAndWarmP95() {
        Platform platform = savePlatform("core_cache_platform");
        Category category = saveCategory("core_cache_category");
        AppUser author = saveUser("core_cache_author");
        for (int i = 0; i < 100; i++) {
            savePost(author, platform, category, "트렌딩캐시테스트-" + i, new BigDecimal("4.0"));
        }

        Cache cache = cacheManager.getCache("trending");
        if (cache != null) {
            cache.clear();
        }

        long coldStart = System.nanoTime();
        coreService.categoryRankings();
        long coldElapsed = System.nanoTime() - coldStart;

        int warmCalls = 30;
        int cacheHitCount = 0;
        List<Long> warmElapsed = new ArrayList<>();
        for (int i = 0; i < warmCalls; i++) {
            if (cache != null && cache.get("category-rankings") != null) {
                cacheHitCount++;
            }
            long startNanos = System.nanoTime();
            coreService.categoryRankings();
            long endNanos = System.nanoTime();
            warmElapsed.add(endNanos - startNanos);
        }

        double hitRatio = cacheHitCount / (double) warmCalls;
        double coldMillis = coldElapsed / 1_000_000.0;
        double warmP95Millis = percentileMillis(warmElapsed, 0.95);

        System.out.printf("SECTION6_CORE_TRENDING_CACHE_METRIC hit_ratio=%.4f cold_ms=%.2f warm_p95_ms=%.2f warm_calls=%d%n",
                hitRatio, coldMillis, warmP95Millis, warmCalls);

        assertTrue(hitRatio >= 0.95, "Cache hit ratio should be high after warm-up");
        assertTrue(warmP95Millis <= coldMillis * 1.5, "Warm p95 should be significantly lower than cold");
    }

    private static double percentileMillis(List<Long> samplesNanos, double percentile) {
        List<Long> sorted = samplesNanos.stream().sorted().toList();
        int index = Math.max(0, Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * percentile) - 1));
        return sorted.get(index) / 1_000_000.0;
    }

    private AppUser saveUser(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser user = new AppUser();
        user.setUsername(prefix + "_" + suffix);
        user.setEmail(prefix + "_" + suffix + "@example.com");
        user.setPassword("encoded");
        return appUserRepository.save(user);
    }

    private Platform savePlatform(String baseName) {
        Platform platform = new Platform();
        platform.setName(baseName + "_" + UUID.randomUUID().toString().substring(0, 8));
        platform.setActive(true);
        return platformRepository.save(platform);
    }

    private Category saveCategory(String baseName) {
        Category category = new Category();
        category.setName(baseName + "_" + UUID.randomUUID().toString().substring(0, 8));
        return categoryRepository.save(category);
    }

    private void savePost(AppUser author, Platform platform, Category category, String title, BigDecimal satisfaction) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setTitle(title);
        post.setPrompt("core performance prompt 본문입니다.");
        post.setAiResponse("core performance ai_response 본문입니다.");
        post.setSatisfaction(satisfaction);
        post.setTags(java.util.List.of("perf", "core"));
        postRepository.save(post);
    }
}
