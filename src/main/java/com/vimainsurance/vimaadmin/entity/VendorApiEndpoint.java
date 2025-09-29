package com.vimainsurance.vimaadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(schema = "admin", name = "vendor_api_endpoints")
public class VendorApiEndpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "api_key", nullable = false, length = 100)
    private String apiKey;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false, length = 10)
    private String method = "POST";

    @Column(nullable = false)
    private Boolean active = true;

    @Column
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 