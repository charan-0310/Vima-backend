package com.vimainsurance.vimaadmin.service.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.vimainsurance.vimaadmin.dto.PremiumRateTableCsvUploadResultDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.PremiumRateTable;
import com.vimainsurance.vimaadmin.repository.IPremiumRateTableRepository;
import com.vimainsurance.vimaadmin.service.PremiumRateTableCacheService;

@ExtendWith(MockitoExtension.class)
class PremiumRateTableServiceImplTest {

    @Mock
    private IPremiumRateTableRepository repository;

    @Mock
    private PremiumRateTableCacheService cacheService;

    @InjectMocks
    private PremiumRateTableServiceImpl service;

    @Test
    void uploadCsv_shouldAllowMultipleAgeBandRowsForSamePlanMember_whenRangesDoNotOverlap() {
        UUID companyId = UUID.randomUUID();
        when(repository.findByOrganizationId(companyId)).thenReturn(List.of());

        String csv = String.join("\n",
                "product_type,member_type,rate,effective_from,effective_to,pricing_model,age_band_min,age_band_max,sum_insured_amount,family_size_min,family_size_max,rate_source,gst_inclusive,gst_percentage,policy_id",
                "GMC,SELF,2500,2025-01-01,,AGE_BANDED,18,25,500000,,,NEGOTIATED,false,,",
                "GMC,SELF,3000,2025-01-01,,AGE_BANDED,26,35,500000,,,NEGOTIATED,false,,");
        MockMultipartFile file = new MockMultipartFile("file", "rate.csv", "text/csv", csv.getBytes());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PremiumRateTable>> saveCaptor = ArgumentCaptor.forClass(List.class);
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.uploadCsv(companyId, file);
        ResponseDto<PremiumRateTableCsvUploadResultDto> body = response.getBody();

        verify(repository).saveAll(saveCaptor.capture());
        verify(cacheService).invalidate(companyId);

        List<PremiumRateTable> savedRows = saveCaptor.getValue();
        assertEquals(2, savedRows.size());
        assertEquals(2, body.getPayload().getInsertedCount());
        assertEquals(0, body.getPayload().getErrorCount());
    }

    @Test
    void uploadCsv_shouldRejectOverlappingAgeBandRowsForSameBucket() {
        UUID companyId = UUID.randomUUID();
        when(repository.findByOrganizationId(companyId)).thenReturn(new ArrayList<>());

        String csv = String.join("\n",
                "product_type,member_type,rate,effective_from,effective_to,pricing_model,age_band_min,age_band_max,sum_insured_amount,family_size_min,family_size_max,rate_source,gst_inclusive,gst_percentage,policy_id",
                "GMC,SELF,2500,2025-01-01,,AGE_BANDED,18,30,500000,,,NEGOTIATED,false,,",
                "GMC,SELF,3000,2025-01-01,,AGE_BANDED,25,35,500000,,,NEGOTIATED,false,,");
        MockMultipartFile file = new MockMultipartFile("file", "rate.csv", "text/csv", csv.getBytes());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PremiumRateTable>> saveCaptor = ArgumentCaptor.forClass(List.class);
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.uploadCsv(companyId, file);
        ResponseDto<PremiumRateTableCsvUploadResultDto> body = response.getBody();

        verify(repository).saveAll(saveCaptor.capture());
        List<PremiumRateTable> savedRows = saveCaptor.getValue();
        assertEquals(1, savedRows.size());
        assertEquals(1, body.getPayload().getInsertedCount());
        assertEquals(1, body.getPayload().getErrorCount());
        assertEquals("Overlapping age band range for the same plan/member/sum insured/effective dates.",
                body.getPayload().getErrors().get(0).getMessage());
    }

    @Test
    void uploadCsv_shouldParseCommaFormattedNumericValues() {
        UUID companyId = UUID.randomUUID();
        when(repository.findByOrganizationId(companyId)).thenReturn(List.of());

        String csv = String.join("\n",
                "product_type,member_type,rate,effective_from,effective_to,pricing_model,age_band_min,age_band_max,sum_insured_amount,family_size_min,family_size_max,rate_source,gst_inclusive,gst_percentage,policy_id",
                "GMC,SELF,\"4,000\",2026-04-01,2027-03-31,AGE_BANDED,18,25,\"300,000\",,,INSURER_CARD,FALSE,18.00,");
        MockMultipartFile file = new MockMultipartFile("file", "rate.csv", "text/csv", csv.getBytes());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PremiumRateTable>> saveCaptor = ArgumentCaptor.forClass(List.class);
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.uploadCsv(companyId, file);
        ResponseDto<PremiumRateTableCsvUploadResultDto> body = response.getBody();

        verify(repository).saveAll(saveCaptor.capture());
        List<PremiumRateTable> savedRows = saveCaptor.getValue();
        assertEquals(1, savedRows.size());
        assertEquals("4000", savedRows.get(0).getRate().stripTrailingZeros().toPlainString());
        assertEquals("300000", savedRows.get(0).getSumInsuredAmount().stripTrailingZeros().toPlainString());
        assertEquals(1, body.getPayload().getInsertedCount());
        assertEquals(0, body.getPayload().getErrorCount());
    }
}
