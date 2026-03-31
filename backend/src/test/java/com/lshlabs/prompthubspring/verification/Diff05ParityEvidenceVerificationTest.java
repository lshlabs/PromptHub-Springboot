package com.lshlabs.prompthubspring.verification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Diff05ParityEvidenceVerificationTest {

    @Test
    void diff05Evidence_mustContainQueryCacheStatsSections_andValidTestLinks() throws Exception {
        Path evidencePath = Path.of("..", "docs", "ops", "diff05-core-stats-parity-evidence.md").normalize();
        assertTrue(Files.exists(evidencePath), "diff05-core-stats-parity-evidence.md must exist");

        String content = Files.readString(evidencePath);
        List<String> requiredBlocks = List.of(
                "질의 조합별 결과 비교",
                "캐시 계약 점검",
                "통계 계약 점검",
                "search_type",
                "from_cache=false",
                "total_posts"
        );
        for (String block : requiredBlocks) {
            assertTrue(content.contains(block), "required evidence block missing: " + block);
        }

        Pattern testPathPattern = Pattern.compile("backend/src/test/java/[\\w/]+\\.java");
        Matcher matcher = testPathPattern.matcher(content);
        int testPathCount = 0;
        while (matcher.find()) {
            testPathCount++;
            Path testPath = Path.of("..", matcher.group()).normalize();
            assertTrue(Files.exists(testPath), "test path must exist: " + matcher.group());
        }
        assertTrue(testPathCount >= 4, "enough test links are required");
    }
}
