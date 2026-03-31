package com.lshlabs.prompthubspring.auth;

import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import com.lshlabs.prompthubspring.user.UserSessionRepository;
import com.lshlabs.prompthubspring.user.UserSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private UserSettingsRepository userSettingsRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private AuthTokenRepository authTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Test
    void refresh_rotatesTokensAndRevokesOldRefresh() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );
        String oldRefresh = "old-refresh-token";
        AuthToken stored = new AuthToken();
        stored.setToken(oldRefresh);
        stored.setTokenType(AuthTokenType.REFRESH);
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        AppUser storedUser = mock(AppUser.class);
        when(storedUser.getId()).thenReturn(1L);
        stored.setUser(storedUser);

        AppUser user = new AppUser();
        user.setEmail("user@example.com");
        user.setUsername("user1");

        when(authTokenRepository.findByTokenAndTokenTypeAndRevokedAtIsNull(oldRefresh, AuthTokenType.REFRESH))
                .thenReturn(Optional.of(stored));
        when(jwtProvider.isValidRefreshToken(oldRefresh)).thenReturn(true);
        when(jwtProvider.parseRefreshToken(oldRefresh)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("new-access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("new-refresh-token");
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService.TokenPair result = authService.refresh(oldRefresh);

        assertEquals("new-access-token", result.access());
        assertEquals("new-refresh-token", result.refresh());
        verify(authTokenRepository).revokeByToken(eq(oldRefresh), any(Instant.class));
        verify(authTokenRepository, times(2)).save(any(AuthToken.class));
    }

    @Test
    void refresh_throwsUnauthorized_whenRefreshExpired() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );
        String oldRefresh = "expired-refresh-token";
        AuthToken stored = new AuthToken();
        stored.setToken(oldRefresh);
        stored.setTokenType(AuthTokenType.REFRESH);
        stored.setExpiresAt(Instant.now().minusSeconds(1));

        when(authTokenRepository.findByTokenAndTokenTypeAndRevokedAtIsNull(oldRefresh, AuthTokenType.REFRESH))
                .thenReturn(Optional.of(stored));
        when(jwtProvider.isValidRefreshToken(oldRefresh)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> authService.refresh(oldRefresh));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(jwtProvider, never()).parseRefreshToken(anyString());
    }

    @Test
    void refresh_throwsBadRequest_whenRefreshTokenMissing() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );

        ApiException exception = assertThrows(ApiException.class, () -> authService.refresh(" "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(authTokenRepository, never()).findByTokenAndTokenTypeAndRevokedAtIsNull(anyString(), any());
    }

    @Test
    void refresh_throwsUnauthorized_whenJwtPayloadInvalid() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );
        String refreshToken = "invalid-refresh-token";
        when(jwtProvider.isValidRefreshToken(refreshToken)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> authService.refresh(refreshToken));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(authTokenRepository, never()).findByTokenAndTokenTypeAndRevokedAtIsNull(anyString(), any());
    }

    @Test
    void refresh_throwsUnauthorized_whenStoredTokenUserDoesNotMatchJwtSubject() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );
        String refreshToken = "valid-refresh-token";
        AuthToken stored = new AuthToken();
        stored.setToken(refreshToken);
        stored.setTokenType(AuthTokenType.REFRESH);
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        AppUser storedUser = mock(AppUser.class);
        when(storedUser.getId()).thenReturn(100L);
        stored.setUser(storedUser);

        when(jwtProvider.isValidRefreshToken(refreshToken)).thenReturn(true);
        when(authTokenRepository.findByTokenAndTokenTypeAndRevokedAtIsNull(refreshToken, AuthTokenType.REFRESH))
                .thenReturn(Optional.of(stored));
        when(jwtProvider.parseRefreshToken(refreshToken)).thenReturn(200L);

        ApiException exception = assertThrows(ApiException.class, () -> authService.refresh(refreshToken));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getMessage().contains("유효하지 않은 refresh token"));
    }
}
