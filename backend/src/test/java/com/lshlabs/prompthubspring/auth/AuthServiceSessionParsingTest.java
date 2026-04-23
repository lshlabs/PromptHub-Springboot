package com.lshlabs.prompthubspring.auth;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.user.AppUser;
import com.lshlabs.prompthubspring.user.AppUserRepository;
import com.lshlabs.prompthubspring.user.UserSession;
import com.lshlabs.prompthubspring.user.UserSessionRepository;
import com.lshlabs.prompthubspring.user.UserSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuthServiceSessionParsingTest {

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
    void login_parsesDeviceBrowserOsFromUserAgent() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );

        AppUser user = new AppUser();
        user.setEmail("user@example.com");
        user.setPassword("encoded-password");
        user.setUsername("user1");

        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.login("user@example.com", "plain-password", ua, "127.0.0.1");

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        UserSession saved = sessionCaptor.getValue();

        assertEquals(ua, saved.getUserAgent());
        assertEquals("Windows PC", saved.getDevice());
        assertNotNull(saved.getBrowser());
        assertNotNull(saved.getOs());
        assertTrue(saved.getBrowser().startsWith("Chrome"));
        assertTrue(saved.getOs().startsWith("Windows"));
    }

    @Test
    void login_withoutUserAgent_keepsDeviceBrowserOsNull() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );

        AppUser user = new AppUser();
        user.setEmail("user2@example.com");
        user.setPassword("encoded-password");
        user.setUsername("user2");

        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.login("user2@example.com", "plain-password", null, "127.0.0.1");

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        UserSession saved = sessionCaptor.getValue();

        assertNull(saved.getDevice());
        assertNull(saved.getBrowser());
        assertNull(saved.getOs());
    }

    @Test
    void googleLogin_parsesDeviceBrowserOsFromUserAgent() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );

        AppUser user = new AppUser();
        user.setEmail("google@example.com");
        user.setPassword("encoded-password");
        user.setUsername("googleuser");
        user.setGoogleSub("google-sub-1");

        String ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

        when(googleTokenVerifier.verify("google-id-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleUserPayload("google-sub-1", "google@example.com", "Google User"));
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.loginWithGoogle("google-id-token", ua, "127.0.0.1");

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        UserSession saved = sessionCaptor.getValue();

        assertEquals("Pixel", saved.getDevice());
        assertTrue(saved.getBrowser().startsWith("Chrome"));
        assertTrue(saved.getOs().startsWith("Android"));
    }
}
