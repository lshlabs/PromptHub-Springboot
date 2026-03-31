package com.lshlabs.prompthubspring.stats;

import com.lshlabs.prompthubspring.post.Post;
import com.lshlabs.prompthubspring.post.PostInteractionRepository;
import com.lshlabs.prompthubspring.post.PostRepository;
import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final PostRepository postRepository;
    private final PostInteractionRepository postInteractionRepository;
    private final AppUserRepository userRepository;

    @Cacheable(value = "stats", key = "'dashboard'", sync = true)
    public DashboardResponse dashboard() {
        long totalPosts = postRepository.count();
        long totalUsers = userRepository.count();
        long totalViews = postRepository.sumViewCountAll();
        long totalLikes = postRepository.sumLikeCountAll();
        long totalBookmarks = postRepository.sumBookmarkCountAll();
        Double avgSatisfactionRaw = postRepository.avgSatisfactionAll();
        double avgSatisfaction = scale(avgSatisfactionRaw);

        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        long weeklyAddedPosts = postRepository.countByCreatedAtAfter(since);

        List<RecentPostData> recentPosts = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5))
                .stream()
                .map(this::toRecentPost)
                .toList();

        List<TagCountData> popularTags = extractPopularTags();
        List<PlatformDistributionData> platformDistribution = postRepository.countPostsByPlatform().stream()
                .map(row -> new PlatformDistributionData(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
        List<CategoryDistributionData> categoryDistribution = postRepository.countPostsByCategory().stream()
                .map(row -> new CategoryDistributionData(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();

        Instant activeUserSince = Instant.now().minus(30, ChronoUnit.DAYS);
        long activeUsers = postRepository.countDistinctAuthorsSince(activeUserSince);

        return new DashboardResponse(
                "success",
                new DashboardData(
                        totalPosts,
                        totalUsers,
                        totalViews,
                        totalLikes,
                        totalBookmarks,
                        avgSatisfaction,
                        weeklyAddedPosts,
                        activeUsers,
                        recentPosts,
                        popularTags,
                        platformDistribution,
                        categoryDistribution
                )
        );
    }

    public UserStatsResponse userStats(AppUser user) {
        long postsCount = postRepository.countByAuthor(user);
        long totalViews = postRepository.sumViewCountByAuthor(user);
        long totalLikes = postInteractionRepository.countLikedReceivedByAuthor(user);
        long totalBookmarks = postInteractionRepository.countBookmarkedReceivedByAuthor(user);
        double avgSatisfaction = scale(postRepository.avgSatisfactionByAuthor(user));

        String mostUsedPlatform = postRepository.findTopPlatformNamesByAuthor(user, PageRequest.of(0, 1)).stream()
                .findFirst().orElse(null);
        String mostUsedCategory = postRepository.findTopCategoryNamesByAuthor(user, PageRequest.of(0, 1)).stream()
                .findFirst().orElse(null);

        RecentActivityData recent = new RecentActivityData(
                formatInstant(postRepository.findLastPostDateByAuthor(user)),
                formatInstant(postInteractionRepository.findLastLikedAtByUser(user)),
                formatInstant(postInteractionRepository.findLastBookmarkedAtByUser(user))
        );

        return new UserStatsResponse(
                "success",
                new UserStatsData(
                        postsCount,
                        totalViews,
                        totalLikes,
                        totalBookmarks,
                        avgSatisfaction,
                        mostUsedPlatform,
                        mostUsedCategory,
                        recent
                )
        );
    }

    private RecentPostData toRecentPost(Post post) {
        return new RecentPostData(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getUsername(),
                post.getCreatedAt().toString(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getPlatform().getName(),
                post.getCategory().getName()
        );
    }

    private List<TagCountData> extractPopularTags() {
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Post p : postRepository.findAll()) {
            if (p.getTags() == null || p.getTags().isBlank()) {
                continue;
            }
            for (String tag : p.getTags().split(",")) {
                String key = tag.trim();
                if (!key.isBlank()) {
                    counts.put(key, counts.getOrDefault(key, 0L) + 1);
                }
            }
        }
        return counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> new TagCountData(e.getKey(), e.getValue()))
                .toList();
    }

    private static double scale(Double value) {
        if (value == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static String formatInstant(Instant value) {
        return value == null ? null : value.toString();
    }

    public record DashboardResponse(String status, DashboardData data) {
    }

    public record DashboardData(
            long total_posts,
            long total_users,
            long total_views,
            long total_likes,
            long total_bookmarks,
            double avg_satisfaction,
            long weekly_added_posts,
            long active_users,
            List<RecentPostData> recent_posts,
            List<TagCountData> popular_tags,
            List<PlatformDistributionData> platform_distribution,
            List<CategoryDistributionData> category_distribution
    ) {
    }

    public record UserStatsResponse(String status, UserStatsData data) {
    }

    public record UserStatsData(
            long posts_count,
            long total_views,
            long total_likes,
            long total_bookmarks,
            double avg_satisfaction,
            String most_used_platform,
            String most_used_category,
            RecentActivityData recent_activity
    ) {
    }

    public record RecentActivityData(
            String last_post_date,
            String last_like_date,
            String last_bookmark_date
    ) {
    }

    public record RecentPostData(
            Long id,
            String title,
            String author,
            String created_at,
            long views,
            long likes,
            String platform,
            String category
    ) {
    }

    public record TagCountData(String name, long count) {
    }

    public record PlatformDistributionData(String platform, long count) {
    }

    public record CategoryDistributionData(String category, long count) {
    }
}
