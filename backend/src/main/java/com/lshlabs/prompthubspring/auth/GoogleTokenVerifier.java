package com.lshlabs.prompthubspring.auth;

import com.lshlabs.prompthubspring.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

@Component
public class GoogleTokenVerifier {
    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private final String expectedClientId;
    private final JwtDecoder jwtDecoder;

    @Autowired
    public GoogleTokenVerifier(@Value("${app.security.google.client-id:}") String expectedClientId) {
        this(expectedClientId, NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build());
    }

    GoogleTokenVerifier(String expectedClientId, JwtDecoder jwtDecoder) {
        this.expectedClientId = expectedClientId;
        this.jwtDecoder = jwtDecoder;
    }

    public GoogleUserPayload verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "id_token이 필요합니다.");
        }

        log.debug("Google token verification started.");

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException e) {
            log.debug("Google token verification failed due to invalid JWT.", e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "유효하지 않은 Google 토큰입니다.");
        } catch (RuntimeException e) {
            log.warn("Google token verification request failed due to upstream/network issue.", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Google 토큰 검증 요청에 실패했습니다.");
        }

        Collection<String> audiences = jwt.getAudience();
        String iss = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        boolean emailVerified = bool(jwt.getClaim("email_verified"));
        Instant expiresAt = jwt.getExpiresAt();

        if (sub == null || sub.isBlank()) {
            log.debug("Google token verification failed: missing sub.");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google 토큰의 sub 클레임이 없습니다.");
        }
        if (email == null || email.isBlank()) {
            log.debug("Google token verification failed: missing email.");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google 토큰의 email 클레임이 없습니다.");
        }
        if (!emailVerified) {
            log.debug("Google token verification failed: email_verified is false.");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google 토큰의 email_verified 검증에 실패했습니다.");
        }
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            log.debug("Google token verification failed: token expired. exp={}", expiresAt);
            throw new ApiException(HttpStatus.BAD_REQUEST, "만료된 Google 토큰입니다.");
        }
        if (expectedClientId != null && !expectedClientId.isBlank()
                && (audiences == null || audiences.stream().noneMatch(expectedClientId::equals))) {
            log.debug("Google token verification failed: aud mismatch.");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google 토큰 aud가 유효하지 않습니다.");
        }
        // Google 토큰은 iss가 두 형태로 올 수 있어서 공식 값 두 개만 허용한다.
        if (!Objects.equals("https://accounts.google.com", iss) && !Objects.equals("accounts.google.com", iss)) {
            log.debug("Google token verification failed: iss mismatch.");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google 토큰 iss가 유효하지 않습니다.");
        }

        log.debug("Google token verification succeeded for subject={}", sub);
        return new GoogleUserPayload(sub, email, name == null || name.isBlank() ? email.split("@")[0] : name);
    }

    private boolean bool(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        // 테스트 더블이나 일부 파서가 문자열/숫자로 넘겨도 email_verified 의미는 유지한다.
        String normalized = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(normalized) || "1".equals(normalized);
    }

    public record GoogleUserPayload(String sub, String email, String name) {
    }
}
