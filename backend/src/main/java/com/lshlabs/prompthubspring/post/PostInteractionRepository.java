package com.lshlabs.prompthubspring.post;

import com.lshlabs.prompthubspring.user.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PostInteractionRepository extends JpaRepository<PostInteraction, Long> {
    Optional<PostInteraction> findByUserAndPost(AppUser user, Post post);

    Page<PostInteraction> findByUserAndLikedTrueOrderByUpdatedAtDescIdDesc(AppUser user, Pageable pageable);

    Page<PostInteraction> findByUserAndBookmarkedTrueOrderByUpdatedAtDescIdDesc(AppUser user, Pageable pageable);

    long countByPostAndLikedTrue(Post post);

    long countByPostAndBookmarkedTrue(Post post);

    @Query("select count(pi) from PostInteraction pi where pi.post.author = :author and pi.liked = true")
    long countLikedReceivedByAuthor(@Param("author") AppUser author);

    @Query("select count(pi) from PostInteraction pi where pi.post.author = :author and pi.bookmarked = true")
    long countBookmarkedReceivedByAuthor(@Param("author") AppUser author);

    @Query("select max(pi.updatedAt) from PostInteraction pi where pi.user = :user and pi.liked = true")
    Instant findLastLikedAtByUser(@Param("user") AppUser user);

    @Query("select max(pi.updatedAt) from PostInteraction pi where pi.user = :user and pi.bookmarked = true")
    Instant findLastBookmarkedAtByUser(@Param("user") AppUser user);
}
