package com.lshlabs.prompthubspring.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lshlabs.prompthubspring.post.AiModelRepository;
import com.lshlabs.prompthubspring.post.CategoryRepository;
import com.lshlabs.prompthubspring.post.PlatformRepository;
import com.lshlabs.prompthubspring.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CoreService {
    private static final String TRENDING_CACHE = "trending";
    private static final String CATEGORY_RANKINGS_CACHE_KEY = "category-rankings";

    private final PostService postService;
    private final PlatformRepository platformRepository;
    private final CategoryRepository categoryRepository;
    private final AiModelRepository aiModelRepository;
    private final TrendingCategoryRepository trendingCategoryRepo;
    private final TrendingRankingRepository trendingRankingRepo;
    private final CacheManager cacheManager;

    public PostService.PostListResponse search(String q, String sort, int page, int pageSize,
            Long platform, Long category, BigDecimal satisfactionMin, BigDecimal satisfactionMax) {
        return postService.searchPosts(null, page, pageSize, q, sort, platform, category, satisfactionMin, satisfactionMax);
    }

    public PostService.PostListResponse search(String q, String searchType, String sort, int page, int pageSize,
            String categories, String platforms, String models,
            Long platform, Long category, BigDecimal satisfactionMin, BigDecimal satisfactionMax) {
        String mergedPlatforms = mergeCsv(platforms, platform);
        String mergedCategories = mergeCsv(categories, category);

        return postService.listPosts(
                null,
                page,
                pageSize,
                q,
                searchType,
                sort,
                null,
                mergedCategories,
                mergedPlatforms,
                models,
                satisfactionMin,
                satisfactionMax
        );
    }

    public SearchResult searchWithLegacySchema(String q, String searchType, String sort, int page, int pageSize,
            String categories, String platforms, String models,
            Long platform, Long category, BigDecimal satisfactionMin, BigDecimal satisfactionMax) {
        // 레거시 응답 스키마(results/pagination) 호환을 위해 별도 래퍼를 유지한다.
        PostService.PostListResponse response = search(
                q, searchType, sort, page, pageSize, categories, platforms, models,
                platform, category, satisfactionMin, satisfactionMax
        );
        List<PostService.PostCardData> cards = postService.toPostCardDataList(response.data().results());
        return new SearchResult(cards, response.data().pagination());
    }

    private static String mergeCsv(String csv, Long singleId) {
        if (singleId == null) {
            return csv;
        }
        if (csv == null || csv.isBlank()) {
            return String.valueOf(singleId);
        }
        String trimmed = csv.trim();
        if (trimmed.isEmpty()) {
            return String.valueOf(singleId);
        }
        return trimmed + "," + singleId;
    }

    public SortOptionsResponse sortOptions() {
        Map<String, String> sortOptions = new LinkedHashMap<>();
        sortOptions.put("latest", "최신순");
        sortOptions.put("oldest", "오래된순");
        sortOptions.put("popular", "인기순");
        sortOptions.put("satisfaction", "만족도순");
        sortOptions.put("views", "조회순");
        return new SortOptionsResponse(sortOptions, "latest");
    }

    public FilterOptionsResponse filterOptions() {
        List<PostService.IdNameData> platforms = platformRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(p -> new PostService.IdNameData(p.getId(), p.getName()))
                .toList();
        List<PostService.IdNameData> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(c -> new PostService.IdNameData(c.getId(), c.getName()))
                .toList();

        Map<String, List<ModelFilterData>> modelsByPlatform = new LinkedHashMap<>();
        aiModelRepository.findAll(Sort.by(Sort.Direction.ASC, "platform.id").and(Sort.by(Sort.Direction.ASC, "id")))
                .forEach(model -> {
                    String platformName = model.getPlatform().getName();
                    modelsByPlatform.computeIfAbsent(platformName, key -> new java.util.ArrayList<>())
                            .add(new ModelFilterData(
                                    model.getId(),
                                    model.getName(),
                                    model.getPlatform().getId(),
                                    platformName
                            ));
                });

        return new FilterOptionsResponse(platforms, categories, modelsByPlatform);
    }

    public CategoryRankingsResponse categoryRankings() {
        Cache cache = cacheManager.getCache(TRENDING_CACHE);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(CATEGORY_RANKINGS_CACHE_KEY);
            if (wrapper != null && wrapper.get() instanceof Map<?, ?> cached) {
                @SuppressWarnings("unchecked")
                Map<String, TrendingCategoryData> cachedData = (Map<String, TrendingCategoryData>) cached;
                return new CategoryRankingsResponse("success", cachedData, true);
            }
        }

        // 캐시 미스 시에만 DB를 조회해 트렌딩 페이로드를 구성한다.
        Map<String, TrendingCategoryData> payload = fetchCategoryRankings();
        if (cache != null) {
            cache.put(CATEGORY_RANKINGS_CACHE_KEY, payload);
        }
        return new CategoryRankingsResponse("success", payload, false);
    }

    public RefreshCacheResponse refreshTrendingCache() {
        Cache cache = cacheManager.getCache(TRENDING_CACHE);
        if (cache != null) {
            cache.evict(CATEGORY_RANKINGS_CACHE_KEY);
        }
        return new RefreshCacheResponse("success", "트렌딩 캐시가 성공적으로 삭제되었습니다.");
    }

    private Map<String, TrendingCategoryData> fetchCategoryRankings() {
        Map<String, TrendingCategoryData> payload = new LinkedHashMap<>();
        for (TrendingCategoryEntity category : trendingCategoryRepo.findVisibleCategories()) {
            List<RankingData> rankings = trendingRankingRepo.findRankingsByCategory(category)
                    .stream()
                    .map(r -> new RankingData(r.getRank(), r.getName(), r.getScore(), r.getProvider()))
                    .toList();

            payload.put(category.getName(), new TrendingCategoryData(
                    category.getTitle(),
                    category.getSubtitle(),
                    category.getIconName(),
                    rankings
            ));
        }
        return payload;
    }

    public TrendingModelPostsResponse trendingModelPosts(String modelName, int page, int pageSize, String sort) {
        Optional<TrendingRankingEntity> rankingOptional = findRepresentativeRanking(modelName);
        if (rankingOptional.isEmpty()) {
            return null;
        }

        TrendingRankingEntity ranking = rankingOptional.get();
        PostService.PostListData postsData = fetchTrendingPostsData(ranking, page, pageSize, sort);
        return new TrendingModelPostsResponse(
                postsData.results(),
                postsData.pagination(),
                modelInfoData(ranking)
        );
    }

    public TrendingModelInfoResponse trendingModelInfo(String modelName) {
        Optional<TrendingRankingEntity> rankingOptional = findRepresentativeRanking(modelName);
        if (rankingOptional.isEmpty()) {
            return null;
        }
        return new TrendingModelInfoResponse("success", modelInfoData(rankingOptional.get()));
    }

    private Optional<TrendingRankingEntity> findRepresentativeRanking(String modelName) {
        List<TrendingRankingEntity> primaryRankings = trendingRankingRepo.findPrimaryRankingsByName(modelName);
        if (!primaryRankings.isEmpty()) {
            return Optional.of(primaryRankings.getFirst());
        }
        List<TrendingRankingEntity> rankings = trendingRankingRepo.findActiveByName(modelName);
        if (!rankings.isEmpty()) {
            return Optional.of(rankings.getFirst());
        }
        return Optional.empty();
    }

    private PostService.PostListData fetchTrendingPostsData(TrendingRankingEntity ranking, int page, int pageSize,
            String sort) {
        if (ranking.getRelatedModel() == null) {
            // 트렌딩 메타는 존재하지만 연결 모델이 없으면 빈 목록을 반환한다(404 아님).
            return new PostService.PostListData(
                    List.of(),
                    new PostService.PaginationData(Math.max(page, 1), 0, 0, false, false)
            );
        }

        PostService.PostListResponse response = postService.listPostsByTrendingModel(
                null,
                ranking.getRelatedModel(),
                ranking.isUseExactMatching(),
                ranking.getModelDetailContains(),
                ranking.getModelEtcContains(),
                page,
                pageSize,
                sort
        );
        return response.data();
    }

    private TrendingModelData modelInfoData(TrendingRankingEntity ranking) {
        long relatedPostsCount = 0;
        if (ranking.getRelatedModel() != null) {
            relatedPostsCount = postService.countPostsByTrendingModel(
                    ranking.getRelatedModel(),
                    ranking.isUseExactMatching(),
                    ranking.getModelDetailContains(),
                    ranking.getModelEtcContains()
            );
        }

        RelatedModelData relatedModel = null;
        if (ranking.getRelatedModel() != null) {
            relatedModel = new RelatedModelData(
                    ranking.getRelatedModel().getId(),
                    ranking.getRelatedModel().getName(),
                    ranking.getRelatedModel().getPlatform().getName(),
                    ranking.isUseExactMatching(),
                    ranking.getModelDetailContains(),
                    ranking.getModelEtcContains()
            );
        }

        return new TrendingModelData(
                ranking.getName(),
                ranking.getProvider(),
                ranking.getScore(),
                ranking.getRank(),
                new TrendingCategoryMetaData(ranking.getCategory().getName(), ranking.getCategory().getTitle()),
                relatedModel,
                relatedPostsCount
        );
    }

    public record SortOptionsResponse(Map<String, String> sort_options, @JsonProperty("default") String default_value) {
    }

    public record FilterOptionsResponse(
            List<PostService.IdNameData> platforms,
            List<PostService.IdNameData> categories,
            @JsonProperty("models_by_platform") Map<String, List<ModelFilterData>> models_by_platform
    ) {
    }

    public record ModelFilterData(Long id, String name, Long platform_id, String platform_name) {
    }

    public record CategoryRankingsResponse(String status, Map<String, TrendingCategoryData> data,
                                           boolean from_cache) {
    }

    public record SearchResult(
            List<PostService.PostCardData> results,
            PostService.PaginationData pagination
    ) {
    }

    public record RefreshCacheResponse(String status, String message) {
    }

    public record TrendingCategoryData(String title, String subtitle, String icon, List<RankingData> data) {
    }

    public record RankingData(int rank, String name, String score, String provider) {
    }

    public record TrendingModelPostsResponse(
            List<PostService.PostData> results,
            PostService.PaginationData pagination,
            TrendingModelData trending_model
    ) {
    }

    public record TrendingModelInfoResponse(String status, TrendingModelData data) {
    }

    public record TrendingModelData(
            String trending_name,
            String provider,
            String score,
            int rank,
            TrendingCategoryMetaData category,
            RelatedModelData related_model,
            long related_posts_count
    ) {
    }

    public record TrendingCategoryMetaData(String name, String title) {
    }

    public record RelatedModelData(
            Long id,
            String name,
            String platform,
            boolean exact_matching,
            String model_detail_filter,
            String model_etc_filter
    ) {
    }
}
