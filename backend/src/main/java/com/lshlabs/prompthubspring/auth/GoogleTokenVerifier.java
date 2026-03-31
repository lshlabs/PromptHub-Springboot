package com.lshlabs.prompthubspring.auth;

import com.lshlabs.prompthubspring.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Component
public class GoogleTokenVerifier {
    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    private final String expectedClientId;
    private final RestClient restClient;

    @Autowired
    public GoogleTokenVerifier(@Value("${app.security.google.client-id:}") String expectedClientId,
            RestClient.Builder restClientBuilder) {
        this(expectedClientId, restClientBuilder.baseUrl("https://oauth2.googleapis.com").build());
    }

    GoogleTokenVerifier(String expectedClientId, RestClient restClient) {
        this.expectedClientId = expectedClientId;
        this.restClient = restClient;
    }

    public GoogleUserPayload verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "id_token이 필요합니다.");
        }

        log.debug("Google token verification started.");

        Map<String, Object> payload;
        try {
            payload = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo").queryParam("id_token", idToken).build()).retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.warn("Google token verification rejected by provider. status={}, response={}",
                    e.getRawStatusCode(), abbreviate(e.getResponseBodyAsString()));
            if (e.getStatusCode().is4xxClientError()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "유효하지 않은 Google 토큰입니다.");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Google 토큰 검증 요청에 실패했습니다.");
        } catch (Exception e) {
            log.warn("Google token verification request failed due to upstream/network issue.", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Google 토큰 검증 요청에 실패했습니다.");
        }

        String aud = str(payload.get("aud"));
        String iss = str(payload.get("iss"));
        String sub = str(payload.get("sub"));
        String email = str(payload.get("email"));
        String name = str(payload.get("name"));
        boolean emailVerified = bool(payload.get("email_verified"));
        long exp = longValue(payload.get("exp"));

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
        if (exp > 0 && exp <= Instant.now().getEpochSecond()) {
            log.debug("Google token verification failed: token expired. exp={}", exp);
            throw new ApiException(HttpStatus.BAD_REQUEST, "만료된 Google 토큰입니다.");
        }
        if (expectedClientId != null && !expectedClientId.isBlank() && !expectedClientId.equals(aud)) {
            log.debug("Google token verification failed: aud mismatch.");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google 토큰 aud가 유효하지 않습니다.");
        }
        if (!("https://accounts.google.com".equals(iss) || "accounts.google.com".equals(iss))) {
            log.debug("Google token verification failed: iss mismatch.");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google 토큰 iss가 유효하지 않습니다.");
        }

        log.debug("Google token verification succeeded for subject={}", sub);
        return new GoogleUserPayload(sub, email, name == null || name.isBlank() ? email.split("@")[0] : name);
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean bool(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String normalized = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(normalized) || "1".equals(normalized);
    }

    private long longValue(Object value) {
        if (value == null) {
            return -1L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private String abbreviate(String raw) {
        if (raw == null) {
            return "";
        }
        String compact = raw.replaceAll("\\s+", " ").trim();
        return compact.length() <= 200 ? compact : compact.substring(0, 200) + "...";
    }

    public record GoogleUserPayload(String sub, String email, String name) {
    }
}
