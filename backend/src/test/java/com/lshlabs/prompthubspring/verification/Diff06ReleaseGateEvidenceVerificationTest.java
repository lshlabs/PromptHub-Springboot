package com.lshlabs.prompthubspring.verification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Diff06ReleaseGateEvidenceVerificationTest {

    @Test
    void diff06Evidence_mustContainBrowserE2eCiGateAndArtifacts() throws Exception {
        Path evidencePath = Path.of("..", "docs", "ops", "diff06-release-gate-evidence.md").normalize();
        assertTrue(Files.exists(evidencePath), "diff06-release-gate-evidence.md must exist");

        String content = Files.readString(evidencePath);
        List<String> requiredBlocks = List.of(
                "브라우저 E2E 증빙",
                "CI Smoke Gate 증빙",
                "결과 문서화",
                "비로그인 공개 경로 포함",
                "GET /api/stats/dashboard",
                "GET /api/auth/users/{username}/summary",
                "diff06-home.png",
                "diff06-trending.png",
                "release-gate-smoke.yml"
        );
        for (String block : requiredBlocks) {
            assertTrue(content.contains(block), "required evidence block missing: " + block);
        }

        Pattern screenshotPattern = Pattern.compile("docs/ops/e2e-screenshots/[\\w\\-]+\\.png");
        Matcher screenshotMatcher = screenshotPattern.matcher(content);
        int screenshotCount = 0;
        while (screenshotMatcher.find()) {
            screenshotCount++;
            Path screenshotPath = Path.of("..", screenshotMatcher.group()).normalize();
            assertTrue(Files.exists(screenshotPath), "screenshot evidence path must exist: " + screenshotMatcher.group());
        }
        assertTrue(screenshotCount >= 2, "at least two browser screenshots are required");

        Pattern workflowPattern = Pattern.compile("\\.github/workflows/[\\w\\-]+\\.yml");
        Matcher workflowMatcher = workflowPattern.matcher(content);
        int workflowCount = 0;
        while (workflowMatcher.find()) {
            workflowCount++;
            Path workflowPath = Path.of("..", workflowMatcher.group()).normalize();
            assertTrue(Files.exists(workflowPath), "workflow path must exist: " + workflowMatcher.group());
        }
        assertTrue(workflowCount >= 1, "at least one CI workflow link is required");
    }
}
