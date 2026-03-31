package com.lshlabs.prompthubspring.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_sessions", indexes = @Index(name = "idx_user_sessions_key", columnList = "session_key", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "session_key", nullable = false, unique = true, length = 128)
    private String sessionKey;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "device", length = 100)
    private String device;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "os", length = 100)
    private String os;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_active", nullable = false)
    private Instant lastActive = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
