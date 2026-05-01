package com.projetotcs.instagram.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "blacklist")
@Entity(name = "BlacklistedToken")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Blacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(name = "expiration_date", nullable = false)
    private Instant expirationDate;

    public Blacklist(String jti, Instant expirationDate) {
        this.jti = jti;
        this.expirationDate = expirationDate;
    }
}
