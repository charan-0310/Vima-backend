package com.vimainsurance.vimaadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(schema = "admin", name = "vendors")
public class Vendor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String config;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 