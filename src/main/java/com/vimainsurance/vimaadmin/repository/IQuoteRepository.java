package com.vimainsurance.vimaadmin.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.vimainsurance.vimaadmin.entity.Quotes;

public interface IQuoteRepository  extends JpaRepository<Quotes, UUID>{
    Optional<Quotes> findByQuoteId(String quoteId);
    
    @Query("SELECT MAX(q.quoteId) FROM Quotes q WHERE q.quoteId LIKE 'Q%'")
    String findMaxQuoteId();

}
