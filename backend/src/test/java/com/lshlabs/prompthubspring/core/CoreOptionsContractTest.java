package com.lshlabs.prompthubspring.core;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.post.AiModel;
import com.lshlabs.prompthubspring.post.AiModelRepository;
import com.lshlabs.prompthubspring.post.Category;
import com.lshlabs.prompthubspring.post.CategoryRepository;
import com.lshlabs.prompthubspring.post.Platform;
import com.lshlabs.prompthubspring.post.PlatformRepository;
import com.lshlabs.prompthubspring.post.PostInteractionRepository;
import com.lshlabs.prompthubspring.post.PostRepository;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import com.lshlabs.prompthubspring.user.UserSessionRepository;
import com.lshlabs.prompthubspring.user.UserSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prompthub_core_options_parity;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc(addFilters = false)
@Tag("contract")
@Tag("integration")
class CoreOptionsContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PostInteractionRepository postInteractionRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private AiModelRepository aiModelRepository;
    @Autowired
    private PlatformRepository platformRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AuthTokenRepository authTokenRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    @AfterEach
    void tearDown() {
        postInteractionRepository.deleteAll();
        postRepository.deleteAll();
        aiModelRepository.deleteAll();
        platformRepository.deleteAll();
        categoryRepository.deleteAll();
        authTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        userSettingsRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void sortAndFilterOptions_matchCompatibilityContractShape() throws Exception {
        Platform openAiPlatform = savePlatform("OpenAI_" + System.nanoTime());
        Platform anthropicPlatform = savePlatform("Anthropic_" + System.nanoTime());
        saveCategory("코딩/프로그래밍_" + System.nanoTime());
        saveCategory("기타_" + System.nanoTime());
        saveModel(openAiPlatform, "GPT-5.2");
        saveModel(anthropicPlatform, "Claude Sonnet 4.6");

        mockMvc.perform(get("/api/core/sort-options/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort_options.latest").value("최신순"))
                .andExpect(jsonPath("$.sort_options.oldest").value("오래된순"))
                .andExpect(jsonPath("$.sort_options.popular").value("인기순"))
                .andExpect(jsonPath("$.sort_options.satisfaction").value("만족도순"))
                .andExpect(jsonPath("$.sort_options.views").value("조회순"))
                .andExpect(jsonPath("$.default").value("latest"));

        MvcResult filterResult = mockMvc.perform(get("/api/core/filter-options/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("platforms")))
                .andExpect(jsonPath("$", hasKey("categories")))
                .andExpect(jsonPath("$", hasKey("models_by_platform")))
                .andExpect(jsonPath("$.platforms[0]", hasKey("id")))
                .andExpect(jsonPath("$.platforms[0]", hasKey("name")))
                .andExpect(jsonPath("$.categories[0]", hasKey("id")))
                .andExpect(jsonPath("$.categories[0]", hasKey("name")))
                .andReturn();

        Map<String, List<Map<String, Object>>> modelsByPlatform =
                JsonPath.read(filterResult.getResponse().getContentAsString(), "$.models_by_platform");
        assertTrue(modelsByPlatform.containsKey(openAiPlatform.getName()));
        assertFalse(modelsByPlatform.get(openAiPlatform.getName()).isEmpty());
        Map<String, Object> firstModel = modelsByPlatform.get(openAiPlatform.getName()).get(0);
        assertTrue(firstModel.containsKey("id"));
        assertTrue(firstModel.containsKey("name"));
        assertTrue(firstModel.containsKey("platform_id"));
        assertTrue(firstModel.containsKey("platform_name"));
    }

    private Platform savePlatform(String name) {
        Platform platform = new Platform();
        platform.setName(name);
        platform.setActive(true);
        return platformRepository.save(platform);
    }

    private Category saveCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    private void saveModel(Platform platform, String modelName) {
        AiModel model = new AiModel();
        model.setPlatform(platform);
        model.setName(modelName);
        model.setActive(true);
        model.setDeprecated(false);
        model.setSortOrder(1);
        aiModelRepository.save(model);
    }
}
