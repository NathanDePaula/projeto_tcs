package com.projetotcs.instagram.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import com.projetotcs.instagram.domain.entity.BlacklistedToken;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    boolean existsByJti(String jti);
    void deleteByExpirationDateBefore(Instant now);
}
