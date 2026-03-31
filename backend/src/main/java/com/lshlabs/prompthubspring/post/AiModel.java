package com.lshlabs.prompthubspring.post;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_models", uniqueConstraints = {
        @UniqueConstraint(name = "uk_model_platform_name", columnNames = { "platform_id", "name" }),
        @UniqueConstraint(name = "unique_model_slug_per_platform", columnNames = { "platform_id", "slug" }) })
@Getter
@Setter
@NoArgsConstructor
public class AiModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "platform_id")
    private Platform platform;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 120)
    private String slug;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean isDeprecated = false;

    @Column(nullable = false)
    private int sortOrder = 0;
}
