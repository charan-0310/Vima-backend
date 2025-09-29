package com.vimainsurance.vimaadmin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(schema = "admin", name = "vendor_api_headers")
public class VendorApiHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private VendorApiEndpoint endpoint;

    @Column(name = "header_key", nullable = false, length = 100)
    private String headerKey;

    @Column(name = "header_value", nullable = false)
    private String headerValue;

    @Column(name = "is_secret")
    private Boolean isSecret = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 