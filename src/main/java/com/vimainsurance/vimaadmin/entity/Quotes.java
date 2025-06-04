package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quotes", schema = "admin")
public class Quotes {
    @Id
    @Column(length = 10)
    private String id;

    @Column(name = "quote_id", length = 20)
    private String quoteId;

    @Column(name = "coverage_amount", length = 20)
    private String coverageAmount;

    @Column(name = "best_premium", length = 20)
    private String bestPremium;

    @Column(length = 20)
    private String status;

    @Column(name = "created_date")
    private LocalDate xreatedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference
    private Customer customer;

    @Column(name = "companies", columnDefinition = "text[]")
    private String[] companies;
}
