package com.lshlabs.prompthubspring.user;

import com.lshlabs.prompthubspring.auth.AuthService;
import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.post.PostInteractionRepository;
import com.lshlabs.prompthubspring.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuthTokenRepository authTokenRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final PostInteractionRepository postInteractionRepository;

    public ProfileResponse profile(AppUser user) {
        UserSettings settings = userSettingsRepository.findByUser(user).orElseGet(() -> {
            UserSettings created = new UserSettings();
            created.setUser(user);
            return userSettingsRepository.save(created);
        });

        return new ProfileResponse(
                UserMapper.toUserData(user),
                UserMapper.toSettingsData(settings),
                buildCompleteness(user)
        );
    }

    @Transactional
    public UpdateProfileResponse updateProfile(AppUser user, ProfileUpdateRequest request) {
        if (request.username() != null) {
            String username = request.username().trim();
            if (username.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "사용자명은 비어 있을 수 없습니다.");
            }
            if (userRepository.existsByUsernameAndIdNot(username, user.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "이미 사용 중인 사용자명입니다.");
            }
            user.setUsername(username);
        }

        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.location() != null) {
            user.setLocation(request.location());
        }
        if (request.github_handle() != null) {
            user.setGithubHandle(request.github_handle());
        }
        if (request.profile_image() != null) {
            user.setProfileImage(request.profile_image());
        }
        if (request.avatar_color1() != null) {
            user.setAvatarColor1(request.avatar_color1());
        }
        if (request.avatar_color2() != null) {
            user.setAvatarColor2(request.avatar_color2());
        }

        userRepository.save(user);
        return new UpdateProfileResponse("프로필이 업데이트되었습니다.", UserMapper.toUserData(user));
    }

    @Transactional
    public ChangePasswordResponse changePassword(AppUser user, String currentPassword, String newPassword, String confirm) {
        if (!StringUtils.hasText(currentPassword) || !StringUtils.hasText(newPassword) || !StringUtils.hasText(confirm)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "현재 비밀번호, 새 비밀번호, 확인 비밀번호를 모두 입력해야 합니다.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }
        if (!newPassword.equals(confirm)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        AuthService.TokenPair tokenPair = authService.rotateTokenPair(user);
        if (!StringUtils.hasText(tokenPair.access()) || !StringUtils.hasText(tokenPair.refresh())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "비밀번호 변경 후 토큰 재발급에 실패했습니다.");
        }

        return new ChangePasswordResponse(
                "비밀번호가 성공적으로 변경되었습니다.",
                tokenPair.access(),
                tokenPair.refresh()
        );
    }

    @Transactional
    public UserMapper.SettingsData updateSettings(AppUser user, SettingsUpdateRequest request) {
        UserSettings settings = userSettingsRepository.findByUser(user).orElseGet(() -> {
            UserSettings created = new UserSettings();
            created.setUser(user);
            return created;
        });

        if (request.email_notifications_enabled() != null) {
            settings.setEmailNotificationsEnabled(request.email_notifications_enabled());
        }
        if (request.in_app_notifications_enabled() != null) {
            settings.setInAppNotificationsEnabled(request.in_app_notifications_enabled());
        }
        if (request.public_profile() != null) {
            settings.setPublicProfile(request.public_profile());
        }
        if (request.data_sharing() != null) {
            settings.setDataSharing(request.data_sharing());
        }
        if (request.two_factor_auth_enabled() != null) {
            settings.setTwoFactorAuthEnabled(request.two_factor_auth_enabled());
        }

        return UserMapper.toSettingsData(userSettingsRepository.save(settings));
    }

    public List<UserMapper.SessionData> sessions(AppUser user) {
        return userSessionRepository.findByUserAndRevokedAtIsNullOrderByLastActiveDesc(user)
                .stream()
                .map(UserMapper::toSessionDto)
                .toList();
    }

    @Transactional
    public MessageResponse endSession(AppUser user, String sessionKey) {
        if (!StringUtils.hasText(sessionKey)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "종료할 세션 키가 필요합니다.");
        }

        UserSession session = userSessionRepository.findBySessionKeyAndUserAndRevokedAtIsNull(sessionKey, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "해당 세션을 찾을 수 없습니다."));
        session.setRevokedAt(java.time.Instant.now());
        userSessionRepository.save(session);
        return new MessageResponse("세션이 종료되었습니다.");
    }

    @Transactional
    public EndOtherSessionsResponse endOtherSessions(AppUser user, String currentSessionKey) {
        int revokedCount = userSessionRepository.revokeOtherActiveSessions(
                user,
                StringUtils.hasText(currentSessionKey) ? currentSessionKey : null,
                java.time.Instant.now()
        );
        return new EndOtherSessionsResponse("다른 모든 세션을 종료했습니다.", revokedCount);
    }

    public UserInfoResponse userInfo(AppUser user) {
        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getProfileImage(),
                user.getAvatarColor1(),
                user.getAvatarColor2(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().toString()
        );
    }

    public UserSummaryResponse summary(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        boolean publicProfile = userSettingsRepository.findByUser(user)
                .map(UserSettings::isPublicProfile)
                .orElse(true);

        return new UserSummaryResponse(
                user.getUsername(),
                publicProfile ? user.getBio() : null,
                user.getProfileImage(),
                user.getAvatarColor1(),
                user.getAvatarColor2(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().toString(),
                postRepository.countByAuthor(user),
                postRepository.sumViewCountByAuthor(user),
                postRepository.sumLikeCountByAuthor(user),
                postRepository.sumBookmarkCountByAuthor(user)
        );
    }

    @Transactional
    public UpdateProfileResponse regenerateAvatar(AppUser user) {
        user.setAvatarColor1(randomColor());
        user.setAvatarColor2(randomColor());
        userRepository.save(user);
        return new UpdateProfileResponse("아바타 색상이 재생성되었습니다.", UserMapper.toUserData(user));
    }

    @Transactional
    public MessageResponse deleteAccount(AppUser user) {
        authTokenRepository.deleteByUser(user);
        userSessionRepository.deleteByUser(user);
        userSettingsRepository.deleteByUser(user);
        userRepository.deleteById(user.getId());
        return new MessageResponse("계정이 삭제되었습니다.");
    }

    private String randomColor() {
        byte[] bytes = new byte[3];
        new SecureRandom().nextBytes(bytes);
        return "#" + HexFormat.of().formatHex(bytes).toUpperCase();
    }

    private ProfileCompleteness buildCompleteness(AppUser user) {
        String[] fields = { "username", "bio", "location", "github_handle" };
        int completed = 0;
        List<String> missing = new java.util.ArrayList<>();

        if (StringUtils.hasText(user.getUsername())) {
            completed++;
        } else {
            missing.add("username");
        }
        if (StringUtils.hasText(user.getBio())) {
            completed++;
        } else {
            missing.add("bio");
        }
        if (StringUtils.hasText(user.getLocation())) {
            completed++;
        } else {
            missing.add("location");
        }
        if (StringUtils.hasText(user.getGithubHandle())) {
            completed++;
        } else {
            missing.add("github_handle");
        }

        return new ProfileCompleteness(completed * 100.0 / fields.length, completed, fields.length, missing);
    }

    public record ProfileUpdateRequest(
            String username,
            String bio,
            String location,
            String github_handle,
            String profile_image,
            String avatar_color1,
            String avatar_color2
    ) {}

    public record SettingsUpdateRequest(
            Boolean email_notifications_enabled,
            Boolean in_app_notifications_enabled,
            Boolean public_profile,
            Boolean data_sharing,
            Boolean two_factor_auth_enabled
    ) {}

    public record ChangePasswordRequest(
            String current_password,
            String new_password,
            String new_password_confirm
    ) {}

    public record ProfileResponse(
            UserMapper.UserData user,
            UserMapper.SettingsData settings,
            ProfileCompleteness profile_completeness
    ) {
    }

    public record ProfileCompleteness(
            double percentage,
            int completed_fields,
            int total_fields,
            List<String> missing_fields
    ) {
    }

    public record UpdateProfileResponse(String message, UserMapper.UserData user) {
    }

    public record ChangePasswordResponse(String message, String token, String refresh) {
    }

    public sealed interface SessionEndResponse permits EndOtherSessionsResponse, MessageResponse {
    }

    public record EndOtherSessionsResponse(String message, int count) implements SessionEndResponse {
    }

    public record UserInfoResponse(
            Long id,
            String email,
            String username,
            String avatar_url,
            String avatar_color1,
            String avatar_color2,
            String created_at
    ) {
    }

    public record UserSummaryResponse(
            String username,
            String bio,
            String avatar_url,
            String avatar_color1,
            String avatar_color2,
            String created_at,
            long post_count,
            long total_views,
            long total_likes_received,
            long total_bookmarks_received
    ) {
    }

    public record MessageResponse(String message) implements SessionEndResponse {
    }
}
