package com.lshlabs.prompthubspring.post;

import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
class PostInteractionConcurrencyTest {

    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostInteractionRepository postInteractionRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void concurrentToggle_keepsLikeAndBookmarkCountsConsistent() throws Exception {
        AppUser author = saveUser("author");
        AppUser u1 = saveUser("u1");
        AppUser u2 = saveUser("u2");
        AppUser u3 = saveUser("u3");
        Platform platform = savePlatform("OpenAI");
        Category category = saveCategory("일반");
        Post post = savePost(author, platform, category);

        List<Long> userIds = List.of(u1.getId(), u2.getId(), u3.getId(), u1.getId(), u2.getId(), u3.getId());
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
        long likedCount = postInteractionRepository.countByPostAndLikedTrue(reloaded);
        long bookmarkedCount = postInteractionRepository.countByPostAndBookmarkedTrue(reloaded);

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
        AppUser me = saveUser("me");
        Platform platform = savePlatform("Google");
        Category category = saveCategory("검증");
        Post post1 = savePost(author, platform, category);
        Post post2 = savePost(author, platform, category);

        postService.toggleLike(me, post1.getId());
        postService.toggleBookmark(me, post2.getId());

        PostService.PostListResponse liked = postService.liked(me, 1, 20);
        PostService.PostListResponse bookmarked = postService.bookmarked(me, 1, 20);

        List<PostService.PostData> likedResults = liked.data().results();
        List<PostService.PostData> bookmarkedResults = bookmarked.data().results();

        assertEquals(1, likedResults.size());
        assertEquals(post1.getId(), likedResults.get(0).id());
        assertEquals(1, bookmarkedResults.size());
        assertEquals(post2.getId(), bookmarkedResults.get(0).id());
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
        Platform p = new Platform();
        p.setName(name + "_" + UUID.randomUUID().toString().substring(0, 8));
        p.setActive(true);
        return platformRepository.save(p);
    }

    private Category saveCategory(String name) {
        Category c = new Category();
        c.setName(name + "_" + UUID.randomUUID().toString().substring(0, 8));
        return categoryRepository.save(c);
    }

    private Post savePost(AppUser author, Platform platform, Category category) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setTitle("동시성 테스트 게시글");
        post.setPrompt("동시성 테스트용 prompt 본문입니다.");
        post.setAiResponse("동시성 테스트용 ai response 본문입니다.");
        post.setTags("test");
        return postRepository.save(post);
    }
}
