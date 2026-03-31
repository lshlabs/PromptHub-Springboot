package com.lshlabs.prompthubspring.config;

import com.lshlabs.prompthubspring.security.TokenAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthFilter tokenAuthFilter)
            throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(cors -> {
        }).sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/actuator/**", "/h2-console/**", "/api/core/health", "/api/core/health/").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/core/**").permitAll()
                                .requestMatchers(HttpMethod.GET,
                                        "/api/stats/dashboard", "/api/stats/dashboard/",
                                        "/api/auth/users/*/summary", "/api/auth/users/*/summary/")
                                .permitAll()
                                .requestMatchers(
                                        "/api/auth/register", "/api/auth/register/",
                                        "/api/auth/login", "/api/auth/login/",
                                        "/api/auth/google", "/api/auth/google/",
                                        "/api/auth/token/refresh", "/api/auth/token/refresh/")
                                .permitAll().anyRequest().authenticated())
                .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
