package com.lshlabs.prompthubspring.config;

import com.lshlabs.prompthubspring.security.TokenAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthFilter tokenAuthFilter)
            throws Exception {
        boolean localOrTest = environment.acceptsProfiles(Profiles.of("local", "test"));

        http.csrf(csrf -> csrf.disable()).cors(cors -> {
        }).sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> {
                            auth.requestMatchers("/api/core/health").permitAll();
                            if (localOrTest) {
                                auth.requestMatchers("/h2-console/**").permitAll();
                            }
                            auth.requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/core/**").permitAll();
                            auth.requestMatchers(HttpMethod.GET,
                                            "/api/stats/dashboard",
                                            "/api/auth/users/*/summary")
                                    .permitAll();
                            auth.requestMatchers(
                                            "/api/auth/register",
                                            "/api/auth/login",
                                            "/api/auth/google",
                                            "/api/auth/token/refresh")
                                    .permitAll()
                                    .anyRequest()
                                    .authenticated();
                        })
                .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (localOrTest) {
            http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
