package com.lshlabs.prompthubspring.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users", indexes = { @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_username", columnList = "username", unique = true) })
@Getter
@Setter
@NoArgsConstructor
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "is_superuser", nullable = false)
    private boolean superuser = false;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "first_name", length = 150)
    private String firstName;

    @Column(name = "last_name", length = 150)
    private String lastName;

    @Column(name = "is_staff", nullable = false)
    private boolean staff = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "date_joined", nullable = false)
    private Instant dateJoined = Instant.now();

    @Column(length = 1000)
    private String bio;

    @Column(length = 255)
    private String location;

    @Column(name = "github_handle", length = 100)
    private String githubHandle;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "avatar_color1", length = 20)
    private String avatarColor1 = "#3B82F6";

    @Column(name = "avatar_color2", length = 20)
    private String avatarColor2 = "#8B5CF6";

    @Column(name = "google_sub", length = 255, unique = true)
    private String googleSub;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (dateJoined == null) {
            dateJoined = Instant.now();
        }
    }

}
