package com.vimainsurance.vimaadmin.service.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vimainsurance.vimaadmin.entity.ClaimNumberSequence;
import com.vimainsurance.vimaadmin.repository.IClaimNumberSequenceRepository;

@ExtendWith(MockitoExtension.class)
class ClaimNumberGeneratorTest {

    @Mock
    private IClaimNumberSequenceRepository sequenceRepository;

    private ClaimNumberGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ClaimNumberGenerator(sequenceRepository);
    }

    @Test
    void generateNext_existingSequence_returnsFormattedNumber() {
        int year = Year.now().getValue();
        ClaimNumberSequence seq = new ClaimNumberSequence();
        seq.setClaimYear(year);
        seq.setNextSequence(42);
        when(sequenceRepository.findByClaimYear(year)).thenReturn(Optional.of(seq));
        when(sequenceRepository.save(any(ClaimNumberSequence.class))).thenAnswer(i -> i.getArgument(0));

        String result = generator.generateNext();

        assertTrue(result.matches("VIMA-CLM-" + year + "-\\d{4}"));
        assertEquals("VIMA-CLM-" + year + "-0042", result);
        assertEquals(43, seq.getNextSequence());
        verify(sequenceRepository).save(seq);
    }

    @Test
    void generateNext_newYear_createsSequenceAndReturnsNumber() {
        int year = Year.now().getValue();
        when(sequenceRepository.findByClaimYear(year)).thenReturn(Optional.empty());
        ArgumentCaptor<ClaimNumberSequence> captor = ArgumentCaptor.forClass(ClaimNumberSequence.class);
        when(sequenceRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        String result = generator.generateNext();

        assertTrue(result.startsWith("VIMA-CLM-"));
        assertEquals("VIMA-CLM-" + year + "-0001", result);
        List<ClaimNumberSequence> saved = captor.getAllValues();
        assertTrue(saved.size() >= 1);
        assertEquals(year, saved.get(0).getClaimYear());
    }

    @Test
    void generateNext_formatMatchesVimaClmYyyyNnnn() {
        int year = Year.now().getValue();
        ClaimNumberSequence seq = new ClaimNumberSequence();
        seq.setClaimYear(year);
        seq.setNextSequence(1);
        when(sequenceRepository.findByClaimYear(year)).thenReturn(Optional.of(seq));
        when(sequenceRepository.save(any(ClaimNumberSequence.class))).thenAnswer(i -> i.getArgument(0));

        String result = generator.generateNext();

        assertEquals("VIMA-CLM-" + year + "-0001", result);
    }
}
