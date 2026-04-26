package com.lshlabs.prompthubspring.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.user.AppUser;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PlatformRepository platformRepository;
    private final AiModelRepository aiModelRepository;
    private final CategoryRepository categoryRepository;
    private final PostInteractionRepository interactionRepository;

    public PlatformsResponse listPlatforms() {
        List<IdNameData> data = platformRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .sorted(Comparator.comparingInt(p -> platformOrder(p.getName())))
                .map(p -> new IdNameData(p.getId(), p.getName()))
                .toList();
        return new PlatformsResponse("success", data);
    }

    public ModelsResponse listModels(Long platformId) {
        List<AiModel> models = platformId == null
                ? aiModelRepository.listAvailableModels()
                : aiModelRepository.findDisplayableByPlatformId(platformId);
        List<ModelData> data = models.stream().map(this::toModel).toList();
        return new ModelsResponse("success", data, data.isEmpty() ? null : data.get(0));
    }

    public PlatformModelsResponse platformModelsWithDefault(Long platformId) {
        Platform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "유효하지 않은 플랫폼 ID입니다."));
        List<ModelData> models = aiModelRepository
                .findDisplayableByPlatformId(platformId)
                .stream()
                .map(this::toModel)
                .toList();
        PlatformModelsData data = new PlatformModelsData(
                new IdNameData(platform.getId(), platform.getName()),
                models,
                models.isEmpty() ? null : models.get(0)
        );
        return new PlatformModelsResponse("success", data);
    }

    public CategoriesResponse listCategories() {
        List<IdNameData> data = categoryRepository.findAllByOrderByNameAsc().stream()
                .sorted(Comparator.comparingInt(c -> categoryOrder(c.getName())))
                .map(c -> new IdNameData(c.getId(), c.getName()))
                .toList();
        return new CategoriesResponse("success", data);
    }

    public TagsResponse listTags() {
        List<TagCountData> data = postRepository.countTags().stream()
                .map(row -> new TagCountData(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();

        return new TagsResponse("success", data);
    }

    @Transactional(readOnly = true)
    public PostListResponse listPosts(AppUser user, int page, int size,
            String search, String searchType,
            String sortBy, Long excludeId,
            String categoriesCsv, String platformsCsv, String modelsCsv,
            BigDecimal satisfactionMin, BigDecimal satisfactionMax) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(normalizedPage - 1, normalizedSize);

        Set<Long> categoryIds = parseCsvIds(categoriesCsv);
        Set<Long> platformIds = parseCsvIds(platformsCsv);
        Set<Long> modelIds = parseCsvIds(modelsCsv);

        Map<Long, String> categoryNames = categoryIds.isEmpty()
                ? Map.of()
                : categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<Long, String> modelNames = modelIds.isEmpty()
                ? Map.of()
                : aiModelRepository.findAllById(modelIds).stream()
                .collect(Collectors.toMap(AiModel::getId, AiModel::getName));

        Specification<Post> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String keyword = trimToNull(search);
            String normalizedSearchType = normalizeSearchType(searchType);

            if (keyword != null) {
                String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), like);
                Predicate authorPredicate = cb.like(
                        cb.lower(root.join("author", JoinType.LEFT).get("username")), like);
                Predicate contentPredicate = cb.or(
                        cb.like(cb.lower(root.get("prompt")), like),
                        cb.like(cb.lower(root.get("aiResponse")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("additionalOpinion"), "")), like),
                        buildTagContainsPredicate(root, query, cb, like)
                );
                // 레거시 계약(search_type)에 맞춰 검색 범위를 동적으로 바꾼다.
                predicates.add(switch (normalizedSearchType) {
                    case "title" -> titlePredicate;
                    case "content" -> contentPredicate;
                    case "author" -> authorPredicate;
                    case "title_content" -> cb.or(titlePredicate, contentPredicate);
                    default -> cb.or(titlePredicate, contentPredicate, authorPredicate);
                });
            }

            if (excludeId != null) {
                predicates.add(cb.notEqual(root.get("id"), excludeId));
            }

            if (!platformIds.isEmpty()) {
                predicates.add(root.get("platform").get("id").in(platformIds));
            }

            Predicate categoryFilter = buildCategoryFilterPredicate(root, cb, categoryIds, categoryNames);
            if (categoryFilter != null) {
                predicates.add(categoryFilter);
            }

            Predicate modelFilter = buildModelFilterPredicate(root, cb, modelIds, modelNames);
            if (modelFilter != null) {
                predicates.add(modelFilter);
            }

            if (satisfactionMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("satisfaction"), satisfactionMin));
            }
            if (satisfactionMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("satisfaction"), satisfactionMax));
            }

            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                query.orderBy(resolveListSortOrders(root, cb, sortBy));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return pageResult(postRepository.findAll(specification, pageable), user);
    }

    @Transactional(readOnly = true)
    public PostListResponse searchPosts(AppUser user, int page, int size, String q, String sort,
            Long platformId, Long categoryId, BigDecimal satisfactionMin, BigDecimal satisfactionMax) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), resolveSort(sort));
        Specification<Post> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String keyword = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), keyword),
                        cb.like(cb.lower(root.get("prompt")), keyword),
                        cb.like(cb.lower(root.get("aiResponse")), keyword),
                        buildTagContainsPredicate(root, query, cb, keyword)
                ));
            }
            if (platformId != null) {
                predicates.add(cb.equal(root.get("platform").get("id"), platformId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (satisfactionMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("satisfaction"), satisfactionMin));
            }
            if (satisfactionMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("satisfaction"), satisfactionMax));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return pageResult(postRepository.findAll(specification, pageable), user);
    }

    @Transactional(readOnly = true)
    public PostListResponse listPostsByModelKeyword(AppUser user, String modelKeyword, int page, int size,
            String sort) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), resolveSort(sort));
        return pageResult(postRepository.findByModelKeyword(modelKeyword, pageable), user);
    }

    @Transactional(readOnly = true)
    public PostListResponse listPostsByTrendingModel(AppUser user, AiModel relatedModel,
            boolean useExactMatching, String modelDetailContains, String modelEtcContains,
            int page, int size, String sort) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), resolveSort(sort));
        Specification<Post> specification = trendingModelSpecification(
                relatedModel, useExactMatching, modelDetailContains, modelEtcContains
        );
        return pageResult(postRepository.findAll(specification, pageable), user);
    }

    @Transactional(readOnly = true)
    public long countPostsByTrendingModel(AiModel relatedModel,
            boolean useExactMatching, String modelDetailContains, String modelEtcContains) {
        return postRepository.count(trendingModelSpecification(
                relatedModel, useExactMatching, modelDetailContains, modelEtcContains
        ));
    }

    @Transactional
    public PostDetailResponse detail(Long id, AppUser user) {
        postRepository.incrementViewCount(id);
        Post post = postRepository.findDetailById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        return new PostDetailResponse("success", toPost(post, user, true));
    }

    @Transactional
    public PostMutationResponse create(AppUser user, PostUpsertRequest body) {
        Post post = new Post();
        apply(post, body, user, true);
        post = postRepository.save(post);
        return new PostMutationResponse("success", "게시글이 성공적으로 생성되었습니다.", toPost(post, user, true));
    }

    @Transactional
    public PostMutationResponse update(AppUser user, Long id, PostUpsertRequest body) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "게시글을 수정할 권한이 없습니다.");
        }
        apply(post, body, user, false);
        post = postRepository.save(post);
        return new PostMutationResponse("success", "게시글이 성공적으로 수정되었습니다.", toPost(post, user, true));
    }

    @Transactional
    public MessageResponse delete(AppUser user, Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "게시글을 삭제할 권한이 없습니다.");
        }
        String title = post.getTitle();
        postRepository.delete(post);
        return new MessageResponse("success", "게시글 \"" + title + "\"이(가) 성공적으로 삭제되었습니다.");
    }

    @Transactional
    public ToggleLikeResponse toggleLike(AppUser user, Long id) {
        ToggleLikeResult result = toggleLikeData(user, id);
        return new ToggleLikeResponse("success", result.message(), result.data());
    }

    @Transactional
    public ToggleBookmarkResponse toggleBookmark(AppUser user, Long id) {
        ToggleBookmarkResult result = toggleBookmarkData(user, id);
        return new ToggleBookmarkResponse("success", result.message(), result.data());
    }

    @Transactional(readOnly = true)
    public PostListResponse liked(AppUser user, int page, int size) {
        Page<Post> posts = interactionRepository
                .findRecentLikesByUser(
                        user,
                        PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1))
                )
                .map(PostInteraction::getPost);
        return pageResult(posts, user);
    }

    @Transactional(readOnly = true)
    public PostListResponse bookmarked(AppUser user, int page, int size) {
        Page<Post> posts = interactionRepository
                .findRecentBookmarksByUser(
                        user,
                        PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1))
                )
                .map(PostInteraction::getPost);
        return pageResult(posts, user);
    }

    @Transactional(readOnly = true)
    public PostListResponse myPosts(AppUser user, int page, int size) {
        Page<Post> posts = postRepository.findByAuthor(
                user,
                PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return pageResult(posts, user);
    }

    public SuggestResponse suggestModels(String query, Long platformId) {
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "검색어를 입력해주세요.");
        }
        String normalizedQuery = query.trim();
        String qLower = normalizedQuery.toLowerCase(Locale.ROOT);
        List<AiModel> candidates = platformId == null
                ? aiModelRepository.searchSuggestCandidates(normalizedQuery)
                : aiModelRepository.searchSuggestCandidatesByPlatform(platformId, normalizedQuery);

        List<SuggestModelData> suggestions = candidates.stream()
                .sorted(Comparator.comparingDouble((AiModel m) -> -computeSuggestScore(m, qLower))
                        .thenComparingInt(AiModel::getSortOrder)
                        .thenComparing(m -> m.getName().toLowerCase(Locale.ROOT)))
                .limit(10)
                .map(this::toSuggestModel)
                .toList();

        return new SuggestResponse("success", new SuggestData(normalizedQuery, suggestions, suggestions.size()));
    }

    private ToggleLikeResult toggleLikeData(AppUser user, Long id) {
        // 동시 업데이트에서 카운트 정합성을 지키기 위해 FOR UPDATE 경로를 사용한다.
        Post post = postRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (post.getAuthor().getId().equals(user.getId())) {
            return new ToggleLikeResult(
                    "자신의 게시글에는 좋아요를 누를 수 없습니다.",
                    new ToggleLikeData(false, post.getLikeCount())
            );
        }

        PostInteraction interaction = interactionRepository.findByUserAndPost(user, post).orElseGet(() -> {
            PostInteraction created = new PostInteraction();
            created.setUser(user);
            created.setPost(post);
            return created;
        });

        interaction.setLiked(!interaction.isLiked());
        post.setLikeCount(Math.max(0, post.getLikeCount() + (interaction.isLiked() ? 1 : -1)));
        interactionRepository.save(interaction);
        postRepository.save(post);

        return new ToggleLikeResult(null, new ToggleLikeData(interaction.isLiked(), post.getLikeCount()));
    }

    private ToggleBookmarkResult toggleBookmarkData(AppUser user, Long id) {
        // 동시 업데이트에서 카운트 정합성을 지키기 위해 FOR UPDATE 경로를 사용한다.
        Post post = postRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (post.getAuthor().getId().equals(user.getId())) {
            return new ToggleBookmarkResult(
                    "자신의 게시글에는 북마크를 할 수 없습니다.",
                    new ToggleBookmarkData(false, post.getBookmarkCount())
            );
        }

        PostInteraction interaction = interactionRepository.findByUserAndPost(user, post).orElseGet(() -> {
            PostInteraction created = new PostInteraction();
            created.setUser(user);
            created.setPost(post);
            return created;
        });

        interaction.setBookmarked(!interaction.isBookmarked());
        post.setBookmarkCount(Math.max(0, post.getBookmarkCount() + (interaction.isBookmarked() ? 1 : -1)));
        interactionRepository.save(interaction);
        postRepository.save(post);

        return new ToggleBookmarkResult(null, new ToggleBookmarkData(interaction.isBookmarked(), post.getBookmarkCount()));
    }

    private static Specification<Post> trendingModelSpecification(AiModel relatedModel,
            boolean useExactMatching, String modelDetailContains, String modelEtcContains) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("model").get("id"), relatedModel.getId()));

            if (useExactMatching) {
                String detailKeyword = normalizeExactMatchKeyword(modelDetailContains);
                String etcKeyword = normalizeExactMatchKeyword(modelEtcContains);

                List<Predicate> exactPredicates = new ArrayList<>();
                if (detailKeyword != null) {
                    exactPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("modelDetail"), "")),
                            "%" + detailKeyword + "%"));
                }
                if (etcKeyword != null) {
                    exactPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("modelEtc"), "")),
                            "%" + etcKeyword + "%"));
                }
                if (!exactPredicates.isEmpty()) {
                    predicates.add(cb.or(exactPredicates.toArray(Predicate[]::new)));
                }
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void apply(Post post, PostUpsertRequest body, AppUser author, boolean creating) {
        if (creating) {
            post.setAuthor(author);
        }

        Platform platform = platformRepository.findById(body.platform())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "유효하지 않은 플랫폼입니다."));
        if (!platform.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "비활성화된 플랫폼은 선택할 수 없습니다.");
        }

        Category category = categoryRepository.findById(body.category())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리입니다."));

        post.setTitle(body.title().trim());
        post.setPlatform(platform);
        post.setCategory(category);

        AiModel model = null;
        if (body.model() != null) {
            model = aiModelRepository.findById(body.model())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "유효하지 않은 모델입니다."));
            if (!model.getPlatform().getId().equals(platform.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "선택한 모델이 플랫폼과 일치하지 않습니다.");
            }
            if (!model.isActive() || model.isDeprecated()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "비활성화되었거나 더 이상 사용되지 않는 모델입니다.");
            }
        }

        String modelEtc = trimToNull(body.model_etc());
        String modelDetail = trimToNull(body.model_detail());
        String categoryEtc = trimToNull(body.category_etc());

        // 플랫폼/모델/기타 입력 조합은 프론트-백엔드 호환 규칙을 그대로 강제한다.
        if (isEtc(platform.getName())) {
            if (model != null && !isEtc(model.getName())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "플랫폼이 기타인 경우 기타 모델만 선택할 수 있습니다.");
            }
            if (modelEtc == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "플랫폼이 기타인 경우 기타 모델명을 입력해야 합니다.");
            }
            if (modelDetail != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "플랫폼이 기타인 경우 상세 모델명을 사용할 수 없습니다.");
            }
        }
        if (model == null && modelEtc == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "모델을 선택하거나 기타 모델명을 입력해야 합니다.");
        }
        if (model == null && modelDetail != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "상세 모델명을 사용하려면 기본 모델을 선택해야 합니다.");
        }
        if (model != null && "기타".equals(model.getName()) && modelEtc == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "기타 모델을 선택한 경우 model_etc를 입력해야 합니다.");
        }
        if (model != null && "기타".equals(model.getName()) && modelDetail != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'기타' 모델에서는 상세 모델명을 사용할 수 없습니다.");
        }
        if (model != null && !"기타".equals(model.getName()) && modelEtc != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "기타 모델이 아닌 경우 model_etc를 입력할 수 없습니다.");
        }
        if ("기타".equals(category.getName()) && categoryEtc == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "기타 카테고리를 선택한 경우 category_etc를 입력해야 합니다.");
        }
        if (!"기타".equals(category.getName()) && categoryEtc != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "기타 카테고리가 아닌 경우 category_etc를 입력할 수 없습니다.");
        }

        post.setModel(model);
        post.setModelEtc(modelEtc);
        post.setModelDetail(modelDetail);
        post.setCategoryEtc(categoryEtc);
        post.setTags(normalizeTags(body.tags()));
        post.setPrompt(body.prompt().trim());
        post.setAiResponse(body.ai_response().trim());
        post.setAdditionalOpinion(trimToNull(body.additional_opinion()));

        if (body.satisfaction() != null) {
            validateSatisfactionHalfStep(body.satisfaction());
            post.setSatisfaction(body.satisfaction());
        } else {
            post.setSatisfaction(null);
        }
    }

    private static boolean isEtc(String value) {
        return value != null && "기타".equals(value.trim());
    }

    private static void validateSatisfactionHalfStep(BigDecimal satisfaction) {
        BigDecimal timesTen = satisfaction.multiply(BigDecimal.TEN);
        if (timesTen.stripTrailingZeros().scale() > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "만족도는 0.5점 단위로 입력해야 합니다.");
        }
        if (timesTen.remainder(BigDecimal.valueOf(5)).compareTo(BigDecimal.ZERO) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "만족도는 0.5점 단위로 입력해야 합니다.");
        }
    }

    private PostListResponse pageResult(Page<Post> page, AppUser user) {
        List<PostData> results = page.getContent().stream().map(p -> toPost(p, user, false)).toList();
        PaginationData pagination = new PaginationData(
                page.getNumber() + 1,
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext(),
                page.hasPrevious()
        );
        return new PostListResponse("success", new PostListData(results, pagination));
    }

    private PostData toPost(Post post, AppUser user, boolean detail) {
        boolean liked = false;
        boolean bookmarked = false;
        if (user != null) {
            Optional<PostInteraction> interaction = interactionRepository.findByUserAndPost(user, post);
            liked = interaction.map(PostInteraction::isLiked).orElse(false);
            bookmarked = interaction.map(PostInteraction::isBookmarked).orElse(false);
        }

        List<String> tags = normalizeTags(post.getTags());
        String avatarSrc = null;
        if (post.getAuthor().getProfileImage() != null && !post.getAuthor().getProfileImage().isBlank()) {
            avatarSrc = post.getAuthor().getProfileImage();
        }

        return new PostData(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getUsername(),
                post.getAuthor().getUsername().isBlank() ? "U" : post.getAuthor().getUsername().substring(0, 1).toUpperCase(),
                avatarSrc,
                post.getAuthor().getAvatarColor1(),
                post.getAuthor().getAvatarColor2(),
                post.getCreatedAt().toString(),
                RelativeTimeFormatter.format(post.getCreatedAt()),
                post.getViewCount(),
                post.getPlatform().getId(),
                post.getModel() == null ? null : post.getModel().getId(),
                post.getCategory().getId(),
                post.getModelEtc() == null ? "" : post.getModelEtc(),
                post.getModelDetail() == null ? "" : post.getModelDetail(),
                post.getCategoryEtc() == null ? "" : post.getCategoryEtc(),
                post.modelDisplayName(),
                post.categoryDisplayName(),
                post.getLikeCount(),
                liked,
                post.getBookmarkCount(),
                bookmarked,
                post.getSatisfaction(),
                tags,
                detail ? post.getPrompt() : null,
                detail ? post.getAiResponse() : null,
                detail ? post.getAiResponse() : null,
                detail ? (post.getAdditionalOpinion() == null ? "" : post.getAdditionalOpinion()) : null,
                detail ? (post.getAdditionalOpinion() == null ? "" : post.getAdditionalOpinion()) : null,
                detail ? user != null && post.getAuthor().getId().equals(user.getId()) : null
        );
    }

    public List<PostCardData> toPostCardDataList(List<PostData> results) {
        return results.stream().map(this::toPostCardData).toList();
    }

    private PostCardData toPostCardData(PostData data) {
        return new PostCardData(
                data.id(),
                data.title(),
                data.author(),
                data.authorInitial(),
                data.avatarSrc(),
                data.authorAvatarColor1(),
                data.authorAvatarColor2(),
                data.createdAt(),
                data.relativeTime(),
                data.views(),
                data.platformId(),
                data.modelId(),
                data.categoryId(),
                data.modelEtc(),
                data.modelDetail(),
                data.categoryEtc(),
                data.modelDisplayName(),
                data.categoryDisplayName(),
                data.likes(),
                data.isLiked(),
                data.bookmarks(),
                data.isBookmarked(),
                data.satisfaction(),
                data.tags()
        );
    }

    private ModelData toModel(AiModel model) {
        return new ModelData(
                model.getId(),
                model.getName(),
                model.getPlatform().getId(),
                model.getPlatform().getName()
        );
    }

    private SuggestModelData toSuggestModel(AiModel model) {
        SuggestPlatformData platform = new SuggestPlatformData(
                model.getPlatform().getId(),
                model.getPlatform().getName(),
                normalizeSuggestSlug(model.getPlatform().getSlug(), model.getPlatform().getName())
        );
        return new SuggestModelData(
                model.getId(),
                model.getName(),
                normalizeSuggestSlug(model.getSlug(), model.getName()),
                platform
        );
    }

    private List<jakarta.persistence.criteria.Order> resolveListSortOrders(
            jakarta.persistence.criteria.Root<Post> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String sortBy) {
        String normalizedSort = normalizeSort(sortBy);
        if ("oldest".equals(normalizedSort)) {
            return List.of(cb.asc(root.get("createdAt")));
        }
        if ("popular".equals(normalizedSort)) {
            return List.of(
                    cb.desc(cb.sum(root.get("likeCount"), root.get("bookmarkCount"))),
                    cb.desc(root.get("createdAt"))
            );
        }
        if ("satisfaction".equals(normalizedSort)) {
            return List.of(
                    cb.asc(cb.selectCase().when(cb.isNull(root.get("satisfaction")), 1).otherwise(0)),
                    cb.desc(root.get("satisfaction")),
                    cb.desc(root.get("createdAt"))
            );
        }
        if ("views".equals(normalizedSort)) {
            return List.of(
                    cb.desc(root.get("viewCount")),
                    cb.desc(root.get("createdAt"))
            );
        }
        return List.of(cb.desc(root.get("createdAt")));
    }

    private static String normalizeSort(String sortBy) {
        String sort = trimToNull(sortBy);
        if (sort == null) {
            return "latest";
        }
        String normalized = sort.toLowerCase(Locale.ROOT);
        if ("rating".equals(normalized)) {
            return "satisfaction";
        }
        if ("most_viewed".equals(normalized)) {
            return "views";
        }
        if ("most_liked".equals(normalized)) {
            return "popular";
        }
        return normalized;
    }

    private Sort resolveSort(String sort) {
        String normalizedSort = normalizeSort(sort);
        if ("oldest".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        if ("views".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.DESC, "viewCount");
        }
        if ("popular".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.DESC, "likeCount")
                    .and(Sort.by(Sort.Direction.DESC, "bookmarkCount"))
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        if ("satisfaction".equals(normalizedSort)) {
            return Sort.by(Sort.Order.desc("satisfaction").nullsLast()).and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private static String normalizeSearchType(String searchType) {
        String value = trimToNull(searchType);
        if (value == null) {
            return "all";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "title", "content", "author", "title_content", "all" -> normalized;
            default -> "all";
        };
    }

    private static Predicate buildTagContainsPredicate(jakarta.persistence.criteria.Root<Post> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String like) {
        if (query == null) {
            return cb.disjunction();
        }
        var subQuery = query.subquery(Long.class);
        var subRoot = subQuery.from(Post.class);
        var subTag = subRoot.joinList("tags", JoinType.INNER);
        subQuery.select(subRoot.get("id"))
                .where(
                        cb.equal(subRoot.get("id"), root.get("id")),
                        cb.like(cb.lower(subTag.as(String.class)), like)
                );
        return cb.exists(subQuery);
    }

    private Predicate buildCategoryFilterPredicate(jakarta.persistence.criteria.Root<Post> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Set<Long> categoryIds,
            Map<Long, String> categoryNames) {
        if (categoryIds.isEmpty()) {
            return null;
        }
        List<Predicate> orPredicates = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            String name = categoryNames.get(categoryId);
            if (name == null) {
                continue;
            }
            if ("기타".equals(name)) {
                // '기타' 필터는 category_etc 실값이 있는 데이터만 반환한다.
                orPredicates.add(cb.and(
                        cb.equal(root.get("category").get("id"), categoryId),
                        cb.isNotNull(root.get("categoryEtc")),
                        cb.greaterThan(cb.length(cb.trim(root.get("categoryEtc"))), 0)
                ));
            } else {
                orPredicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
        }
        if (orPredicates.isEmpty()) {
            return null;
        }
        return cb.or(orPredicates.toArray(Predicate[]::new));
    }

    private Predicate buildModelFilterPredicate(jakarta.persistence.criteria.Root<Post> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Set<Long> modelIds,
            Map<Long, String> modelNames) {
        if (modelIds.isEmpty()) {
            return null;
        }
        List<Predicate> orPredicates = new ArrayList<>();
        for (Long modelId : modelIds) {
            String name = modelNames.get(modelId);
            if (name == null) {
                continue;
            }
            if ("기타".equals(name)) {
                // '기타' 모델도 model_etc 실값이 있는 데이터만 매칭한다.
                orPredicates.add(cb.and(
                        cb.equal(root.get("model").get("id"), modelId),
                        cb.isNotNull(root.get("modelEtc")),
                        cb.greaterThan(cb.length(cb.trim(root.get("modelEtc"))), 0)
                ));
            } else {
                orPredicates.add(cb.equal(root.get("model").get("id"), modelId));
            }
        }
        if (orPredicates.isEmpty()) {
            return null;
        }
        return cb.or(orPredicates.toArray(Predicate[]::new));
    }

    private static Set<Long> parseCsvIds(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return Set.of();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static double computeSuggestScore(AiModel model, String qLower) {
        String name = normalizeSuggestSlug(model.getName(), "");
        String slug = normalizeSuggestSlug(model.getSlug(), model.getName());
        String platformName = normalizeSuggestSlug(model.getPlatform().getName(), "");
        String platformSlug = normalizeSuggestSlug(model.getPlatform().getSlug(), model.getPlatform().getName());

        double score = 0.0;
        if (name.startsWith(qLower)) score += 3.0;
        if (slug.startsWith(qLower)) score += 2.5;
        if (platformName.startsWith(qLower)) score += 2.0;
        if (platformSlug.startsWith(qLower)) score += 1.8;
        if (name.contains(qLower)) score += 1.0;
        if (slug.contains(qLower)) score += 0.8;
        if (platformName.contains(qLower)) score += 0.7;
        if (platformSlug.contains(qLower)) score += 0.6;
        return score;
    }

    private static String normalizeSuggestSlug(String slug, String fallbackName) {
        String value = trimToNull(slug);
        if (value != null) {
            return value.toLowerCase(Locale.ROOT);
        }
        String fallback = trimToNull(fallbackName);
        if (fallback == null) {
            return "";
        }
        return fallback.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .map(PostService::trimToNull)
                .filter(Objects::nonNull)
                .toList();
    }

    private static int platformOrder(String name) {
        return switch (name) {
            case "OpenAI" -> 1;
            case "Anthropic" -> 2;
            case "Google" -> 3;
            case "xAI" -> 4;
            case "Meta" -> 5;
            case "Mistral" -> 6;
            case "DeepSeek" -> 7;
            case "기타" -> 8;
            default -> 999;
        };
    }

    private static String normalizeExactMatchKeyword(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");
        return normalized.isBlank() ? null : normalized;
    }

    private static int categoryOrder(String name) {
        return switch (name) {
            case "코딩/프로그래밍" -> 1;
            case "일반지식/학습" -> 2;
            case "글쓰기/번역" -> 3;
            case "AI/자연어처리" -> 4;
            case "취업/커리어" -> 5;
            case "생활정보/상담" -> 6;
            case "문화/엔터테인먼트/게임" -> 7;
            case "비즈니스/경제" -> 8;
            case "기술문서/요약" -> 9;
            case "데이터분석/통계" -> 10;
            case "기타" -> 11;
            default -> 999;
        };
    }

    public record IdNameData(Long id, String name) {
    }

    public record ModelData(Long id, String name, Long platform, String platform_name) {
    }

    public record TagCountData(String name, long count) {
    }

    public record PlatformsResponse(String status, List<IdNameData> data) {
    }

    public record CategoriesResponse(String status, List<IdNameData> data) {
    }

    public record TagsResponse(String status, List<TagCountData> data) {
    }

    public record ModelsResponse(String status, List<ModelData> data, ModelData default_model) {
    }

    public record PlatformModelsData(IdNameData platform, List<ModelData> models, ModelData default_model) {
    }

    public record PlatformModelsResponse(String status, PlatformModelsData data) {
    }

    public record PaginationData(int current_page, int total_pages, long total_count, boolean has_next,
                                 boolean has_previous) {
    }

    public record PostListData(List<PostData> results, PaginationData pagination) {
    }

    public record PostListResponse(String status, PostListData data) {
    }

    public record PostDetailResponse(String status, PostData data) {
    }

    public record PostMutationResponse(String status, String message, PostData data) {
    }

    public record MessageResponse(String status, String message) {
    }

    public record ToggleLikeData(boolean is_liked, long like_count) {
    }

    public record ToggleBookmarkData(boolean is_bookmarked, long bookmark_count) {
    }

    private record ToggleLikeResult(String message, ToggleLikeData data) {
    }

    private record ToggleBookmarkResult(String message, ToggleBookmarkData data) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToggleLikeResponse(String status, String message, ToggleLikeData data) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToggleBookmarkResponse(String status, String message, ToggleBookmarkData data) {
    }

    public record SuggestPlatformData(Long id, String name, String slug) {
    }

    public record SuggestModelData(Long id, String name, String slug, SuggestPlatformData platform) {
    }

    public record SuggestData(String query, List<SuggestModelData> suggestions, int total_count) {
    }

    public record SuggestResponse(String status, SuggestData data) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PostData(
            Long id,
            String title,
            String author,
            String authorInitial,
            String avatarSrc,
            String authorAvatarColor1,
            String authorAvatarColor2,
            String createdAt,
            String relativeTime,
            long views,
            Long platformId,
            Long modelId,
            Long categoryId,
            String modelEtc,
            String modelDetail,
            String categoryEtc,
            String modelDisplayName,
            String categoryDisplayName,
            long likes,
            boolean isLiked,
            long bookmarks,
            boolean isBookmarked,
            BigDecimal satisfaction,
            List<String> tags,
            String prompt,
            String aiResponse,
            String ai_response,
            String additionalOpinion,
            String additional_opinion,
            Boolean isAuthor
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PostCardData(
            Long id,
            String title,
            String author,
            String authorInitial,
            String avatarSrc,
            String authorAvatarColor1,
            String authorAvatarColor2,
            String createdAt,
            String relativeTime,
            long views,
            Long platformId,
            Long modelId,
            Long categoryId,
            String modelEtc,
            String modelDetail,
            String categoryEtc,
            String modelDisplayName,
            String categoryDisplayName,
            long likes,
            boolean isLiked,
            long bookmarks,
            boolean isBookmarked,
            BigDecimal satisfaction,
            List<String> tags
    ) {
    }
}
