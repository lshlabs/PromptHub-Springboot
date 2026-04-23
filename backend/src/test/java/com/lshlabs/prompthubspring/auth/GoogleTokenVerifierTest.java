package com.lshlabs.prompthubspring.auth;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
@Tag("unit")
class GoogleTokenVerifierTest {

    private MockRestServiceServer server;
    private GoogleTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://oauth2.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new GoogleTokenVerifier("expected-client-id", builder.build());
    }

    @Test
    void verify_returnsPayload_whenClaimsAreValid() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=valid-id-token"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                {"aud":"expected-client-id","iss":"https://accounts.google.com","sub":"google-sub-1","email":"user@example.com","email_verified":true,"name":"Prompt Hub"}
                                """,
                        MediaType.APPLICATION_JSON));

        GoogleTokenVerifier.GoogleUserPayload payload = verifier.verify("valid-id-token");

        assertEquals("google-sub-1", payload.sub());
        assertEquals("user@example.com", payload.email());
        assertEquals("Prompt Hub", payload.name());
        server.verify();
    }

    @Test
    void verify_throwsBadRequest_whenAudDoesNotMatch() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=invalid-aud-token"))
                .andRespond(withSuccess(
                        """
                                {"aud":"another-client-id","iss":"https://accounts.google.com","sub":"google-sub-1","email":"user@example.com","email_verified":true,"name":"Prompt Hub"}
                                """,
                        MediaType.APPLICATION_JSON));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("invalid-aud-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰 aud가 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenIssIsInvalid() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=invalid-iss-token"))
                .andRespond(withSuccess(
                        """
                                {"aud":"expected-client-id","iss":"https://malicious.example.com","sub":"google-sub-1","email":"user@example.com","email_verified":true,"name":"Prompt Hub"}
                                """,
                        MediaType.APPLICATION_JSON));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("invalid-iss-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰 iss가 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenSubIsMissing() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=missing-sub-token"))
                .andRespond(withSuccess(
                        """
                                {"aud":"expected-client-id","iss":"https://accounts.google.com","email":"user@example.com","email_verified":true,"name":"Prompt Hub"}
                                """,
                        MediaType.APPLICATION_JSON));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("missing-sub-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰의 sub 클레임이 없습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenEmailVerifiedMissing() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=missing-email-verified"))
                .andRespond(withSuccess(
                        """
                                {"aud":"expected-client-id","iss":"https://accounts.google.com","sub":"google-sub-1","email":"user@example.com","name":"Prompt Hub"}
                                """,
                        MediaType.APPLICATION_JSON));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("missing-email-verified"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰의 email_verified 검증에 실패했습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenEmailVerifiedFalse() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=email-not-verified"))
                .andRespond(withSuccess(
                        """
                                {"aud":"expected-client-id","iss":"https://accounts.google.com","sub":"google-sub-1","email":"user@example.com","email_verified":"false","name":"Prompt Hub"}
                                """,
                        MediaType.APPLICATION_JSON));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("email-not-verified"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Google 토큰의 email_verified 검증에 실패했습니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenTokenIsExpiredByExpClaim() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=expired-token"))
                .andRespond(withSuccess(
                        """
                                {"aud":"expected-client-id","iss":"https://accounts.google.com","sub":"google-sub-1","email":"user@example.com","email_verified":true,"exp":"1"}
                                """,
                        MediaType.APPLICATION_JSON));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("expired-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("만료된 Google 토큰입니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadRequest_whenProviderRejectsTokenLikeSignatureMismatch() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=signature-mismatch-token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":"invalid_token","error_description":"Invalid Value"}
                                """));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("signature-mismatch-token"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("유효하지 않은 Google 토큰입니다.", exception.getMessage());
    }

    @Test
    void verify_throwsBadGateway_whenProviderServerFails() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-upstream-down"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":"server_error"}
                                """));

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("google-upstream-down"));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("Google 토큰 검증 요청에 실패했습니다.", exception.getMessage());
    }
}
