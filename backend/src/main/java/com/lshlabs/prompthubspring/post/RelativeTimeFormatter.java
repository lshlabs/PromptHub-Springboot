package com.lshlabs.prompthubspring.post;

import java.time.Duration;
import java.time.Instant;

public final class RelativeTimeFormatter {

    private RelativeTimeFormatter() {
    }

    public static String format(Instant createdAt) {
        return format(createdAt, Instant.now());
    }

    static String format(Instant createdAt, Instant now) {
        if (createdAt == null || now == null) {
            return "날짜 없음";
        }

        long diffInSeconds = Duration.between(createdAt, now).getSeconds();
        if (diffInSeconds < 0) {
            diffInSeconds = 0;
        }

        if (diffInSeconds < 60) {
            return "방금 전";
        }
        if (diffInSeconds < 3600) {
            return (diffInSeconds / 60) + "분 전";
        }
        if (diffInSeconds < 86400) {
            return (diffInSeconds / 3600) + "시간 전";
        }
        if (diffInSeconds < 604800) {
            return (diffInSeconds / 86400) + "일 전";
        }

        long days = diffInSeconds / 86400;
        if (days < 30) {
            return (days / 7) + "주 전";
        }
        if (days < 365) {
            return (days / 30) + "개월 전";
        }
        return (days / 365) + "년 전";
    }
}
