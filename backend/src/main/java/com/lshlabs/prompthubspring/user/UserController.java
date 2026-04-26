package com.lshlabs.prompthubspring.user;

import com.lshlabs.prompthubspring.security.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {
    private final AuthSupport authSupport;
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserService.ProfileResponse> profile() {
        return ResponseEntity.ok(userService.profile(authSupport.currentUserOrThrow()));
    }

    @PutMapping(
            value = "/profile",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserService.UpdateProfileResponse> updateProfilePut(@RequestBody UserService.ProfileUpdateRequest payload) {
        return ResponseEntity.ok(userService.updateProfile(
                authSupport.currentUserOrThrow(),
                payload
        ));
    }

    @PatchMapping(
            value = "/profile",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserService.UpdateProfileResponse> updateProfilePatch(@RequestBody UserService.ProfileUpdateRequest payload) {
        return ResponseEntity.ok(userService.updateProfile(
                authSupport.currentUserOrThrow(),
                payload
        ));
    }

    @PutMapping(
            value = "/profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserService.UpdateProfileResponse> replaceProfileWithMultipart(
            @RequestPart(value = "profile_image", required = false) MultipartFile profileImage,
            @RequestPart(value = "username", required = false) String username,
            @RequestPart(value = "bio", required = false) String bio,
            @RequestPart(value = "location", required = false) String location,
            @RequestPart(value = "github_handle", required = false) String githubHandle,
            @RequestPart(value = "remove_profile_image", required = false) String removeProfileImage,
            @RequestPart(value = "avatar_color1", required = false) String avatarColor1,
            @RequestPart(value = "avatar_color2", required = false) String avatarColor2
    ) {
        var payload = new UserService.ProfileUpdateRequest(
                username,
                bio,
                location,
                githubHandle,
                null,
                parseBoolean(removeProfileImage),
                avatarColor1,
                avatarColor2
        );
        return ResponseEntity.ok(userService.updateProfile(
                authSupport.currentUserOrThrow(),
                payload,
                profileImage
        ));
    }

    @PatchMapping(
            value = "/profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserService.UpdateProfileResponse> patchProfileWithMultipart(
            @RequestPart(value = "profile_image", required = false) MultipartFile profileImage,
            @RequestPart(value = "username", required = false) String username,
            @RequestPart(value = "bio", required = false) String bio,
            @RequestPart(value = "location", required = false) String location,
            @RequestPart(value = "github_handle", required = false) String githubHandle,
            @RequestPart(value = "remove_profile_image", required = false) String removeProfileImage,
            @RequestPart(value = "avatar_color1", required = false) String avatarColor1,
            @RequestPart(value = "avatar_color2", required = false) String avatarColor2
    ) {
        var payload = new UserService.ProfileUpdateRequest(
                username,
                bio,
                location,
                githubHandle,
                null,
                parseBoolean(removeProfileImage),
                avatarColor1,
                avatarColor2
        );
        return ResponseEntity.ok(userService.updateProfile(
                authSupport.currentUserOrThrow(),
                payload,
                profileImage
        ));
    }

    @PatchMapping("/profile/password")
    public ResponseEntity<UserService.ChangePasswordResponse> changePassword(@RequestBody UserService.ChangePasswordRequest payload) {
        return ResponseEntity.ok(userService.changePassword(
                authSupport.currentUserOrThrow(),
                payload.current_password(),
                payload.new_password(),
                payload.new_password_confirm()
        ));
    }

    @GetMapping("/profile/settings")
    public ResponseEntity<UserMapper.SettingsData> getSettings() {
        var user = authSupport.currentUserOrThrow();
        return ResponseEntity.ok(userService.profile(user).settings());
    }

    @PatchMapping("/profile/settings")
    public ResponseEntity<UserMapper.SettingsData> patchSettings(@RequestBody UserService.SettingsUpdateRequest payload) {
        return ResponseEntity.ok(userService.updateSettings(
                authSupport.currentUserOrThrow(),
                payload
        ));
    }

    @PutMapping("/profile/settings")
    public ResponseEntity<UserMapper.SettingsData> putSettings(@RequestBody UserService.SettingsUpdateRequest payload) {
        return ResponseEntity.ok(userService.updateSettings(
                authSupport.currentUserOrThrow(),
                payload
        ));
    }

    @PostMapping("/profile/avatar/regenerate")
    public ResponseEntity<UserService.UpdateProfileResponse> regenerateAvatar() {
        return ResponseEntity.ok(userService.regenerateAvatar(authSupport.currentUserOrThrow()));
    }

    @GetMapping("/profile/sessions")
    public ResponseEntity<List<UserMapper.SessionData>> sessions() {
        return ResponseEntity.ok(userService.sessions(authSupport.currentUserOrThrow()));
    }

    @DeleteMapping("/profile/sessions")
    public ResponseEntity<UserService.SessionEndResponse> endSessions(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "all", required = false, defaultValue = "false") boolean all,
            @RequestHeader(value = "X-Session-Key", required = false) String currentSessionKey) {
        var user = authSupport.currentUserOrThrow();
        if (all) {
            return ResponseEntity.ok(userService.endOtherSessions(user, currentSessionKey));
        }
        return ResponseEntity.ok(userService.endSession(user, key));
    }

    @DeleteMapping("/profile/delete")
    public ResponseEntity<UserService.MessageResponse> deleteAccount() {
        return ResponseEntity.ok(userService.deleteAccount(authSupport.currentUserOrThrow()));
    }

    @GetMapping("/users/{username}/summary")
    public ResponseEntity<UserService.UserSummaryResponse> summary(@PathVariable String username) {
        return ResponseEntity.ok(userService.summary(username));
    }

    @GetMapping("/info")
    public ResponseEntity<UserService.UserInfoResponse> info() {
        return ResponseEntity.ok(userService.userInfo(authSupport.currentUserOrThrow()));
    }

    private Boolean parseBoolean(String raw) {
        if (raw == null) {
            return null;
        }
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw) || "on".equalsIgnoreCase(raw);
    }
}
