package com.projetotcs.instagram.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projetotcs.instagram.repository.BlacklistedTokenRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BlacklistCleanupService {
    // Classe que limpa a base de dados de tokens já expirados
    
    @Autowired
    private BlacklistedTokenRepository blacklistRepository;

    /**
     * Remove tokens expirados da blacklist a cada 10 minutos (600.000 ms).
     */
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Limpando tokens expirados da blacklist...");
        blacklistRepository.deleteByExpirationDateBefore(Instant.now());
    }
}
