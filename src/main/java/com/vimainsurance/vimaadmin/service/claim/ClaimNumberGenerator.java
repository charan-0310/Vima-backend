package com.vimainsurance.vimaadmin.service.claim;

import java.time.Year;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.entity.ClaimNumberSequence;
import com.vimainsurance.vimaadmin.repository.IClaimNumberSequenceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe claim number generation: VIMA-CLM-{YYYY}-{NNNN}.
 * Uses DB sequence table with pessimistic lock per year.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimNumberGenerator {

    private static final String PREFIX = "VIMA-CLM";

    private final IClaimNumberSequenceRepository sequenceRepository;

    @Transactional(rollbackFor = Exception.class)
    public String generateNext() {
        int year = Year.now().getValue();
        ClaimNumberSequence seq = sequenceRepository.findByClaimYear(year)
                .orElseGet(() -> {
                    ClaimNumberSequence newSeq = new ClaimNumberSequence();
                    newSeq.setClaimYear(year);
                    newSeq.setNextSequence(1);
                    newSeq.setUpdatedAt(java.time.LocalDateTime.now());
                    return sequenceRepository.save(newSeq);
                });
        int next = seq.getNextSequence();
        seq.setNextSequence(next + 1);
        seq.setUpdatedAt(java.time.LocalDateTime.now());
        sequenceRepository.save(seq);
        String claimNumber = String.format("%s-%d-%04d", PREFIX, year, next);
        log.debug("Generated claim number: {}", claimNumber);
        return claimNumber;
    }
}
