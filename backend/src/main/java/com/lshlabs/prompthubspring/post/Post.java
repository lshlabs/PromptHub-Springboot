package com.lshlabs.prompthubspring.post;

import com.lshlabs.prompthubspring.user.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "posts", indexes = { @Index(name = "idx_posts_created_at", columnList = "created_at"),
        @Index(name = "idx_posts_author", columnList = "author_id") })
@Check(constraints = "satisfaction is null or (satisfaction >= 0.0 and satisfaction <= 5.0)")
@Getter
@Setter
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @ManyToOne(optional = false)
    @JoinColumn(name = "platform_id")
    private Platform platform;

    @ManyToOne
    @JoinColumn(name = "model_id")
    private AiModel model;

    @Column(name = "model_etc", length = 100)
    private String modelEtc;

    @Column(name = "model_detail", length = 100)
    private String modelDetail;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "category_etc", length = 100)
    private String categoryEtc;

    @Column(length = 2000)
    private String tags;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "ai_response", nullable = false, columnDefinition = "TEXT")
    private String aiResponse;

    @Column(name = "additional_opinion", columnDefinition = "TEXT")
    private String additionalOpinion;

    @Column(precision = 3, scale = 1)
    private BigDecimal satisfaction;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    @Column(name = "bookmark_count", nullable = false)
    private long bookmarkCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String modelDisplayName() {
        if (modelDetail != null && !modelDetail.isBlank())
            return modelDetail;
        if (model != null && "기타".equals(model.getName()) && modelEtc != null && !modelEtc.isBlank())
            return modelEtc;
        return model != null ? model.getName() : "기타";
    }

    public String categoryDisplayName() {
        if (category != null && "기타".equals(category.getName()) && categoryEtc != null && !categoryEtc.isBlank())
            return categoryEtc;
        return category != null ? category.getName() : "";
    }
}
