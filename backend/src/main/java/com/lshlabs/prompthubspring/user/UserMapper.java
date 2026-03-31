package com.lshlabs.prompthubspring.user;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserData toUserData(AppUser user) {
        return new UserData(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getBio() == null ? "" : user.getBio(),
                user.getLocation() == null ? "" : user.getLocation(),
                user.getGithubHandle() == null ? "" : user.getGithubHandle(),
                user.getProfileImage(),
                user.getAvatarColor1(),
                user.getAvatarColor2(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().toString()
        );
    }

    public static SettingsData toSettingsData(UserSettings settings) {
        return new SettingsData(
                settings.isEmailNotificationsEnabled(),
                settings.isInAppNotificationsEnabled(),
                settings.isPublicProfile(),
                settings.isDataSharing(),
                settings.isTwoFactorAuthEnabled(),
                settings.getUpdatedAt() == null ? null : settings.getUpdatedAt().toString()
        );
    }

    public static SessionData toSessionDto(UserSession session) {
        return new SessionData(
                session.getSessionKey(),
                session.getUserAgent(),
                session.getIpAddress(),
                session.getDevice(),
                session.getBrowser(),
                session.getOs(),
                session.getLocation(),
                session.getCreatedAt() == null ? null : session.getCreatedAt().toString(),
                session.getLastActive() == null ? null : session.getLastActive().toString(),
                session.getRevokedAt() == null ? null : session.getRevokedAt().toString()
        );
    }

    public record UserData(
            Long id,
            String email,
            String username,
            String bio,
            String location,
            String github_handle,
            String profile_image,
            String avatar_color1,
            String avatar_color2,
            String created_at
    ) {
    }

    public record SettingsData(
            boolean email_notifications_enabled,
            boolean in_app_notifications_enabled,
            boolean public_profile,
            boolean data_sharing,
            boolean two_factor_auth_enabled,
            String updated_at
    ) {
    }

    public record SessionData(
            String key,
            String user_agent,
            String ip_address,
            String device,
            String browser,
            String os,
            String location,
            String created_at,
            String last_active,
            String revoked_at
    ) {
    }
}
