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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void googleLogin_retriesUsername_whenConcurrentDuplicateOccurs() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );

        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
        when(googleTokenVerifier.verify("google-id-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleUserPayload("google-sub-race", "race@example.com", "Google User"));
        when(userRepository.findByGoogleSub("google-sub-race")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSettingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<String> attemptedUsernames = new ArrayList<>();
        AtomicInteger saveAttempt = new AtomicInteger(0);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            attemptedUsernames.add(user.getUsername());
            if (saveAttempt.getAndIncrement() == 0) {
                throw new DataIntegrityViolationException("duplicate username");
            }
            user.setId(10L);
            return user;
        });
        when(userRepository.existsByUsername("GoogleUser")).thenReturn(true);

        AuthService.LoginResponse response = authService.loginWithGoogle("google-id-token", ua, "127.0.0.1");

        assertEquals("access-token", response.token());
        assertEquals(List.of("GoogleUser", "GoogleUser1"), attemptedUsernames);
    }

    @Test
    void register_retriesUsername_whenConcurrentDuplicateOccurs() {
        AuthService authService = new AuthService(
                userRepository,
                userSettingsRepository,
                userSessionRepository,
                authTokenRepository,
                passwordEncoder,
                jwtProvider,
                googleTokenVerifier
        );

        when(userRepository.findByEmail("race.user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("encoded-pw");
        when(jwtProvider.createAccessToken(any(), anyString())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSettingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<String> attemptedUsernames = new ArrayList<>();
        AtomicInteger saveAttempt = new AtomicInteger(0);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            attemptedUsernames.add(user.getUsername());
            if (saveAttempt.getAndIncrement() == 0) {
                throw new DataIntegrityViolationException("duplicate username");
            }
            user.setId(11L);
            return user;
        });
        when(userRepository.existsByUsername("raceuser")).thenReturn(true);

        AuthService.RegisterResponse response = authService.register("race.user@example.com", "pw", "pw");

        assertEquals("access-token", response.token());
        assertEquals(List.of("raceuser", "raceuser1"), attemptedUsernames);
    }
}
