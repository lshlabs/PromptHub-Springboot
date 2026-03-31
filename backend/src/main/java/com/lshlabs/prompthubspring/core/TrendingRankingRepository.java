package com.lshlabs.prompthubspring.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface TrendingRankingRepository extends JpaRepository<TrendingRankingEntity, Long> {
    List<TrendingRankingEntity> findByCategoryAndActiveTrueOrderByRankAsc(TrendingCategoryEntity category);

    @EntityGraph(attributePaths = { "category", "relatedModel", "relatedModel.platform" })
    Optional<TrendingRankingEntity> findFirstByNameAndActiveTrueOrderByRankAsc(String name);

    @EntityGraph(attributePaths = { "category", "relatedModel", "relatedModel.platform" })
    Optional<TrendingRankingEntity> findFirstByNameAndActiveTrueAndRelatedModelIsNotNullOrderByRankAsc(String name);
}
