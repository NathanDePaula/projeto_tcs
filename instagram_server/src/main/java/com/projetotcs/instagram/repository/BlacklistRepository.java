package com.projetotcs.instagram.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import com.projetotcs.instagram.domain.entity.Blacklist;

public interface BlacklistRepository extends JpaRepository<Blacklist, String> {
    boolean existsByJti(String jti);
    void deleteByExpirationDateBefore(Instant now);
}
