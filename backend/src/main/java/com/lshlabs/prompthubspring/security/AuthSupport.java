package com.lshlabs.prompthubspring.security;

import com.lshlabs.prompthubspring.common.ApiException;
import com.lshlabs.prompthubspring.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthSupport {
    public AppUser currentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return user;
    }
}
