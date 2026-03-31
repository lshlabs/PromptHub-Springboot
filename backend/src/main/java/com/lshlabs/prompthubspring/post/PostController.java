package com.lshlabs.prompthubspring.post;

import com.lshlabs.prompthubspring.security.AuthSupport;
import com.lshlabs.prompthubspring.user.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final AuthSupport authSupport;

    @GetMapping({ "/platforms", "/platforms/" })
    public ResponseEntity<PostService.PlatformsResponse> platforms() {
        return ResponseEntity.ok(postService.listPlatforms());
    }

    @GetMapping({ "/models", "/models/" })
    public ResponseEntity<PostService.ModelsResponse> models(@RequestParam(value = "platform_id", required = false) Long platformId) {
        return ResponseEntity.ok(postService.listModels(platformId));
    }

    @GetMapping({ "/platforms/{platformId}/models", "/platforms/{platformId}/models/" })
    public ResponseEntity<PostService.PlatformModelsResponse> platformModels(@PathVariable Long platformId) {
        return ResponseEntity.ok(postService.platformModelsWithDefault(platformId));
    }

    @GetMapping({ "/categories", "/categories/" })
    public ResponseEntity<PostService.CategoriesResponse> categories() {
        return ResponseEntity.ok(postService.listCategories());
    }

    @GetMapping({ "/tags", "/tags/" })
    public ResponseEntity<PostService.TagsResponse> tags() {
        return ResponseEntity.ok(postService.listTags());
    }

    @GetMapping({ "", "/" })
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

    @GetMapping({ "/{postId}", "/{postId}/" })
    public ResponseEntity<PostService.PostDetailResponse> detail(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.detail(postId, currentUserOrNull()));
    }

    @PostMapping({ "", "/" })
    public ResponseEntity<PostService.PostMutationResponse> createCanonical(@Valid @RequestBody PostUpsertRequest payload) {
        return createInternal(payload, false);
    }

    @PostMapping({ "/create", "/create/" })
    public ResponseEntity<PostService.PostMutationResponse> create(@Valid @RequestBody PostUpsertRequest payload) {
        return createInternal(payload, true);
    }

    @PutMapping({ "/{postId}", "/{postId}/" })
    public ResponseEntity<PostService.PostMutationResponse> updatePutCanonical(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpsertRequest payload
    ) {
        return updateInternal(postId, payload, false);
    }

    @PatchMapping({ "/{postId}", "/{postId}/" })
    public ResponseEntity<PostService.PostMutationResponse> updatePatchCanonical(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpsertRequest payload
    ) {
        return updateInternal(postId, payload, false);
    }

    @RequestMapping(value = { "/{postId}/update", "/{postId}/update/" }, method = { RequestMethod.PUT, RequestMethod.PATCH })
    public ResponseEntity<PostService.PostMutationResponse> update(@PathVariable Long postId, @Valid @RequestBody PostUpsertRequest payload) {
        return updateInternal(postId, payload, true);
    }

    @DeleteMapping({ "/{postId}", "/{postId}/" })
    public ResponseEntity<PostService.MessageResponse> deleteCanonical(@PathVariable Long postId) {
        return deleteInternal(postId, false);
    }

    @DeleteMapping({ "/{postId}/delete", "/{postId}/delete/" })
    public ResponseEntity<PostService.MessageResponse> delete(@PathVariable Long postId) {
        return deleteInternal(postId, true);
    }

    @PostMapping({ "/{postId}/like", "/{postId}/like/" })
    public ResponseEntity<PostService.ToggleLikeResponse> like(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.toggleLike(authSupport.currentUserOrThrow(), postId));
    }

    @PostMapping({ "/{postId}/bookmark", "/{postId}/bookmark/" })
    public ResponseEntity<PostService.ToggleBookmarkResponse> bookmark(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.toggleBookmark(authSupport.currentUserOrThrow(), postId));
    }

    @GetMapping({ "/liked-posts", "/liked-posts/" })
    public ResponseEntity<PostService.PostListResponse> liked(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(postService.liked(authSupport.currentUserOrThrow(), page, pageSize));
    }

    @GetMapping({ "/bookmarked-posts", "/bookmarked-posts/" })
    public ResponseEntity<PostService.PostListResponse> bookmarked(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(postService.bookmarked(authSupport.currentUserOrThrow(), page, pageSize));
    }

    @GetMapping({ "/my-posts", "/my-posts/" })
    public ResponseEntity<PostService.PostListResponse> myPosts(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(postService.myPosts(authSupport.currentUserOrThrow(), page, pageSize));
    }

    @GetMapping({ "/models/suggest", "/models/suggest/" })
    public ResponseEntity<PostService.SuggestResponse> suggestModels(@RequestParam("query") String query,
            @RequestParam(value = "platform_id", required = false) Long platformId) {
        return ResponseEntity.ok(postService.suggestModels(query, platformId));
    }

    private AppUser currentUserOrNull() {
        try {
            return authSupport.currentUserOrThrow();
        } catch (Exception ignored) {
            return null;
        }
    }

    private ResponseEntity<PostService.PostMutationResponse> createInternal(PostUpsertRequest payload, boolean legacyAlias) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(201);
        if (legacyAlias) {
            builder.header(HttpHeaders.WARNING, "299 - Deprecated legacy endpoint, use POST /api/posts");
        }
        return builder.body(postService.create(authSupport.currentUserOrThrow(), payload));
    }

    private ResponseEntity<PostService.PostMutationResponse> updateInternal(Long postId, PostUpsertRequest payload,
            boolean legacyAlias) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (legacyAlias) {
            builder.header(HttpHeaders.WARNING, "299 - Deprecated legacy endpoint, use PUT/PATCH /api/posts/{postId}");
        }
        return builder.body(postService.update(authSupport.currentUserOrThrow(), postId, payload));
    }

    private ResponseEntity<PostService.MessageResponse> deleteInternal(Long postId, boolean legacyAlias) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (legacyAlias) {
            builder.header(HttpHeaders.WARNING, "299 - Deprecated legacy endpoint, use DELETE /api/posts/{postId}");
        }
        return builder.body(postService.delete(authSupport.currentUserOrThrow(), postId));
    }
}
