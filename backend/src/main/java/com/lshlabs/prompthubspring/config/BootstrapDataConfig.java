package com.lshlabs.prompthubspring.config;

import com.lshlabs.prompthubspring.post.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BootstrapDataConfig {

    @Bean
    CommandLineRunner bootstrapData(PlatformRepository platformRepository, CategoryRepository categoryRepository,
            AiModelRepository aiModelRepository) {
        return args -> {
            if (platformRepository.count() == 0) {
                Platform openAi = new Platform();
                openAi.setName("OpenAI");
                platformRepository.save(openAi);

                Platform anthropic = new Platform();
                anthropic.setName("Anthropic");
                platformRepository.save(anthropic);

                Platform etc = new Platform();
                etc.setName("기타");
                platformRepository.save(etc);
            }

            if (categoryRepository.count() == 0) {
                Category c1 = new Category();
                c1.setName("개발");
                categoryRepository.save(c1);
                Category c2 = new Category();
                c2.setName("생산성");
                categoryRepository.save(c2);
                Category c3 = new Category();
                c3.setName("기타");
                categoryRepository.save(c3);
            }

            if (aiModelRepository.count() == 0) {
                Platform openAi = platformRepository.findByIsActiveTrueOrderByNameAsc().stream()
                        .filter(p -> "OpenAI".equalsIgnoreCase(p.getName())).findFirst().orElse(null);
                Platform anthropic = platformRepository.findByIsActiveTrueOrderByNameAsc().stream()
                        .filter(p -> "Anthropic".equalsIgnoreCase(p.getName())).findFirst().orElse(null);
                Platform etc = platformRepository.findByIsActiveTrueOrderByNameAsc().stream()
                        .filter(p -> "기타".equals(p.getName())).findFirst().orElse(null);

                if (openAi != null) {
                    AiModel m = new AiModel();
                    m.setPlatform(openAi);
                    m.setName("GPT-4o");
                    aiModelRepository.save(m);
                }
                if (anthropic != null) {
                    AiModel m = new AiModel();
                    m.setPlatform(anthropic);
                    m.setName("Claude 3.5 Sonnet");
                    aiModelRepository.save(m);
                }
                if (etc != null) {
                    AiModel m = new AiModel();
                    m.setPlatform(etc);
                    m.setName("기타");
                    aiModelRepository.save(m);
                }
            }
        };
    }
}
