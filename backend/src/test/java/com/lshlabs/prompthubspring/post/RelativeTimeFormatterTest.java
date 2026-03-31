package com.lshlabs.prompthubspring.post;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelativeTimeFormatterTest {

    private static final Instant NOW = Instant.parse("2026-03-27T12:00:00Z");

    @Test
    void format_matchesLegacyBoundaryRules() {
        assertEquals("방금 전", RelativeTimeFormatter.format(NOW.minusSeconds(59), NOW));
        assertEquals("1분 전", RelativeTimeFormatter.format(NOW.minusSeconds(60), NOW));
        assertEquals("59분 전", RelativeTimeFormatter.format(NOW.minusSeconds(59 * 60), NOW));
        assertEquals("1시간 전", RelativeTimeFormatter.format(NOW.minusSeconds(60 * 60), NOW));
        assertEquals("23시간 전", RelativeTimeFormatter.format(NOW.minusSeconds(23 * 60 * 60), NOW));
        assertEquals("1일 전", RelativeTimeFormatter.format(NOW.minusSeconds(24 * 60 * 60), NOW));
        assertEquals("6일 전", RelativeTimeFormatter.format(NOW.minusSeconds(6L * 24 * 60 * 60), NOW));
        assertEquals("1주 전", RelativeTimeFormatter.format(NOW.minusSeconds(7L * 24 * 60 * 60), NOW));
        assertEquals("4주 전", RelativeTimeFormatter.format(NOW.minusSeconds(29L * 24 * 60 * 60), NOW));
        assertEquals("1개월 전", RelativeTimeFormatter.format(NOW.minusSeconds(30L * 24 * 60 * 60), NOW));
        assertEquals("12개월 전", RelativeTimeFormatter.format(NOW.minusSeconds(364L * 24 * 60 * 60), NOW));
        assertEquals("1년 전", RelativeTimeFormatter.format(NOW.minusSeconds(365L * 24 * 60 * 60), NOW));
    }

    @Test
    void format_returnsFallbackForNullCreatedAt() {
        assertEquals("날짜 없음", RelativeTimeFormatter.format(null, NOW));
    }
}
