package com.lshlabs.prompthubspring.core;

import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_cache_parity;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Tag("contract")
@Tag("integration")
class CoreTrendingCacheTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private TrendingCategoryRepository trendingCategoryRepo;
    @Autowired
    private TrendingRankingRepository trendingRankingRepo;

    @BeforeEach
    void setUp() {
        clearTrendingCache();

        TrendingCategoryEntity category = new TrendingCategoryEntity();
        category.setName("cache_parity_" + UUID.randomUUID().toString().substring(0, 8));
        category.setTitle("캐시 파리티 테스트");
        category.setSubtitle("cache parity");
        category.setIconName("TestIcon");
        category.setOrderNum(1);
        category.setActive(true);
        category = trendingCategoryRepo.save(category);

        TrendingRankingEntity ranking = new TrendingRankingEntity();
        ranking.setCategory(category);
        ranking.setRank(1);
        ranking.setName("CacheModel");
        ranking.setScore("99.9");
        ranking.setProvider("TestProvider");
        ranking.setActive(true);
        trendingRankingRepo.save(ranking);
    }

    @AfterEach
    void tearDown() {
        clearTrendingCache();
        trendingRankingRepo.deleteAll();
        trendingCategoryRepo.deleteAll();
    }

    @Test
    void categoryRankings_returnsFalseThenTrueForCacheHitState() throws Exception {
        mockMvc.perform(get("/api/core/trending/category-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.from_cache").value(false));

        mockMvc.perform(get("/api/core/trending/category-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.from_cache").value(true));
    }

    @Test
    void refreshCache_deletesCacheAndNextReadIsMiss() throws Exception {
        mockMvc.perform(get("/api/core/trending/category-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from_cache").value(false));

        mockMvc.perform(get("/api/core/trending/category-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from_cache").value(true));

        mockMvc.perform(post("/api/core/trending/refresh-cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("트렌딩 캐시가 성공적으로 삭제되었습니다."));

        mockMvc.perform(get("/api/core/trending/category-rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from_cache").value(false));
    }

    private void clearTrendingCache() {
        Cache cache = cacheManager.getCache("trending");
        if (cache != null) {
            cache.clear();
        }
    }
}
