package com.lshlabs.prompthubspring.verification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiContractSnapshotsVerificationTest {

    @Test
    void apiContractSnapshots_mustContainRequiredSectionsAndValidSpringPaths() throws Exception {
        Path snapshotPath = Path.of("..", "docs", "ops", "api-contract-snapshots.md").normalize();
        assertTrue(Files.exists(snapshotPath), "api-contract-snapshots.md must exist");

        String content = Files.readString(snapshotPath);
        List<String> requiredSections = List.of(
                "[SNAP-AUTH]",
                "[SNAP-USER]",
                "[SNAP-POST-LIST]",
                "[SNAP-INTERACTION]",
                "[SNAP-CORE]",
                "[SNAP-STATS]"
        );
        for (String section : requiredSections) {
            assertTrue(content.contains(section), "section missing: " + section);
        }

        assertTrue(content.contains("\"status\""), "snapshot must include status field evidence");
        assertTrue(content.contains("\"data\""), "snapshot must include data field evidence");
        assertTrue(content.contains("\"pagination\""), "snapshot must include pagination field evidence");
        assertTrue(content.contains("\"message\""), "snapshot must include message field evidence");

        int endpointCount = countMatches(content, "- Endpoint:");
        assertTrue(endpointCount >= 6, "each snapshot section must include endpoint");

        Pattern springPathPattern = Pattern.compile("backend/src/main/java/[\\w/]+\\.java");
        Matcher matcher = springPathPattern.matcher(content);
        int pathCount = 0;
        while (matcher.find()) {
            pathCount++;
            Path springFile = Path.of("..", matcher.group()).normalize();
            assertTrue(Files.exists(springFile), "referenced spring file must exist: " + matcher.group());
        }
        assertTrue(pathCount >= 10, "enough spring file references are required");
    }

    private int countMatches(String content, String needle) {
        int count = 0;
        int index = 0;
        while (true) {
            int found = content.indexOf(needle, index);
            if (found < 0) {
                return count;
            }
            count++;
            index = found + needle.length();
        }
    }
}
