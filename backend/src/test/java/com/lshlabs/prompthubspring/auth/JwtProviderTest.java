package com.lshlabs.prompthubspring.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "prompthub-local-secret-change-me-prompthub-local-secret",
            3_600_000L,
            1_209_600_000L
    );

    @Test
    void parseRefreshToken_returnsUserId_whenTokenTypeIsRefresh() {
        String refresh = jwtProvider.createRefreshToken(77L);

        Long userId = jwtProvider.parseRefreshToken(refresh);

        assertEquals(77L, userId);
    }

    @Test
    void parseRefreshToken_throws_whenTokenTypeIsAccess() {
        String access = jwtProvider.createAccessToken(11L, "user@example.com");

        assertThrows(IllegalArgumentException.class, () -> jwtProvider.parseRefreshToken(access));
    }

    @Test
    void isValidRefreshToken_returnsFalse_whenMalformedOrWrongType() {
        String access = jwtProvider.createAccessToken(11L, "user@example.com");

        assertFalse(jwtProvider.isValidRefreshToken("not-a-jwt"));
        assertFalse(jwtProvider.isValidRefreshToken(access));
        assertTrue(jwtProvider.isValidRefreshToken(jwtProvider.createRefreshToken(99L)));
    }
}
