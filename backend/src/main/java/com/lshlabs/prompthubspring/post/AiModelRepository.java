package com.lshlabs.prompthubspring.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiModelRepository extends JpaRepository<AiModel, Long> {
    @Query("""
            select m
              from AiModel m
             where m.isActive = true
               and m.isDeprecated = false
             order by m.sortOrder asc, m.name asc
            """)
    List<AiModel> listAvailableModels();

    @Query("""
            select m
              from AiModel m
             where m.platform.id = :platformId
               and m.isActive = true
               and m.isDeprecated = false
             order by m.sortOrder asc, m.name asc
            """)
    List<AiModel> findDisplayableByPlatformId(@Param("platformId") Long platformId);

    @Query("""
            select m
              from AiModel m
             where lower(m.name) like lower(concat('%', :query, '%'))
               and m.isActive = true
               and m.isDeprecated = false
             order by m.sortOrder asc, m.name asc
            """)
    List<AiModel> findModelSuggestions(@Param("query") String query);

    @Query("""
            select m
              from AiModel m
             where m.platform.id = :platformId
               and lower(m.name) like lower(concat('%', :query, '%'))
               and m.isActive = true
               and m.isDeprecated = false
             order by m.sortOrder asc, m.name asc
            """)
    List<AiModel> suggestTop10ByPlatform(@Param("platformId") Long platformId, @Param("query") String query);

    @Query("""
            select m
              from AiModel m
              join m.platform p
             where m.isActive = true
               and m.isDeprecated = false
               and p.isActive = true
               and (
                   lower(m.name) like lower(concat('%', :query, '%'))
                   or lower(coalesce(m.slug, '')) like lower(concat('%', :query, '%'))
                   or lower(p.name) like lower(concat('%', :query, '%'))
                   or lower(coalesce(p.slug, '')) like lower(concat('%', :query, '%'))
               )
            """)
    List<AiModel> searchSuggestCandidates(@Param("query") String query);

    @Query("""
            select m
              from AiModel m
              join m.platform p
             where m.isActive = true
               and m.isDeprecated = false
               and p.isActive = true
               and p.id = :platformId
               and (
                   lower(m.name) like lower(concat('%', :query, '%'))
                   or lower(coalesce(m.slug, '')) like lower(concat('%', :query, '%'))
                   or lower(p.name) like lower(concat('%', :query, '%'))
                   or lower(coalesce(p.slug, '')) like lower(concat('%', :query, '%'))
               )
            """)
    List<AiModel> searchSuggestCandidatesByPlatform(@Param("platformId") Long platformId, @Param("query") String query);
}
