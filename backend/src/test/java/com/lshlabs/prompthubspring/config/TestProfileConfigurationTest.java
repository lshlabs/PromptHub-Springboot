package com.lshlabs.prompthubspring.config;

import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class TestProfileConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    void testProfile_usesH2AndCreateDrop() {
        assertEquals("org.h2.Driver", environment.getProperty("spring.datasource.driver-class-name"));
        assertEquals("create-drop", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
    }
}
