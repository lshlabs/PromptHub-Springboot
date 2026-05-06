package com.lshlabs.prompthubspring.auth;

import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
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
    private static final int USERNAME_RETRY_MAX_ATTEMPTS = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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

        AppUser user = createUserWithUniqueUsername(email, null, passwordEncoder.encode(password), generateUsernameBaseFromEmail(email));

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
            AppUser saved = createUserWithUniqueUsername(
                    payload.email(),
                    payload.sub(),
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    payload.name()
            );

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
                .findValidByTokenAndType(refreshToken, AuthTokenType.REFRESH)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다."));
        // JWT 자체 유효성 + 서버 저장 토큰 상태를 함께 검사해서 탈취/재사용 토큰을 차단한다.
        if (stored.getExpiresAt() != null && stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "만료된 refresh token입니다.");
        }
        Long userId = jwtProvider.parseRefreshToken(refreshToken);
        // DB에 저장된 소유자와 JWT subject가 다르면 위변조/혼용으로 간주한다.
        if (stored.getUser() == null || stored.getUser().getId() == null || !stored.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다.");
        }
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다."));

        // refresh는 1회성으로 취급해 즉시 폐기하고 새 토큰 쌍을 발급한다.
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
        // Access/Refresh를 모두 DB에 저장해 서버 측 강제 만료(로그아웃, 비번 변경)를 가능하게 한다.
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

    private String generateUsernameBaseFromEmail(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        return base.isBlank() ? "user" : base;
    }

    private AppUser createUserWithUniqueUsername(String email, String googleSub, String encodedPassword, String usernameBase) {
        String normalizedBase = normalizeUsernameBase(usernameBase);
        for (int attempt = 0; attempt < USERNAME_RETRY_MAX_ATTEMPTS; attempt++) {
            String candidate = usernameCandidate(normalizedBase, attempt);
            AppUser created = new AppUser();
            created.setGoogleSub(googleSub);
            created.setEmail(email);
            created.setUsername(candidate);
            created.setPassword(encodedPassword);
            created.setAvatarColor1(randomColor());
            created.setAvatarColor2(randomColor());
            try {
                return userRepository.save(created);
            } catch (DataIntegrityViolationException exception) {
                // 같은 이름 가입이 동시에 들어올 수 있어서 DB 제약에 걸린 경우만 다음 후보를 다시 시도한다.
                if (userRepository.existsByUsername(candidate)) {
                    continue;
                }
                throw exception;
            }
        }
        throw new ApiException(HttpStatus.CONFLICT, "사용 가능한 사용자명을 할당하지 못했습니다.");
    }

    private static String normalizeUsernameBase(String base) {
        if (base == null || base.isBlank()) {
            return "user";
        }
        String normalized = base.replaceAll("[^a-zA-Z0-9_]", "");
        return normalized.isBlank() ? "user" : normalized;
    }

    private static String usernameCandidate(String normalizedBase, int attempt) {
        if (attempt == 0) {
            return normalizedBase;
        }
        return normalizedBase + attempt;
    }

    private String randomColor() {
        byte[] bytes = new byte[3];
        SECURE_RANDOM.nextBytes(bytes);
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
