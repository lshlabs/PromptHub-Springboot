package com.lshlabs.prompthubspring.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrendingRankingRepository extends JpaRepository<TrendingRankingEntity, Long> {
    @Query("""
            select r
              from TrendingRankingEntity r
             where r.category = :category
               and r.active = true
             order by r.rank asc
            """)
    List<TrendingRankingEntity> findRankingsByCategory(@Param("category") TrendingCategoryEntity category);

    @EntityGraph(attributePaths = { "category", "relatedModel", "relatedModel.platform" })
    @Query("""
            select r
              from TrendingRankingEntity r
             where r.name = :name
               and r.active = true
             order by r.rank asc
            """)
    Optional<TrendingRankingEntity> findTopActiveByName(@Param("name") String name);

    @EntityGraph(attributePaths = { "category", "relatedModel", "relatedModel.platform" })
    @Query("""
            select r
              from TrendingRankingEntity r
             where r.name = :name
               and r.active = true
               and r.relatedModel is not null
             order by r.rank asc
            """)
    Optional<TrendingRankingEntity> findPrimaryRankingByName(@Param("name") String name);
}
