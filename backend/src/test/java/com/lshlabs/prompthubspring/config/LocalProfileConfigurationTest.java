package com.lshlabs.prompthubspring.config;

import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:prompthub_local_profile;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        }
)
@ActiveProfiles("local")
@Tag("integration")
class LocalProfileConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    void localProfile_keepsUpdateDdlPolicy() {
        assertEquals("local", environment.getActiveProfiles()[0]);
        assertEquals("update", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
    }
}
