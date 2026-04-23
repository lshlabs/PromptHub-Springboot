package com.lshlabs.prompthubspring.stats;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_stats_perf;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Tag("performance")
class StatsServicePerformanceTest {

    @Autowired
    private StatsService statsService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostInteractionRepository postInteractionRepository;
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
        postInteractionRepository.deleteAll();
        postRepository.deleteAll();
        aiModelRepository.deleteAll();
        platformRepository.deleteAll();
        categoryRepository.deleteAll();
        authTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        appUserRepository.deleteAll();
        Cache cache = cacheManager.getCache("stats");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void dashboardCache_reportsHitRatioAndWarmP95() {
        Platform platform = savePlatform("stats_perf_platform");
        Category category = saveCategory("stats_perf_category");
        AppUser author = saveUser("stats_perf_author");
        AppUser actor = saveUser("stats_perf_actor");

        for (int i = 0; i < 80; i++) {
            Post post = savePost(author, platform, category, "통계성능-" + i, i);
            if (i % 3 == 0) {
                saveInteraction(actor, post);
            }
        }

        Cache cache = cacheManager.getCache("stats");
        if (cache != null) {
            cache.clear();
        }

        long coldStart = System.nanoTime();
        StatsService.DashboardResponse cold = statsService.dashboard();
        long coldElapsed = System.nanoTime() - coldStart;
        assertTrue(cold.data().total_posts() >= 1);

        int warmCalls = 30;
        int cacheHitCount = 0;
        List<Long> warmElapsed = new ArrayList<>();
        for (int i = 0; i < warmCalls; i++) {
            if (cache != null && cache.get("dashboard") != null) {
                cacheHitCount++;
            }
            long startNanos = System.nanoTime();
            statsService.dashboard();
            long endNanos = System.nanoTime();
            warmElapsed.add(endNanos - startNanos);
        }

        double hitRatio = cacheHitCount / (double) warmCalls;
        double coldMillis = coldElapsed / 1_000_000.0;
        double warmP95Millis = percentileMillis(warmElapsed, 0.95);

        System.out.printf("STATS_DASHBOARD_CACHE_METRIC hit_ratio=%.4f cold_ms=%.2f warm_p95_ms=%.2f warm_calls=%d%n",
                hitRatio, coldMillis, warmP95Millis, warmCalls);

        assertTrue(hitRatio >= 0.95, "Cache hit ratio should be high after warm-up");
        assertTrue(warmP95Millis <= coldMillis * 1.5, "Warm p95 should be significantly lower than cold");
    }

    @Test
    void userStats_reportsTpsAndP95Metrics() {
        Platform platform = savePlatform("user_stats_platform");
        Category category = saveCategory("user_stats_category");
        AppUser author = saveUser("user_stats_author");
        AppUser actor = saveUser("user_stats_actor");

        for (int i = 0; i < 100; i++) {
            Post post = savePost(author, platform, category, "유저통계성능-" + i, i);
            if (i % 2 == 0) {
                saveInteraction(actor, post);
            }
        }

        int iterations = 40;
        List<Long> elapsedNanos = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();
            StatsService.UserStatsResponse response = statsService.userStats(author);
            long endNanos = System.nanoTime();
            elapsedNanos.add(endNanos - startNanos);
            assertTrue(response.data().posts_count() >= 1);
        }
        long end = System.nanoTime();

        double totalSeconds = (end - start) / 1_000_000_000.0;
        double tps = iterations / totalSeconds;
        double p95Millis = percentileMillis(elapsedNanos, 0.95);

        System.out.printf("STATS_USER_METRIC tps=%.2f p95_ms=%.2f iterations=%d%n", tps, p95Millis, iterations);

        assertTrue(tps > 0, "TPS should be positive");
        assertTrue(p95Millis > 0, "P95 should be positive");
        assertTrue(p95Millis < 5000, "P95 should be within reasonable bound for test env");
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

    private Post savePost(AppUser author, Platform platform, Category category, String title, int offset) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setTitle(title);
        post.setPrompt("stats performance prompt 본문입니다.");
        post.setAiResponse("stats performance ai_response 본문입니다.");
        post.setTags("stats,perf");
        post.setSatisfaction(new BigDecimal("4.2"));
        post.setViewCount(100 + offset);
        post.setLikeCount(10 + (offset % 5));
        post.setBookmarkCount(5 + (offset % 3));
        return postRepository.save(post);
    }

    private void saveInteraction(AppUser user, Post post) {
        PostInteraction interaction = new PostInteraction();
        interaction.setUser(user);
        interaction.setPost(post);
        interaction.setLiked(true);
        interaction.setBookmarked(true);
        postInteractionRepository.save(interaction);
    }
}
