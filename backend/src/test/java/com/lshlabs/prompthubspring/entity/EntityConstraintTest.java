package com.lshlabs.prompthubspring.entity;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.core.TrendingCategoryEntity;
import com.lshlabs.prompthubspring.core.TrendingCategoryRepository;
import com.lshlabs.prompthubspring.core.TrendingRankingEntity;
import com.lshlabs.prompthubspring.core.TrendingRankingRepository;
import com.lshlabs.prompthubspring.post.AiModel;
import com.lshlabs.prompthubspring.post.AiModelRepository;
import com.lshlabs.prompthubspring.post.Platform;
import com.lshlabs.prompthubspring.post.PlatformRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Tag("contract")
class EntityConstraintTest {

    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private AiModelRepository aiModelRepository;
    @Autowired
    private TrendingCategoryRepository trendingCategoryRepository;
    @Autowired
    private TrendingRankingRepository trendingRankingRepository;

    @BeforeEach
    void cleanUp() {
        trendingRankingRepository.deleteAll();
        trendingCategoryRepository.deleteAll();
        aiModelRepository.deleteAll();
        platformRepository.deleteAll();
    }

    @Test
    void aiModel_throws_whenDuplicatedSlugWithinSamePlatform() {
        Platform platform = new Platform();
        platform.setName("OpenAI");
        platform.setActive(true);
        platform = platformRepository.save(platform);

        AiModel first = new AiModel();
        first.setPlatform(platform);
        first.setName("GPT-5.2");
        first.setSlug("gpt-5");
        first.setActive(true);
        aiModelRepository.saveAndFlush(first);

        AiModel duplicatedSlug = new AiModel();
        duplicatedSlug.setPlatform(platform);
        duplicatedSlug.setName("GPT-5.3");
        duplicatedSlug.setSlug("gpt-5");
        duplicatedSlug.setActive(true);

        assertThrows(DataIntegrityViolationException.class, () -> aiModelRepository.saveAndFlush(duplicatedSlug));
    }

    @Test
    void trendingRanking_throws_whenDuplicatedRankInSameCategory() {
        TrendingCategoryEntity category = new TrendingCategoryEntity();
        category.setName("reasoning");
        category.setTitle("Reasoning");
        category.setSubtitle("Best in reasoning");
        category.setIconName("brain");
        category.setOrderNum(1);
        category.setActive(true);
        category = trendingCategoryRepository.saveAndFlush(category);

        TrendingRankingEntity first = new TrendingRankingEntity();
        first.setCategory(category);
        first.setRank(1);
        first.setName("GPT-5.2");
        first.setScore("92.4");
        first.setProvider("openai");
        first.setUseExactMatching(false);
        first.setActive(true);
        trendingRankingRepository.saveAndFlush(first);

        TrendingRankingEntity duplicatedRank = new TrendingRankingEntity();
        duplicatedRank.setCategory(category);
        duplicatedRank.setRank(1);
        duplicatedRank.setName("Gemini 3 Pro");
        duplicatedRank.setScore("91.9");
        duplicatedRank.setProvider("google");
        duplicatedRank.setUseExactMatching(false);
        duplicatedRank.setActive(true);

        assertThrows(DataIntegrityViolationException.class, () -> trendingRankingRepository.saveAndFlush(duplicatedRank));
    }
}
