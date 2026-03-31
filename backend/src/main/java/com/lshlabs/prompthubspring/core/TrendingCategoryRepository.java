package com.lshlabs.prompthubspring.core;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrendingCategoryRepository extends JpaRepository<TrendingCategoryEntity, Long> {
    List<TrendingCategoryEntity> findByActiveTrueOrderByOrderNumAscNameAsc();
}
