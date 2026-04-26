package com.lshlabs.prompthubspring.post;

import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.security.AuthSupport;
import com.lshlabs.prompthubspring.user.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final AuthSupport authSupport;

    @GetMapping("/platforms")
    public ResponseEntity<PostService.PlatformsResponse> platforms() {
        return ResponseEntity.ok(postService.listPlatforms());
    }

    @GetMapping("/models")
    public ResponseEntity<PostService.ModelsResponse> models(@RequestParam(value = "platform_id", required = false) Long platformId) {
        return ResponseEntity.ok(postService.listModels(platformId));
    }

    @GetMapping("/platforms/{platformId}/models")
    public ResponseEntity<PostService.PlatformModelsResponse> platformModels(@PathVariable Long platformId) {
        return ResponseEntity.ok(postService.platformModelsWithDefault(platformId));
    }

    @GetMapping("/categories")
    public ResponseEntity<PostService.CategoriesResponse> categories() {
        return ResponseEntity.ok(postService.listCategories());
    }

    @GetMapping("/tags")
    public ResponseEntity<PostService.TagsResponse> tags() {
        return ResponseEntity.ok(postService.listTags());
    }

    @GetMapping("")
    public ResponseEntity<PostService.PostListResponse> list(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "10") int pageSize,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "search_type", required = false) String searchType,
            @RequestParam(value = "sort_by", required = false) String sortBy,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "exclude_id", required = false) Long excludeId,
            @RequestParam(value = "categories", required = false) String categories,
            @RequestParam(value = "platforms", required = false) String platforms,
            @RequestParam(value = "models", required = false) String models) {
        AppUser user = currentUserOrNull();
        String resolvedSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : sort;
        return ResponseEntity.ok(postService.listPosts(
                user,
                page,
                pageSize,
                search,
                searchType,
                resolvedSortBy,
                excludeId,
                categories,
                platforms,
                models,
                null,
                null
        ));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostService.PostDetailResponse> detail(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.detail(postId, currentUserOrNull()));
    }

    @PostMapping("")
    public ResponseEntity<PostService.PostMutationResponse> create(@Valid @RequestBody PostUpsertRequest payload) {
        return ResponseEntity.status(201).body(postService.create(authSupport.currentUserOrThrow(), payload));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostService.PostMutationResponse> updatePutCanonical(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpsertRequest payload
    ) {
        return ResponseEntity.ok(postService.update(authSupport.currentUserOrThrow(), postId, payload));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostService.PostMutationResponse> updatePatchCanonical(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpsertRequest payload
    ) {
        return ResponseEntity.ok(postService.update(authSupport.currentUserOrThrow(), postId, payload));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<PostService.MessageResponse> delete(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.delete(authSupport.currentUserOrThrow(), postId));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<PostService.ToggleLikeResponse> like(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.toggleLike(authSupport.currentUserOrThrow(), postId));
    }

    @PostMapping("/{postId}/bookmark")
    public ResponseEntity<PostService.ToggleBookmarkResponse> bookmark(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.toggleBookmark(authSupport.currentUserOrThrow(), postId));
    }

    @GetMapping("/liked-posts")
    public ResponseEntity<PostService.PostListResponse> liked(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(postService.liked(authSupport.currentUserOrThrow(), page, pageSize));
    }

    @GetMapping("/bookmarked-posts")
    public ResponseEntity<PostService.PostListResponse> bookmarked(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(postService.bookmarked(authSupport.currentUserOrThrow(), page, pageSize));
    }

    @GetMapping("/my-posts")
    public ResponseEntity<PostService.PostListResponse> myPosts(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(postService.myPosts(authSupport.currentUserOrThrow(), page, pageSize));
    }

    @GetMapping("/models/suggest")
    public ResponseEntity<PostService.SuggestResponse> suggestModels(@RequestParam("query") String query,
            @RequestParam(value = "platform_id", required = false) Long platformId) {
        return ResponseEntity.ok(postService.suggestModels(query, platformId));
    }

    private AppUser currentUserOrNull() {
        try {
            return authSupport.currentUserOrThrow();
        } catch (ApiException exception) {
            if (exception.getStatus() == HttpStatus.UNAUTHORIZED) {
                return null;
            }
            throw exception;
        }
    }

}
