package com.lshlabs.prompthubspring.auth;

import com.lshlabs.prompthubspring.security.AuthSupport;
import com.lshlabs.prompthubspring.user.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthSupport authSupport;

    @PostMapping("/register")
    public ResponseEntity<AuthService.RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(201)
                .body(authService.register(req.email(), req.password(), req.password_confirm()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthService.LoginResponse> login(@Valid @RequestBody LoginRequest req,
            @RequestHeader(value = "User-Agent", required = false) String ua,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xff,
            jakarta.servlet.http.HttpServletRequest request) {
        String ip = xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
        return ResponseEntity.ok(authService.login(req.email(), req.password(), ua, ip));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthService.LoginResponse> google(@Valid @RequestBody GoogleLoginRequest req,
            @RequestHeader(value = "User-Agent", required = false) String ua,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xff,
            jakarta.servlet.http.HttpServletRequest request) {
        String ip = xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
        return ResponseEntity.ok(authService.loginWithGoogle(req.id_token(), ua, ip));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @RequestBody(required = false) LogoutRequest body) {
        AppUser user = authSupport.currentUserOrThrow();
        if ((sessionKey == null || sessionKey.isBlank()) && body != null) {
            sessionKey = body.session_key();
        }
        authService.logout(user, sessionKey);
        return ResponseEntity.ok(new LogoutResponse("로그아웃이 완료되었습니다."));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<AuthService.TokenPair> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.refresh()));
    }

    public record RegisterRequest(@Email String email, @NotBlank String password, @NotBlank String password_confirm) {
    }

    public record LoginRequest(@Email String email, @NotBlank String password) {
    }

    public record GoogleLoginRequest(@NotBlank String id_token) {
    }

    public record RefreshRequest(@NotBlank String refresh) {
    }

    public record LogoutRequest(@JsonProperty("session_key") String session_key) {
    }

    public record LogoutResponse(String message) {
    }
}
