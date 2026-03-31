package com.lshlabs.prompthubspring.security;

import com.lshlabs.prompthubspring.auth.AuthToken;
import com.lshlabs.prompthubspring.auth.AuthTokenRepository;
import com.lshlabs.prompthubspring.auth.AuthTokenType;
import com.lshlabs.prompthubspring.user.AppUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

@Component
public class TokenAuthFilter extends OncePerRequestFilter {
    private final AuthTokenRepository authTokenRepository;

    public TokenAuthFilter(AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Token ")) {
            String tokenValue = authHeader.substring("Token ".length()).trim();
            Optional<AuthToken> tokenOpt = authTokenRepository.findByTokenAndTokenTypeAndRevokedAtIsNull(tokenValue,
                    AuthTokenType.ACCESS);
            if (tokenOpt.isPresent()) {
                AuthToken authToken = tokenOpt.get();
                if ((authToken.getExpiresAt() == null || authToken.getExpiresAt().isAfter(Instant.now()))
                        && authToken.getRevokedAt() == null) {
                    AppUser user = authToken.getUser();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user,
                            null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
