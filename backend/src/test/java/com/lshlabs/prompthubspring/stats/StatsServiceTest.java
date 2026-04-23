package com.lshlabs.prompthubspring.stats;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.post.Category;
import com.lshlabs.prompthubspring.post.CategoryRepository;
import com.lshlabs.prompthubspring.post.Platform;
import com.lshlabs.prompthubspring.post.PlatformRepository;
import com.lshlabs.prompthubspring.post.Post;
import com.lshlabs.prompthubspring.post.PostInteraction;
import com.lshlabs.prompthubspring.post.PostInteractionRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_stats;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Tag("integration")
class StatsServiceTest {

    @Autowired
    private StatsService statsService;
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
    private PostInteractionRepository postInteractionRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @AfterEach
    void tearDown() {
        postInteractionRepository.deleteAll();
        postRepository.deleteAll();
        authTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        appUserRepository.deleteAll();
        Cache statsCache = cacheManager.getCache("stats");
        if (statsCache != null) {
            statsCache.clear();
        }
    }

    @Test
    void dashboard_returnsAggregatedValuesAndCachesResult() {
        Platform platform = ensurePlatform();
        Category category = ensureCategory();
        AppUser author = saveUser("stats_stats_author");

        Post firstPost = savePost(author, platform, category, "대시보드 포스트1", 101, 10, 5, new BigDecimal("4.5"));
        Post secondPost = savePost(author, platform, category, "대시보드 포스트2", 99, 7, 3, new BigDecimal("3.5"));

        StatsService.DashboardResponse response = statsService.dashboard();
        StatsService.DashboardData data = response.data();

        assertEquals(2L, data.total_posts());
        assertEquals(200L, data.total_views());
        assertEquals(17L, data.total_likes());
        assertEquals(8L, data.total_bookmarks());
        StatsService.RecentPostData recentPost = data.recent_posts().get(0);
        assertNotNull(recentPost.created_at());
        assertTrue(!recentPost.created_at().isBlank());

        Cache cache = cacheManager.getCache("stats");
        assertNotNull(cache);
        assertNotNull(cache.get("dashboard"));
    }

    @Test
    void userStats_returnsAuthorBasedAggregates() {
        Platform platform = ensurePlatform();
        Category category = ensureCategory();
        AppUser author = saveUser("stats_user_stats_author");
        AppUser viewer = saveUser("stats_user_stats_viewer");

        Post firstPost = savePost(author, platform, category, "사용자 통계 포스트1", 30, 0, 0, new BigDecimal("4.2"));
        Post secondPost = savePost(author, platform, category, "사용자 통계 포스트2", 20, 0, 0, new BigDecimal("3.8"));

        saveInteraction(viewer, firstPost, true, true);
        saveInteraction(viewer, secondPost, true, false);

        StatsService.UserStatsResponse response = statsService.userStats(author);
        StatsService.UserStatsData data = response.data();

        assertEquals(2L, data.posts_count());
        assertEquals(50L, data.total_views());
        assertEquals(2L, data.total_likes());
        assertEquals(1L, data.total_bookmarks());
        assertNotNull(data.recent_activity());
    }

    @Test
    void dashboard_activeUsers_countsOnlyRecent30DaysAuthors() {
        Platform platform = ensurePlatform();
        Category category = ensureCategory();
        AppUser recentAuthor = saveUser("stats_recent_author");
        AppUser oldAuthor = saveUser("stats_old_author");

        Post recentPost = savePost(recentAuthor, platform, category, "최근 작성글", 10, 1, 1, new BigDecimal("4.0"));
        Post oldPost = savePost(oldAuthor, platform, category, "오래된 작성글", 10, 1, 1, new BigDecimal("4.0"));
        ReflectionTestUtils.setField(oldPost, "createdAt", Instant.now().minus(40, ChronoUnit.DAYS));
        postRepository.save(oldPost);
        postRepository.save(recentPost);

        StatsService.DashboardResponse response = statsService.dashboard();
        StatsService.DashboardData data = response.data();

        assertEquals(1L, data.active_users());
    }

    @Test
    void userStats_recentActivity_usesInteractionUpdatedAt() {
        Platform platform = ensurePlatform();
        Category category = ensureCategory();
        AppUser author = saveUser("stats_recent_activity_author");
        AppUser actor = saveUser("stats_recent_activity_actor");

        Post post = savePost(author, platform, category, "활동 기준 포스트", 12, 0, 0, new BigDecimal("4.0"));
        PostInteraction interaction = new PostInteraction();
        interaction.setUser(actor);
        interaction.setPost(post);
        interaction.setLiked(true);
        interaction.setBookmarked(true);
        interaction = postInteractionRepository.save(interaction);

        Instant futureCreatedAt = Instant.now().plus(2, ChronoUnit.DAYS);
        ReflectionTestUtils.setField(interaction, "createdAt", futureCreatedAt);
        postInteractionRepository.save(interaction);

        StatsService.UserStatsResponse response = statsService.userStats(actor);
        StatsService.RecentActivityData recent = response.data().recent_activity();

        String lastLikeDate = recent.last_like_date();
        String lastBookmarkDate = recent.last_bookmark_date();
        assertNotNull(lastLikeDate);
        assertNotNull(lastBookmarkDate);

        Instant lastLikeInstant = Instant.parse(lastLikeDate);
        Instant lastBookmarkInstant = Instant.parse(lastBookmarkDate);
        assertNotNull(lastLikeInstant);
        assertNotNull(lastBookmarkInstant);
        assertTrue(lastLikeInstant.isBefore(futureCreatedAt));
        assertTrue(lastBookmarkInstant.isBefore(futureCreatedAt));
    }

    private AppUser saveUser(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser user = new AppUser();
        user.setUsername(prefix + "_" + suffix);
        user.setEmail(prefix + "_" + suffix + "@example.com");
        user.setPassword("encoded");
        return appUserRepository.save(user);
    }

    private Post savePost(AppUser author, Platform platform, Category category, String title,
            long views, long likes, long bookmarks, BigDecimal satisfaction) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setTitle(title);
        post.setPrompt("stats prompt 본문입니다.");
        post.setAiResponse("stats ai response 본문입니다.");
        post.setTags("tag1,tag2");
        post.setViewCount(views);
        post.setLikeCount(likes);
        post.setBookmarkCount(bookmarks);
        post.setSatisfaction(satisfaction);
        return postRepository.save(post);
    }

    private void saveInteraction(AppUser user, Post post, boolean liked, boolean bookmarked) {
        PostInteraction pi = new PostInteraction();
        pi.setUser(user);
        pi.setPost(post);
        pi.setLiked(liked);
        pi.setBookmarked(bookmarked);
        postInteractionRepository.save(pi);
    }

    private Platform ensurePlatform() {
        return platformRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .findFirst()
                .orElseGet(() -> {
                    Platform platform = new Platform();
                    platform.setName("stats_platform_" + UUID.randomUUID().toString().substring(0, 8));
                    platform.setActive(true);
                    return platformRepository.save(platform);
                });
    }

    private Category ensureCategory() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .findFirst()
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName("stats_category_" + UUID.randomUUID().toString().substring(0, 8));
                    return categoryRepository.save(category);
                });
    }
}
