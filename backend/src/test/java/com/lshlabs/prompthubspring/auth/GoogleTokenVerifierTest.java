package com.lshlabs.prompthubspring.auth;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class GoogleTokenVerifierTest {

    private JwtDecoder jwtDecoder;
    private GoogleTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        verifier = new GoogleTokenVerifier("expected-client-id", jwtDecoder);
    }

    @Test
    void verify_returnsPayload_whenClaimsAreValid() {
        when(jwtDecoder.decode("valid-id-token")).thenReturn(jwt(
                "valid-id-token",
                Map.of(
                        "sub", "google-sub-1",
                        "email", "user@example.com",
                        "name", "Prompt Hub",
                        "email_verified", true,
                        "iss", "https://accounts.google.com",
                        "aud", List.of("expected-client-id")
                ),
                Instant.now().plusSeconds(600)
        ));

        GoogleTokenVerifier.GoogleUserPayload payload = verifier.verify("valid-id-token");

        assertEquals("google-sub-1", payload.sub());
        assertEquals("user@example.com", payload.email());
        assertEquals("Prompt Hub", payload.name());
    }

    @Test
    void verify_throwsBadRequest_whenAudDoesNotMatch() {
        when(jwtDecoder.decode("invalid-aud-token")).thenReturn(jwt(
                "invalid-aud-token",
                Map.of(
                        "sub", "google-sub-1",
                        "email", "user@example.com",
                        "name", "Prompt Hub",
                        "email_verified", true,
                        "iss", "https://accounts.google.com",
                        "aud", List.of("another-client-id")
                ),
                Instant.now().plusSeconds(600)
        ));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("invalid-aud-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰 aud가 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenIssIsInvalid() {
        when(jwtDecoder.decode("invalid-iss-token")).thenReturn(jwt(
                "invalid-iss-token",
                Map.of(
                        "sub", "google-sub-1",
                        "email", "user@example.com",
                        "name", "Prompt Hub",
                        "email_verified", true,
                        "iss", "https://malicious.example.com",
                        "aud", List.of("expected-client-id")
                ),
                Instant.now().plusSeconds(600)
        ));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("invalid-iss-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰 iss가 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenSubIsMissing() {
        when(jwtDecoder.decode("missing-sub-token")).thenReturn(jwt(
                "missing-sub-token",
                Map.of(
                        "email", "user@example.com",
                        "name", "Prompt Hub",
                        "email_verified", true,
                        "iss", "https://accounts.google.com",
                        "aud", List.of("expected-client-id")
                ),
                Instant.now().plusSeconds(600)
        ));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("missing-sub-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰의 sub 클레임이 없습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenEmailVerifiedMissing() {
        when(jwtDecoder.decode("missing-email-verified")).thenReturn(jwt(
                "missing-email-verified",
                Map.of(
                        "sub", "google-sub-1",
                        "email", "user@example.com",
                        "name", "Prompt Hub",
                        "iss", "https://accounts.google.com",
                        "aud", List.of("expected-client-id")
                ),
                Instant.now().plusSeconds(600)
        ));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("missing-email-verified"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰의 email_verified 검증에 실패했습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenEmailVerifiedFalse() {
        when(jwtDecoder.decode("email-not-verified")).thenReturn(jwt(
                "email-not-verified",
                Map.of(
                        "sub", "google-sub-1",
                        "email", "user@example.com",
                        "name", "Prompt Hub",
                        "email_verified", "false",
                        "iss", "https://accounts.google.com",
                        "aud", List.of("expected-client-id")
                ),
                Instant.now().plusSeconds(600)
        ));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("email-not-verified"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰의 email_verified 검증에 실패했습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenTokenIsExpiredByExpClaim() {
        when(jwtDecoder.decode("expired-token")).thenReturn(jwt(
                "expired-token",
                Map.of(
                        "sub", "google-sub-1",
                        "email", "user@example.com",
                        "name", "Prompt Hub",
                        "email_verified", true,
                        "iss", "https://accounts.google.com",
                        "aud", List.of("expected-client-id")
                ),
                Instant.now().minusSeconds(5)
        ));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("expired-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("만료된 Google 토큰입니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenDecoderRejectsTokenLikeSignatureMismatch() {
        when(jwtDecoder.decode("signature-mismatch-token"))
                .thenThrow(new JwtException("Invalid JWT signature"));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("signature-mismatch-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("유효하지 않은 Google 토큰입니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadGateway_whenDecoderFailsDueToUpstreamOrNetworkIssue() {
        when(jwtDecoder.decode("google-upstream-down"))
                .thenThrow(new RuntimeException("Failed to retrieve remote JWK set"));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("google-upstream-down"));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("Google 토큰 검증 요청에 실패했습니다.", exception.getMessage());
    }

    private Jwt jwt(String tokenValue, Map<String, Object> claims, Instant expiresAt) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(expiresAt)
                .claims(existing -> existing.putAll(claims))
                .build();
    }
}
