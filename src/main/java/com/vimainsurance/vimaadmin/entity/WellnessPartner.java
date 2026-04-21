package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wellness_partners", schema = "admin")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WellnessPartner {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "icon_name", length = 50)
    private String iconName;

    @Column(name = "card_color", length = 20)
    private String cardColor;

    @Column(name = "redirect_url", length = 500)
    private String redirectUrl;

    @Column(name = "redirect_type", nullable = false, length = 20)
    private String redirectType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
