package com.lshlabs.prompthubspring.verification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceRuleParityMatrixVerificationTest {

    @Test
    void serviceRuleMatrix_mustContainRequiredSections_andValidPaths() throws Exception {
        Path matrixPath = Path.of("..", "docs", "ops", "service-rule-parity-matrix.md").normalize();
        assertTrue(Files.exists(matrixPath), "service-rule-parity-matrix.md must exist");

        String content = Files.readString(matrixPath);
        List<String> requiredSections = List.of(
                "[SR-USER]",
                "[SR-POST]",
                "[SR-INTERACTION]",
                "[SR-CORE-STATS]"
        );
        for (String section : requiredSections) {
            assertTrue(content.contains(section), "section missing: " + section);
        }

        assertTrue(countMatches(content, "- Django 원본 경로:") >= 4, "django source blocks required");
        assertTrue(countMatches(content, "- Spring 대응 경로:") >= 4, "spring path blocks required");
        assertTrue(countMatches(content, "- 핵심 규칙:") >= 4, "rule blocks required");
        assertTrue(countMatches(content, "- 테스트 링크:") >= 4, "test link blocks required");
        assertTrue(countMatches(content, "- 판정 근거:") >= 4, "evidence blocks required");

        Pattern springMainPattern = Pattern.compile("backend/src/main/java/[\\w/]+\\.java");
        Matcher mainMatcher = springMainPattern.matcher(content);
        int mainCount = 0;
        while (mainMatcher.find()) {
            mainCount++;
            Path path = Path.of("..", mainMatcher.group()).normalize();
            assertTrue(Files.exists(path), "spring main path missing: " + mainMatcher.group());
        }
        assertTrue(mainCount >= 6, "enough spring main paths are required");

        Pattern testPathPattern = Pattern.compile("backend/src/test/java/[\\w/]+\\.java");
        Matcher testMatcher = testPathPattern.matcher(content);
        int testCount = 0;
        while (testMatcher.find()) {
            testCount++;
            Path path = Path.of("..", testMatcher.group()).normalize();
            assertTrue(Files.exists(path), "spring test path missing: " + testMatcher.group());
        }
        assertTrue(testCount >= 8, "enough test links are required");
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
