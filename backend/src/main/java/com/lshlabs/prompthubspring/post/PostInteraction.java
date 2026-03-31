package com.lshlabs.prompthubspring.post;

import com.lshlabs.prompthubspring.user.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "post_interactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_interaction_user_post", columnNames = { "user_id", "post_id" }) })
@Getter
@Setter
@NoArgsConstructor
public class PostInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "is_liked", nullable = false)
    private boolean liked;

    @Column(name = "is_bookmarked", nullable = false)
    private boolean bookmarked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
