package com.lshlabs.prompthubspring.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrendingCategoryRepository extends JpaRepository<TrendingCategoryEntity, Long> {
    @Query("""
            select c
              from TrendingCategoryEntity c
             where c.active = true
             order by c.orderNum asc, c.name asc
            """)
    List<TrendingCategoryEntity> findVisibleCategories();
}
