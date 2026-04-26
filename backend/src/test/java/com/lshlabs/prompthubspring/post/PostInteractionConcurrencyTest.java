package com.lshlabs.prompthubspring.post;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_post_interaction_concurrency;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Tag("integration")
class PostInteractionConcurrencyTest {

    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostInteractionRepository postInteractionRepo;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void concurrentToggle_keepsLikeAndBookmarkCountsConsistent() throws Exception {
        AppUser author = saveUser("author");
        AppUser userOne = saveUser("u1");
        AppUser userTwo = saveUser("u2");
        AppUser userThree = saveUser("u3");
        Platform platform = savePlatform("OpenAI");
        Category category = saveCategory("일반");
        Post post = savePost(author, platform, category);

        List<Long> userIds = List.of(userOne.getId(), userTwo.getId(), userThree.getId(), userOne.getId(), userTwo.getId(), userThree.getId());
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            final long userId = userIds.get(i % userIds.size());
            final boolean like = i % 2 == 0;
            tasks.add(() -> {
                AppUser user = appUserRepository.findById(userId).orElseThrow();
                if (like) {
                    postService.toggleLike(user, post.getId());
                } else {
                    postService.toggleBookmark(user, post.getId());
                }
                return null;
            });
        }
        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        Post reloaded = postRepository.findById(post.getId()).orElseThrow();
        long likedCount = postInteractionRepo.countByPostAndLikedTrue(reloaded);
        long bookmarkedCount = postInteractionRepo.countByPostAndBookmarkedTrue(reloaded);

        assertEquals(likedCount, reloaded.getLikeCount());
        assertEquals(bookmarkedCount, reloaded.getBookmarkCount());
    }

    @Test
    void concurrentDetail_incrementsViewCountWithoutLoss() throws Exception {
        AppUser author = saveUser("author2");
        Platform platform = savePlatform("Anthropic");
        Category category = saveCategory("테스트");
        Post post = savePost(author, platform, category);

        int calls = 80;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < calls; i++) {
            tasks.add(() -> {
                postService.detail(post.getId(), null);
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        Post reloaded = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(calls, reloaded.getViewCount());
    }

    @Test
    void likedAndBookmarkedEndpoints_matchInteractionState() {
        AppUser author = saveUser("author3");
        AppUser currentUser = saveUser("current-user");
        Platform platform = savePlatform("Google");
        Category category = saveCategory("검증");
        Post likedPost = savePost(author, platform, category);
        Post bookmarkedPost = savePost(author, platform, category);

        postService.toggleLike(currentUser, likedPost.getId());
        postService.toggleBookmark(currentUser, bookmarkedPost.getId());

        PostService.PostListResponse liked = postService.liked(currentUser, 1, 20);
        PostService.PostListResponse bookmarked = postService.bookmarked(currentUser, 1, 20);

        List<PostService.PostData> likedResults = liked.data().results();
        List<PostService.PostData> bookmarkedResults = bookmarked.data().results();

        assertEquals(1, likedResults.size());
        assertEquals(likedPost.getId(), likedResults.get(0).id());
        assertEquals(1, bookmarkedResults.size());
        assertEquals(bookmarkedPost.getId(), bookmarkedResults.get(0).id());
    }

    private AppUser saveUser(String username) {
        String unique = username + "_" + UUID.randomUUID().toString().substring(0, 8);
        AppUser user = new AppUser();
        user.setUsername(unique);
        user.setEmail(unique + "@example.com");
        user.setPassword("encoded");
        return appUserRepository.save(user);
    }

    private Platform savePlatform(String name) {
        Platform platform = new Platform();
        platform.setName(name + "_" + UUID.randomUUID().toString().substring(0, 8));
        platform.setActive(true);
        return platformRepository.save(platform);
    }

    private Category saveCategory(String name) {
        Category category = new Category();
        category.setName(name + "_" + UUID.randomUUID().toString().substring(0, 8));
        return categoryRepository.save(category);
    }

    private Post savePost(AppUser author, Platform platform, Category category) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setTitle("동시성 테스트 게시글");
        post.setPrompt("동시성 테스트용 prompt 본문입니다.");
        post.setAiResponse("동시성 테스트용 ai response 본문입니다.");
        post.setTags(java.util.List.of("test"));
        return postRepository.save(post);
    }
}
