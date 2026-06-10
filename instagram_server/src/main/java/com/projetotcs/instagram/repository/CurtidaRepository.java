package com.projetotcs.instagram.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.projetotcs.instagram.domain.entity.Curtida;

@Repository
public interface CurtidaRepository extends JpaRepository<Curtida, Long> {
    long countByPostId(Long postId);
    Optional<Curtida> findByUsuarioIdAndPostId(Long usuarioId, Long postId);
    
    @Transactional
    @Modifying
    void deleteByPostId(Long postId);
}
