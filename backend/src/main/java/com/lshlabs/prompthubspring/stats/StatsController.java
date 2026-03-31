package com.lshlabs.prompthubspring.stats;

import com.lshlabs.prompthubspring.security.AuthSupport;
import com.lshlabs.prompthubspring.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stats")
public class StatsController {
    private final AuthSupport authSupport;
    private final StatsService statsService;

    @GetMapping("/dashboard")
    public ResponseEntity<StatsService.DashboardResponse> dashboard() {
        return ResponseEntity.ok(statsService.dashboard());
    }

    @GetMapping("/user")
    public ResponseEntity<StatsService.UserStatsResponse> userStats() {
        AppUser user = authSupport.currentUserOrThrow();
        return ResponseEntity.ok(statsService.userStats(user));
    }
}
