package com.vimainsurance.vimaadmin.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.CdBalanceResponseDto;
import com.vimainsurance.vimaadmin.dto.CdBalanceTransactionRequestDto;
import com.vimainsurance.vimaadmin.dto.CdBalanceTransactionResponseDto;
import com.vimainsurance.vimaadmin.dto.EndorsementCdBalanceEntryDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.enums.EndorsementType;

public interface ICdBalanceService {

    ResponseEntity<ResponseDto<CdBalanceResponseDto>> getCdBalance(UUID cdAccountId);

    ResponseEntity<ResponseDto<CdBalanceTransactionResponseDto>> recordTransaction(
            CdBalanceTransactionRequestDto requestDto,
            MultipartFile[] files);

    void recordEndorsementCdBalanceEntries(
            UUID endorsementId,
            UUID organizationId,
            List<EndorsementCdBalanceEntryDto> entries,
            EndorsementType endorsementType,
            String performedBy);

    ResponseEntity<ResponseDto<List<CdBalanceTransactionResponseDto>>> getTransactionLedger(
            UUID cdAccountId,
            Long policyId,
            int page,
            int size,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo);

    ResponseEntity<ResponseDto<CdBalanceResponseDto>> recalculateBalance(UUID cdAccountId);
}
