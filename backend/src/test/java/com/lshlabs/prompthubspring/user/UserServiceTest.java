package com.lshlabs.prompthubspring.user;

import org.junit.jupiter.api.Tag;

import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.common.CloudinaryService;
import com.lshlabs.prompthubspring.auth.AuthService;
import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserServiceTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private UserSettingsRepository userSettingsRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private AuthTokenRepository authTokenRepository;
    @Mock
    private AuthService authService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CloudinaryService cloudinaryService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userSettingsRepository, userSessionRepository,
                authTokenRepository, authService, passwordEncoder, postRepository, cloudinaryService);
    }

    @Test
    void summary_returnsAggregatedCounts() {
        AppUser user = new AppUser();
        user.setUsername("alice");
        user.setBio("bio");
        user.setProfileImage("https://example.com/avatar.png");
        user.setAvatarColor1("#111111");
        user.setAvatarColor2("#222222");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(postRepository.countByAuthor(user)).thenReturn(3L);
        when(postRepository.sumViewCountByAuthor(user)).thenReturn(120L);
        when(postRepository.sumLikeCountByAuthor(user)).thenReturn(45L);
        when(postRepository.sumBookmarkCountByAuthor(user)).thenReturn(9L);

        UserService.UserSummaryResponse result = userService.summary("alice");

        assertEquals(3L, result.post_count());
        assertEquals(120L, result.total_views());
        assertEquals(45L, result.total_likes_received());
        assertEquals(9L, result.total_bookmarks_received());
    }

    @Test
    void endSession_throwsNotFound_whenSessionNotOwnedByCurrentUser() {
        AppUser currentUser = new AppUser();
        currentUser.setEmail("me@example.com");
        currentUser.setUsername("me");

        when(userSessionRepository.findActiveBySessionKeyAndUser("other-user-session", currentUser))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> userService.endSession(currentUser, "other-user-session"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void endSession_revokesTargetSession() {
        AppUser currentUser = new AppUser();
        currentUser.setEmail("me@example.com");
        currentUser.setUsername("me");
        UserSession session = new UserSession();
        session.setUser(currentUser);
        session.setSessionKey("session-1");

        when(userSessionRepository.findActiveBySessionKeyAndUser("session-1", currentUser))
                .thenReturn(Optional.of(session));
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(session);

        UserService.MessageResponse result = userService.endSession(currentUser, "session-1");

        assertEquals("세션이 종료되었습니다.", result.message());
        verify(userSessionRepository).save(session);
    }

    @Test
    void endOtherSessions_returnsRevokedCount() {
        AppUser currentUser = new AppUser();
        currentUser.setEmail("me@example.com");
        currentUser.setUsername("me");
        when(userSessionRepository.revokeOtherActiveSessions(any(), any(), any(Instant.class))).thenReturn(2);

        UserService.EndOtherSessionsResponse result = userService.endOtherSessions(currentUser, "current-session");

        assertEquals("다른 모든 세션을 종료했습니다.", result.message());
        assertEquals(2, result.count());
    }

    @Test
    void deleteAccount_removesUserRelatedRowsBeforeDeletingUser() {
        AppUser currentUser = new AppUser();
        currentUser.setEmail("me@example.com");
        currentUser.setUsername("me");

        UserService.MessageResponse result = userService.deleteAccount(currentUser);

        assertEquals("계정이 삭제되었습니다.", result.message());
        verify(authTokenRepository).deleteByUser(currentUser);
        verify(userSessionRepository).deleteByUser(currentUser);
        verify(userSettingsRepository).deleteByUser(currentUser);
        verify(userRepository).deleteById(currentUser.getId());
    }

    @Test
    void changePassword_rotatesTokensAndReturnsNewPair() {
        AppUser currentUser = new AppUser();
        currentUser.setEmail("me@example.com");
        currentUser.setUsername("me");
        currentUser.setPassword("encoded-old");

        when(passwordEncoder.matches("OldPass!1", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("NewPass!2")).thenReturn("encoded-new");
        when(authService.rotateTokenPair(currentUser))
                .thenReturn(new AuthService.TokenPair("new-access-token", "new-refresh-token"));

        UserService.ChangePasswordResponse result = userService.changePassword(currentUser, "OldPass!1", "NewPass!2",
                "NewPass!2");

        assertEquals("비밀번호가 성공적으로 변경되었습니다.", result.message());
        assertEquals("new-access-token", result.token());
        assertEquals("new-refresh-token", result.refresh());
        verify(userRepository).save(currentUser);
        verify(authService).rotateTokenPair(currentUser);
    }

    @Test
    void summary_masksBio_whenPublicProfileFalse() {
        AppUser user = new AppUser();
        user.setUsername("private-user");
        user.setBio("secret bio");
        user.setProfileImage("https://example.com/avatar.png");
        user.setAvatarColor1("#111111");
        user.setAvatarColor2("#222222");

        UserSettings settings = new UserSettings();
        settings.setUser(user);
        settings.setPublicProfile(false);

        when(userRepository.findByUsername("private-user")).thenReturn(Optional.of(user));
        when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(settings));
        when(postRepository.countByAuthor(user)).thenReturn(1L);
        when(postRepository.sumViewCountByAuthor(user)).thenReturn(10L);
        when(postRepository.sumLikeCountByAuthor(user)).thenReturn(5L);
        when(postRepository.sumBookmarkCountByAuthor(user)).thenReturn(2L);

        UserService.UserSummaryResponse result = userService.summary("private-user");

        assertEquals(null, result.bio());
        assertEquals(1L, result.post_count());
    }

    @Test
    void summary_exposesBio_whenSettingsMissing() {
        AppUser user = new AppUser();
        user.setUsername("default-public");
        user.setBio("public bio");
        user.setProfileImage("https://example.com/avatar.png");
        user.setAvatarColor1("#111111");
        user.setAvatarColor2("#222222");

        when(userRepository.findByUsername("default-public")).thenReturn(Optional.of(user));
        when(userSettingsRepository.findByUser(user)).thenReturn(Optional.empty());
        when(postRepository.countByAuthor(user)).thenReturn(2L);
        when(postRepository.sumViewCountByAuthor(user)).thenReturn(20L);
        when(postRepository.sumLikeCountByAuthor(user)).thenReturn(10L);
        when(postRepository.sumBookmarkCountByAuthor(user)).thenReturn(4L);

        UserService.UserSummaryResponse result = userService.summary("default-public");

        assertEquals("public bio", result.bio());
        assertEquals(2L, result.post_count());
    }
}
