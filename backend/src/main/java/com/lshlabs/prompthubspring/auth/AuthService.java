package com.lshlabs.prompthubspring.auth;

import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Transactional
    public RegisterResponse register(String email, String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다.");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(generateUsername(email));
        user.setAvatarColor1(randomColor());
        user.setAvatarColor2(randomColor());
        user = userRepository.save(user);

        UserSettings settings = new UserSettings();
        settings.setUser(user);
        userSettingsRepository.save(settings);

        TokenPair tokens = issueTokenPair(user);
        return new RegisterResponse(
                "회원가입이 완료되었습니다.",
                tokens.access(),
                tokens.refresh(),
                UserMapper.toUserData(user)
        );
    }

    @Transactional
    public LoginResponse login(String email, String password, String userAgent, String ip) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        TokenPair tokens = issueTokenPair(user);
        UserSession session = createSession(user, userAgent, ip);

        return new LoginResponse(
                "로그인이 완료되었습니다.",
                tokens.access(),
                tokens.refresh(),
                UserMapper.toUserData(user),
                UserMapper.toSessionDto(session)
        );
    }

    @Transactional
    public LoginResponse loginWithGoogle(String idToken, String userAgent, String ip) {
        var payload = googleTokenVerifier.verify(idToken);

        AppUser user = userRepository.findByGoogleSub(payload.sub()).orElseGet(() -> {
            AppUser created = new AppUser();
            created.setGoogleSub(payload.sub());
            created.setEmail(payload.email());
            created.setUsername(generateUniqueUsername(payload.name()));
            created.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            created.setAvatarColor1(randomColor());
            created.setAvatarColor2(randomColor());
            AppUser saved = userRepository.save(created);

            UserSettings settings = new UserSettings();
            settings.setUser(saved);
            userSettingsRepository.save(settings);
            return saved;
        });

        TokenPair tokens = issueTokenPair(user);
        UserSession session = createSession(user, userAgent, ip);

        return new LoginResponse(
                "Google 로그인이 완료되었습니다.",
                tokens.access(),
                tokens.refresh(),
                UserMapper.toUserData(user),
                UserMapper.toSessionDto(session)
        );
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "refresh token이 필요합니다.");
        }
        if (!jwtProvider.isValidRefreshToken(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다.");
        }
        AuthToken stored = authTokenRepository
                .findByTokenAndTokenTypeAndRevokedAtIsNull(refreshToken, AuthTokenType.REFRESH)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다."));
        if (stored.getExpiresAt() != null && stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "만료된 refresh token입니다.");
        }
        Long userId = jwtProvider.parseRefreshToken(refreshToken);
        if (stored.getUser() == null || stored.getUser().getId() == null || !stored.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다.");
        }
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다."));

        authTokenRepository.revokeByToken(refreshToken, Instant.now());
        return issueTokenPair(user);
    }

    @Transactional
    public void logout(AppUser user, String sessionKey) {
        authTokenRepository.revokeAllByUser(user, Instant.now());
        if (sessionKey != null && !sessionKey.isBlank()) {
            userSessionRepository.findBySessionKeyAndUser(sessionKey, user).ifPresent(session -> {
                session.setRevokedAt(Instant.now());
                userSessionRepository.save(session);
            });
        }
    }

    @Transactional
    public TokenPair rotateTokenPair(AppUser user) {
        authTokenRepository.revokeAllByUser(user, Instant.now());
        return issueTokenPair(user);
    }

    private TokenPair issueTokenPair(AppUser user) {
        String access = issueToken(user, AuthTokenType.ACCESS);
        String refresh = issueToken(user, AuthTokenType.REFRESH);
        return new TokenPair(access, refresh);
    }

    private String issueToken(AppUser user, AuthTokenType type) {
        String token = jwtProvider.createAccessToken(user.getId(), user.getEmail());
        if (type == AuthTokenType.REFRESH) {
            token = jwtProvider.createRefreshToken(user.getId());
        }
        AuthToken authToken = new AuthToken();
        authToken.setUser(user);
        authToken.setToken(token);
        authToken.setTokenType(type);
        authToken.setExpiresAt(type == AuthTokenType.ACCESS
                ? Instant.now().plusMillis(jwtProvider.getAccessExpirationMs())
                : Instant.now().plusMillis(jwtProvider.getRefreshExpirationMs()));
        authTokenRepository.save(authToken);
        return token;
    }

    private UserSession createSession(AppUser user, String userAgent, String ip) {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(userAgent);
        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionKey(
                UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        session.setUserAgent(parsed.rawUserAgent());
        session.setIpAddress(ip);
        session.setDevice(parsed.device());
        session.setBrowser(parsed.browser());
        session.setOs(parsed.os());
        return userSessionRepository.save(session);
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        return generateUniqueUsername(base);
    }

    private String generateUniqueUsername(String base) {
        String trimmed = (base == null || base.isBlank()) ? "user" : base;
        String candidate = trimmed;
        int idx = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = trimmed + idx;
            idx++;
        }
        return candidate;
    }

    private String randomColor() {
        byte[] bytes = new byte[3];
        new SecureRandom().nextBytes(bytes);
        return "#" + HexFormat.of().formatHex(bytes).toUpperCase();
    }

    public record TokenPair(String access, String refresh) {
    }

    public record RegisterResponse(String message, String token, String refresh, UserMapper.UserData user) {
    }

    public record LoginResponse(
            String message,
            String token,
            String refresh,
            UserMapper.UserData user,
            UserMapper.SessionData session
    ) {
    }
}
