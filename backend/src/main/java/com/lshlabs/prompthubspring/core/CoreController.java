package com.lshlabs.prompthubspring.core;

import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/core")
public class CoreController {
    private final CoreService coreService;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("ok"));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "search_type", required = false) String searchType,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "10") int pageSize,
            @RequestParam(value = "categories", required = false) String categories,
            @RequestParam(value = "platforms", required = false) String platforms,
            @RequestParam(value = "models", required = false) String models,
            @RequestParam(value = "platform", required = false) Long platform,
            @RequestParam(value = "category", required = false) Long category,
            @RequestParam(value = "satisfaction_min", required = false) BigDecimal satisfactionMin,
            @RequestParam(value = "satisfaction_max", required = false) BigDecimal satisfactionMax) {
        CoreService.SearchResult result = coreService.searchForCompatibilityContract(
                q, searchType, sort, page, pageSize,
                categories, platforms, models,
                platform, category, satisfactionMin, satisfactionMax
        );
        PostService.PaginationData pagination = result.pagination();
        String next = pagination.has_next() ? buildPageLink(pagination.current_page() + 1) : null;
        String previous = pagination.has_previous() ? buildPageLink(pagination.current_page() - 1) : null;
        long total = pagination.total_count();
        return ResponseEntity.ok(new SearchResponse(
                total,
                total,
                next,
                previous,
                result.results(),
                pagination.current_page(),
                pagination.total_pages(),
                pagination.has_next(),
                pagination.has_previous()
        ));
    }

    @GetMapping("/sort-options")
    public ResponseEntity<CoreService.SortOptionsResponse> sortOptions() {
        return ResponseEntity.ok(coreService.sortOptions());
    }

    @GetMapping("/filter-options")
    public ResponseEntity<CoreService.FilterOptionsResponse> filterOptions() {
        return ResponseEntity.ok(coreService.filterOptions());
    }

    @GetMapping("/trending/category-rankings")
    public ResponseEntity<CoreService.CategoryRankingsResponse> categoryRankings() {
        return ResponseEntity.ok(coreService.categoryRankings());
    }

    @PostMapping("/trending/refresh-cache")
    public ResponseEntity<CoreService.RefreshCacheResponse> refreshTrendingCache() {
        return ResponseEntity.ok(coreService.refreshTrendingCache());
    }

    @GetMapping("/trending/model/{modelName}/posts")
    public ResponseEntity<CoreService.TrendingModelPostsResponse> trendingModelPosts(@PathVariable String modelName,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(value = "sort", required = false) String sort) {
        CoreService.TrendingModelPostsResponse data = coreService.trendingModelPosts(modelName, page, pageSize, sort);
        if (data == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "해당 트렌딩 모델을 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/trending/model/{modelName}/info")
    public ResponseEntity<CoreService.TrendingModelInfoResponse> trendingModelInfo(@PathVariable String modelName) {
        CoreService.TrendingModelInfoResponse data = coreService.trendingModelInfo(modelName);
        if (data == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "해당 트렌딩 모델을 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(data);
    }

    private String buildPageLink(int page) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQueryParam("page", page)
                .build()
                .toUriString();
    }

    public record SearchResponse(
            long count,
            long total_count,
            String next,
            String previous,
            List<PostService.PostCardData> results,
            int current_page,
            int total_pages,
            boolean has_next,
            boolean has_previous
    ) {
    }

    public record HealthResponse(String status) {
    }
}
