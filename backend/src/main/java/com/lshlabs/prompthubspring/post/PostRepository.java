package com.lshlabs.prompthubspring.post;

import com.lshlabs.prompthubspring.user.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    long countByAuthor(AppUser author);

    long countByCreatedAtAfter(Instant since);

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select coalesce(sum(p.viewCount), 0) from Post p where p.author = :author")
    long sumViewCountByAuthor(@Param("author") AppUser author);

    @Query("select coalesce(sum(p.likeCount), 0) from Post p where p.author = :author")
    long sumLikeCountByAuthor(@Param("author") AppUser author);

    @Query("select coalesce(sum(p.bookmarkCount), 0) from Post p where p.author = :author")
    long sumBookmarkCountByAuthor(@Param("author") AppUser author);

    @Query("select coalesce(avg(p.satisfaction), 0) from Post p where p.satisfaction is not null")
    Double avgSatisfactionAll();

    @Query("select coalesce(avg(p.satisfaction), 0) from Post p where p.author = :author and p.satisfaction is not null")
    Double avgSatisfactionByAuthor(@Param("author") AppUser author);

    @Query("select coalesce(sum(p.viewCount), 0) from Post p")
    long sumViewCountAll();

    @Query("select coalesce(sum(p.likeCount), 0) from Post p")
    long sumLikeCountAll();

    @Query("select coalesce(sum(p.bookmarkCount), 0) from Post p")
    long sumBookmarkCountAll();

    @Query("select p.platform.name, count(p) from Post p group by p.platform.name order by count(p) desc")
    List<Object[]> countPostsByPlatform();

    @Query("select p.category.name, count(p) from Post p group by p.category.name order by count(p) desc")
    List<Object[]> countPostsByCategory();

    @Query("select count(distinct p.author.id) from Post p where p.createdAt >= :since")
    long countDistinctAuthorsSince(@Param("since") Instant since);

    @Query("select p.platform.name from Post p where p.author = :author group by p.platform.name order by count(p) desc")
    List<String> findTopPlatformNamesByAuthor(@Param("author") AppUser author, Pageable pageable);

    @Query("select p.category.name from Post p where p.author = :author group by p.category.name order by count(p) desc")
    List<String> findTopCategoryNamesByAuthor(@Param("author") AppUser author, Pageable pageable);

    @Query("select max(p.createdAt) from Post p where p.author = :author")
    Instant findLastPostDateByAuthor(@Param("author") AppUser author);

    Page<Post> findByAuthor(AppUser author, Pageable pageable);

    Page<Post> findByTitleContainingIgnoreCase(String query, Pageable pageable);

    @Query("""
            select p
              from Post p
              left join p.model m
             where lower(coalesce(m.name, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.modelDetail, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.modelEtc, '')) like lower(concat('%', :keyword, '%'))
            """)
    Page<Post> findByModelKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("select p from Post p left join fetch p.author left join fetch p.platform left join fetch p.model left join fetch p.category where p.id = :id")
    Optional<Post> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
