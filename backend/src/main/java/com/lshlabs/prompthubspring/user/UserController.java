package com.lshlabs.prompthubspring.user;

import com.lshlabs.prompthubspring.security.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {
    private final AuthSupport authSupport;
    private final UserService userService;

    @GetMapping({ "/profile", "/profile/" })
    public ResponseEntity<UserService.ProfileResponse> profile() {
        return ResponseEntity.ok(userService.profile(authSupport.currentUserOrThrow()));
    }

    @PutMapping({ "/profile", "/profile/" })
    public ResponseEntity<UserService.UpdateProfileResponse> updateProfilePut(@RequestBody UserService.ProfileUpdateRequest payload) {
        return ResponseEntity.ok(userService.updateProfile(
                authSupport.currentUserOrThrow(),
                payload
        ));
    }

    @PatchMapping({ "/profile", "/profile/" })
    public ResponseEntity<UserService.UpdateProfileResponse> updateProfilePatch(@RequestBody UserService.ProfileUpdateRequest payload) {
        return ResponseEntity.ok(userService.updateProfile(
                authSupport.currentUserOrThrow(),
                payload
        ));
    }

    @PatchMapping({ "/profile/password", "/profile/password/" })
    public ResponseEntity<UserService.ChangePasswordResponse> changePassword(@RequestBody UserService.ChangePasswordRequest payload) {
        return ResponseEntity.ok(userService.changePassword(
                authSupport.currentUserOrThrow(),
                payload.current_password(),
                payload.new_password(),
                payload.new_password_confirm()
        ));
    }

    @GetMapping({ "/profile/settings", "/profile/settings/" })
    public ResponseEntity<UserMapper.SettingsData> getSettings() {
        var user = authSupport.currentUserOrThrow();
        return ResponseEntity.ok(userService.profile(user).settings());
    }

    @PatchMapping({ "/profile/settings", "/profile/settings/" })
    public ResponseEntity<UserMapper.SettingsData> patchSettings(@RequestBody UserService.SettingsUpdateRequest payload) {
        return ResponseEntity.ok(userService.updateSettings(
                authSupport.currentUserOrThrow(),
                payload
        ));
    }

    @PutMapping({ "/profile/settings", "/profile/settings/" })
    public ResponseEntity<UserMapper.SettingsData> putSettings(@RequestBody UserService.SettingsUpdateRequest payload) {
        return ResponseEntity.ok(userService.updateSettings(
                authSupport.currentUserOrThrow(),
                payload
        ));
    }

    @PostMapping({ "/profile/avatar/regenerate", "/profile/avatar/regenerate/" })
    public ResponseEntity<UserService.UpdateProfileResponse> regenerateAvatar() {
        return ResponseEntity.ok(userService.regenerateAvatar(authSupport.currentUserOrThrow()));
    }

    @GetMapping({ "/profile/sessions", "/profile/sessions/" })
    public ResponseEntity<List<UserMapper.SessionData>> sessions() {
        return ResponseEntity.ok(userService.sessions(authSupport.currentUserOrThrow()));
    }

    @DeleteMapping({ "/profile/sessions", "/profile/sessions/" })
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

    @DeleteMapping({ "/profile/delete", "/profile/delete/" })
    public ResponseEntity<UserService.MessageResponse> deleteAccount() {
        return ResponseEntity.ok(userService.deleteAccount(authSupport.currentUserOrThrow()));
    }

    @GetMapping({ "/users/{username}/summary", "/users/{username}/summary/" })
    public ResponseEntity<UserService.UserSummaryResponse> summary(@PathVariable String username) {
        return ResponseEntity.ok(userService.summary(username));
    }

    @GetMapping({ "/info", "/info/" })
    public ResponseEntity<UserService.UserInfoResponse> info() {
        return ResponseEntity.ok(userService.userInfo(authSupport.currentUserOrThrow()));
    }
}
