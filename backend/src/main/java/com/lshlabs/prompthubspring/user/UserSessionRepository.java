package com.lshlabs.prompthubspring.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findByUserOrderByLastActiveDesc(AppUser user);

    @Query("""
            select s
              from UserSession s
             where s.user = :user
               and s.revokedAt is null
             order by s.lastActive desc
            """)
    List<UserSession> findActiveSessionsByUser(@Param("user") AppUser user);

    Optional<UserSession> findBySessionKeyAndUser(String sessionKey, AppUser user);

    @Query("""
            select s
              from UserSession s
             where s.sessionKey = :sessionKey
               and s.user = :user
               and s.revokedAt is null
            """)
    Optional<UserSession> findActiveBySessionKeyAndUser(@Param("sessionKey") String sessionKey, @Param("user") AppUser user);

    @Modifying
    @Query("""
            update UserSession s
               set s.revokedAt = :now
             where s.user = :user
               and s.revokedAt is null
               and (:currentSessionKey is null or s.sessionKey <> :currentSessionKey)
            """)
    int revokeOtherActiveSessions(@Param("user") AppUser user,
                                  @Param("currentSessionKey") String currentSessionKey,
                                  @Param("now") Instant now);

    void deleteByUser(AppUser user);
}
