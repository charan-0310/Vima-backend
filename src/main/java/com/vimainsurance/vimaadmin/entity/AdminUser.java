package com.vimainsurance.vimaadmin.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Data
@Table(name = "admin_users", schema = "admin")
public class AdminUser {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(length = 100, unique = true, nullable = false)
    private String username;

    @Column(length = 255, unique = true, nullable = false)
    private String email;

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Column(length = 50, nullable = false)
    private String role;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider;

    @Column(name = "oauth_provider_id", length = 100)
    private String oauthProviderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}

