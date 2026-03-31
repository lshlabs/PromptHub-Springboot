package com.lshlabs.prompthubspring.post;

import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import com.lshlabs.prompthubspring.user.UserSessionRepository;
import com.lshlabs.prompthubspring.user.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class PostRepositoryConstraintTest {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private PostInteractionRepository postInteractionRepository;
    @Autowired
    private AiModelRepository aiModelRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @BeforeEach
    void cleanUp() {
        postInteractionRepository.deleteAll();
        postRepository.deleteAll();
        aiModelRepository.deleteAll();
        platformRepository.deleteAll();
        categoryRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void save_throwsDataIntegrityViolation_whenSatisfactionOutOfRange() {
        AppUser author = new AppUser();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        author.setUsername("author_" + suffix);
        author.setEmail("author_" + suffix + "@example.com");
        author.setPassword("encoded");
        author = appUserRepository.save(author);

        Platform platform = new Platform();
        platform.setName("OpenAI_" + suffix);
        platform.setActive(true);
        platform = platformRepository.save(platform);

        Category category = new Category();
        category.setName("일반_" + suffix);
        category = categoryRepository.save(category);

        Post post = new Post();
        post.setAuthor(author);
        post.setPlatform(platform);
        post.setCategory(category);
        post.setTitle("DB 제약 테스트용 게시글 제목");
        post.setPrompt("DB 제약 테스트용으로 충분히 긴 prompt 내용입니다.");
        post.setAiResponse("DB 제약 테스트용으로 충분히 긴 ai_response 내용입니다.");
        post.setSatisfaction(new BigDecimal("6.0"));

        assertThrows(DataIntegrityViolationException.class, () -> postRepository.saveAndFlush(post));
    }
}
