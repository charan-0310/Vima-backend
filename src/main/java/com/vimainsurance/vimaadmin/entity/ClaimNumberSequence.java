package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claim_number_sequence", schema = "claims")
public class ClaimNumberSequence {

    @Id
    @Column(name = "claim_year", nullable = false)
    private Integer claimYear;

    @Column(name = "next_sequence", nullable = false)
    private Integer nextSequence = 1;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
