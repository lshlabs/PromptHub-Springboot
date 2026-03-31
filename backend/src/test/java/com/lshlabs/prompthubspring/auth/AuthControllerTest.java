package com.lshlabs.prompthubspring.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.common.GlobalExceptionHandler;
import com.lshlabs.prompthubspring.security.AuthSupport;
import com.lshlabs.prompthubspring.security.TokenAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthSupport authSupport;

    @MockBean
    private TokenAuthFilter tokenAuthFilter;

    @Test
    void google_returnsOk_whenAuthServiceSucceeds() throws Exception {
        when(authService.loginWithGoogle(eq("valid-id-token"), any(), any()))
                .thenReturn(new AuthService.LoginResponse(
                        "Google 로그인이 완료되었습니다.",
                        "access",
                        "refresh",
                        null,
                        null
                ));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("id_token", "valid-id-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access"))
                .andExpect(jsonPath("$.refresh").value("refresh"));
    }

    @Test
    void google_returnsBadRequest_whenIdTokenMissing() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());

        verify(authService, never()).loginWithGoogle(anyString(), anyString(), anyString());
    }

    @Test
    void google_returnsBadRequest_whenAudienceIsInvalid() throws Exception {
        when(authService.loginWithGoogle(eq("invalid-aud-token"), any(), any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Google 토큰 aud가 유효하지 않습니다."));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("id_token", "invalid-aud-token"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Google 토큰 aud가 유효하지 않습니다."));
    }

    @Test
    void refresh_returnsBadRequest_whenRefreshMissing() throws Exception {
        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }
}
