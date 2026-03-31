package com.lshlabs.prompthubspring.core;

import com.lshlabs.prompthubspring.post.AiModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "trending_rankings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trending_category_rank", columnNames = { "category_id", "rank" }) })
@Getter
@Setter
@NoArgsConstructor
public class TrendingRankingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private TrendingCategoryEntity category;

    @Column(nullable = false)
    private Integer rank;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String score;

    @Column(nullable = false, length = 50)
    private String provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_model_id")
    private AiModel relatedModel;

    @Column(name = "model_detail_contains", nullable = false, length = 100)
    private String modelDetailContains = "";

    @Column(name = "model_etc_contains", nullable = false, length = 100)
    private String modelEtcContains = "";

    @Column(name = "use_exact_matching", nullable = false)
    private boolean useExactMatching;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
