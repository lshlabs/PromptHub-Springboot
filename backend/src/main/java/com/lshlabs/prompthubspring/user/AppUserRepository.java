package com.lshlabs.prompthubspring.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<AppUser> findByGoogleSub(String googleSub);
    boolean existsByUsernameAndIdNot(String username, Long id);
}
