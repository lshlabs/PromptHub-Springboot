package com.lshlabs.prompthubspring.auth;

import com.lshlabs.prompthubspring.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByToken(String token);

    @Query("""
            select t
              from AuthToken t
             where t.token = :token
               and t.tokenType = :tokenType
               and t.revokedAt is null
            """)
    Optional<AuthToken> findValidByTokenAndType(@Param("token") String token, @Param("tokenType") AuthTokenType tokenType);

    @Modifying
    @Query("update AuthToken t set t.revokedAt = :now where t.user = :user and t.revokedAt is null")
    int revokeAllByUser(@Param("user") AppUser user, @Param("now") Instant now);

    @Modifying
    @Query("update AuthToken t set t.revokedAt = :now where t.token = :token and t.revokedAt is null")
    int revokeByToken(@Param("token") String token, @Param("now") Instant now);

    void deleteByUser(AppUser user);
}
