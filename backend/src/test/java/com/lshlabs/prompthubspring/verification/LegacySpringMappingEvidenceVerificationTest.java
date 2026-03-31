package com.lshlabs.prompthubspring.verification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacySpringMappingEvidenceVerificationTest {

    @Test
    void mappingEvidence_mustExist_withRequiredFields_andValidSpringPaths() throws Exception {
        Path evidencePath = Path.of("..", "docs", "ops", "django-spring-mapping-evidence.md").normalize();
        assertTrue(Files.exists(evidencePath), "django-spring-mapping-evidence.md must exist");

        String content = Files.readString(evidencePath);
        List<String> requiredSections = List.of(
                "[MAP-AUTH]",
                "[MAP-USER]",
                "[MAP-POST-CRUD]",
                "[MAP-INTERACTION]",
                "[MAP-CORE-STATS]",
                "[MAP-RELEASE]"
        );

        for (String section : requiredSections) {
            assertTrue(content.contains(section), "section missing: " + section);
        }

        int sourcePathCount = countMatches(content, "- Django 원본 경로:");
        int springPathCount = countMatches(content, "- Spring 대응 경로:");
        int testLinkCount = countMatches(content, "- 테스트 링크:");
        int evidenceCount = countMatches(content, "- 판정 근거:");

        assertTrue(sourcePathCount >= 6, "source path blocks must exist");
        assertTrue(springPathCount >= 6, "Spring path blocks must exist");
        assertTrue(testLinkCount >= 6, "test link blocks must exist");
        assertTrue(evidenceCount >= 6, "evidence blocks must exist");

        Pattern springPathPattern = Pattern.compile("backend/src/(main|test)/java/[\\w/]+\\.java");
        Matcher matcher = springPathPattern.matcher(content);
        int matched = 0;
        while (matcher.find()) {
            matched++;
            Path springFile = Path.of("..", matcher.group()).normalize();
            assertTrue(Files.exists(springFile), "referenced spring path must exist: " + matcher.group());
        }
        assertTrue(matched >= 12, "enough spring references are required");
    }

    private int countMatches(String content, String needle) {
        int count = 0;
        int idx = 0;
        while (true) {
            int found = content.indexOf(needle, idx);
            if (found < 0) {
                return count;
            }
            count++;
            idx = found + needle.length();
        }
    }
}
