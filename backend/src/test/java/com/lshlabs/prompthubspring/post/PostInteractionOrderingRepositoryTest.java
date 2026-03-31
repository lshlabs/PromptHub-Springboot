package com.lshlabs.prompthubspring.post;

import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PostInteractionOrderingRepositoryTest {

    @Autowired
    private PostInteractionRepository postInteractionRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PostRepository postRepository;

    @Test
    void likedOrdering_matchesLegacyInteractionUpdatedAtDesc() {
        AppUser me = saveUser("me");
        AppUser author = saveUser("author");
        Platform platform = savePlatform("OpenAI");
        Category category = saveCategory("검증");

        Post newerPost = savePost(author, platform, category, "newer");
        Post olderPost = savePost(author, platform, category, "older");

        // Insert order intentionally opposite to updated_at order.
        PostInteraction older = saveInteraction(me, olderPost, true, false, Instant.parse("2026-02-01T00:00:00Z"));
        PostInteraction newer = saveInteraction(me, newerPost, true, false, Instant.parse("2026-03-01T00:00:00Z"));

        var page = postInteractionRepository.findByUserAndLikedTrueOrderByUpdatedAtDescIdDesc(
                me,
                PageRequest.of(0, 20)
        );

        assertEquals(2, page.getTotalElements());
        assertEquals(newer.getId(), page.getContent().get(0).getId());
        assertEquals(older.getId(), page.getContent().get(1).getId());
    }

    @Test
    void bookmarkedOrdering_tiebreaksByIdDesc_whenUpdatedAtSame() {
        AppUser me = saveUser("me2");
        AppUser author = saveUser("author2");
        Platform platform = savePlatform("Anthropic");
        Category category = saveCategory("동률검증");

        Post p1 = savePost(author, platform, category, "p1");
        Post p2 = savePost(author, platform, category, "p2");
        Instant sameTime = Instant.parse("2026-03-10T00:00:00Z");

        PostInteraction first = saveInteraction(me, p1, false, true, sameTime);
        PostInteraction second = saveInteraction(me, p2, false, true, sameTime);

        var page = postInteractionRepository.findByUserAndBookmarkedTrueOrderByUpdatedAtDescIdDesc(
                me,
                PageRequest.of(0, 20)
        );

        assertEquals(2, page.getTotalElements());
        assertEquals(second.getId(), page.getContent().get(0).getId());
        assertEquals(first.getId(), page.getContent().get(1).getId());
    }

    private PostInteraction saveInteraction(AppUser user, Post post, boolean liked, boolean bookmarked, Instant updatedAt) {
        PostInteraction interaction = new PostInteraction();
        interaction.setUser(user);
        interaction.setPost(post);
        interaction.setLiked(liked);
        interaction.setBookmarked(bookmarked);
        interaction.setCreatedAt(updatedAt.minusSeconds(60));
        interaction.setUpdatedAt(updatedAt);
        return postInteractionRepository.save(interaction);
    }

    private AppUser saveUser(String base) {
        String unique = base + "_" + UUID.randomUUID().toString().substring(0, 8);
        AppUser user = new AppUser();
        user.setEmail(unique + "@example.com");
        user.setPassword("encoded");
        user.setUsername(unique);
        return appUserRepository.save(user);
    }

    private Platform savePlatform(String base) {
        Platform platform = new Platform();
        platform.setName(base + "_" + UUID.randomUUID().toString().substring(0, 8));
        platform.setActive(true);
        return platformRepository.save(platform);
    }

    private Category saveCategory(String base) {
        Category category = new Category();
        category.setName(base + "_" + UUID.randomUUID().toString().substring(0, 8));
        return categoryRepository.save(category);
    }

    private Post savePost(AppUser author, Platform platform, Category category, String suffix) {
        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setTitle("정렬테스트-" + suffix);
        post.setPrompt("prompt-" + suffix);
        post.setAiResponse("response-" + suffix);
        post.setTags("tag");
        return postRepository.save(post);
    }
}
