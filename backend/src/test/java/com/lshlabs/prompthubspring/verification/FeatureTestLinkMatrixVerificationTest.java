package com.lshlabs.prompthubspring.verification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureTestLinkMatrixVerificationTest {

    @Test
    void featureTestLinkMatrix_mustExist_andReferenceExistingTestFiles() throws Exception {
        Path matrixPath = Path.of("..", "docs", "ops", "feature-test-link-matrix.md").normalize();
        assertTrue(Files.exists(matrixPath), "feature-test-link-matrix.md must exist");

        String content = Files.readString(matrixPath);
        List<String> requiredSections = List.of(
                "[AUTH]",
                "[USER]",
                "[POST-CRUD]",
                "[INTERACTION]",
                "[CORE-STATS]",
                "[RELEASE-GATE]",
                "[ENTITY-TRANSACTION]"
        );
        for (String section : requiredSections) {
            assertTrue(content.contains(section), "matrix must contain section: " + section);
        }

        Pattern testPathPattern = Pattern.compile("backend/src/test/java/[\\w/]+\\.java");
        Matcher matcher = testPathPattern.matcher(content);

        int matchedCount = 0;
        while (matcher.find()) {
            matchedCount++;
            Path testFilePath = Path.of("..", matcher.group()).normalize();
            assertTrue(Files.exists(testFilePath), "referenced test file must exist: " + matcher.group());
        }

        assertTrue(matchedCount >= 10, "matrix must reference enough test files");
    }
}
