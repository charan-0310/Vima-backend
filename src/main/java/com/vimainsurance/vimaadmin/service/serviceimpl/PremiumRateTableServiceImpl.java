package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.PremiumRateTableCsvUploadResultDto;
import com.vimainsurance.vimaadmin.dto.PremiumRateTableRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumRateTableResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.PremiumRateTable;
import com.vimainsurance.vimaadmin.enums.PricingModel;
import com.vimainsurance.vimaadmin.enums.RateSource;
import com.vimainsurance.vimaadmin.mapper.PremiumRateTableMapper;
import com.vimainsurance.vimaadmin.repository.IPremiumRateTableRepository;
import com.vimainsurance.vimaadmin.service.IPremiumRateTableService;
import com.vimainsurance.vimaadmin.service.PremiumRateTableCacheService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PremiumRateTableServiceImpl implements IPremiumRateTableService {

    private static final Logger log = LoggerFactory.getLogger(PremiumRateTableServiceImpl.class);

    private final IPremiumRateTableRepository repository;
    private final PremiumRateTableCacheService cacheService;

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "premium_rate_tables", entityType = "PREMIUM_RATE_TABLE", action = "CREATE")
    public ResponseEntity<ResponseDto<PremiumRateTableResponseDto>> create(PremiumRateTableRequestDto dto) {
        BaseResponse<PremiumRateTableResponseDto> responseObj = new BaseResponse<>();
        try {
            PremiumRateTable entity = PremiumRateTableMapper.toEntity(dto);
            entity = repository.save(entity);
            cacheService.invalidate(dto.getCompanyId());
            return responseObj.render(responseObj.formSuccessResponse("Rate table created", PremiumRateTableMapper.toResponseDto(entity)));
        } catch (Exception e) {
            log.error("create premium rate table error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to create rate table"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "premium_rate_tables", entityType = "PREMIUM_RATE_TABLE", action = "UPDATE")
    public ResponseEntity<ResponseDto<PremiumRateTableResponseDto>> update(UUID id, PremiumRateTableRequestDto dto) {
        BaseResponse<PremiumRateTableResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<PremiumRateTable> opt = repository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Rate table not found"));
            }
            PremiumRateTable entity = opt.get();
            entity.setOrganizationId(dto.getCompanyId());
            entity.setPolicyId(dto.getPolicyId());
            entity.setProductType(dto.getProductType());
            entity.setMemberType(dto.getMemberType());
            entity.setAgeBandMin(dto.getAgeBandMin());
            entity.setAgeBandMax(dto.getAgeBandMax());
            entity.setRate(dto.getRate());
            entity.setEffectiveFrom(dto.getEffectiveFrom());
            entity.setEffectiveTo(dto.getEffectiveTo());
            entity.setPricingModel(dto.getPricingModel());
            entity.setSumInsuredAmount(dto.getSumInsuredAmount());
            entity.setFamilySizeMin(dto.getFamilySizeMin());
            entity.setFamilySizeMax(dto.getFamilySizeMax());
            entity.setRateSource(dto.getRateSource());
            entity.setGstInclusive(Boolean.TRUE.equals(dto.getGstInclusive()));
            entity.setGstPercentage(dto.getGstPercentage());
            entity = repository.save(entity);
            cacheService.invalidate(dto.getCompanyId());
            return responseObj.render(responseObj.formSuccessResponse("Rate table updated", PremiumRateTableMapper.toResponseDto(entity)));
        } catch (Exception e) {
            log.error("update premium rate table error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to update rate table"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "premium_rate_tables", entityType = "PREMIUM_RATE_TABLE", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> softDelete(UUID id) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<PremiumRateTable> opt = repository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Rate table not found"));
            }
            UUID companyId = opt.get().getOrganizationId();
            int updated = repository.softDeleteById(id);
            if (updated > 0) {
                cacheService.invalidate(companyId);
                return responseObj.render(responseObj.formSuccessResponse("Rate table deleted", "OK"));
            }
            return responseObj.render(responseObj.formErrorResponse("Failed to delete rate table"));
        } catch (Exception e) {
            log.error("softDelete premium rate table error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to delete rate table"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<PremiumRateTableResponseDto>> getById(UUID id) {
        BaseResponse<PremiumRateTableResponseDto> responseObj = new BaseResponse<>();
        try {
            return repository.findById(id)
                    .map(PremiumRateTableMapper::toResponseDto)
                    .map(dto -> responseObj.render(responseObj.formSuccessResponse("OK", dto)))
                    .orElseGet(() -> responseObj.render(responseObj.formErrorResponse(404, "Rate table not found")));
        } catch (Exception e) {
            log.error("getById premium rate table error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get rate table"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Page<PremiumRateTableResponseDto>>> list(
            UUID companyId,
            String planType,
            String pricingModel,
            Pageable pageable) {
        BaseResponse<Page<PremiumRateTableResponseDto>> responseObj = new BaseResponse<>();
        try {
            Specification<PremiumRateTable> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (companyId != null) {
                    predicates.add(cb.equal(root.get("organizationId"), companyId));
                }
                if (planType != null && !planType.isBlank()) {
                    predicates.add(cb.equal(root.get("productType"), planType));
                }
                if (pricingModel != null && !pricingModel.isBlank()) {
                    try {
                        predicates.add(cb.equal(root.get("pricingModel"), PricingModel.fromValue(pricingModel)));
                    } catch (IllegalArgumentException ignored) {
                        // ignore invalid enum
                    }
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            Page<PremiumRateTable> page = repository.findAll(spec, pageable);
            List<PremiumRateTableResponseDto> dtos = page.getContent().stream()
                    .map(PremiumRateTableMapper::toResponseDto)
                    .toList();
            Page<PremiumRateTableResponseDto> result = new PageImpl<>(dtos, pageable, page.getTotalElements());
            return responseObj.render(responseObj.formSuccessResponse("OK", result));
        } catch (Exception e) {
            log.error("list premium rate tables error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to list rate tables"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "premium_rate_tables", entityType = "PREMIUM_RATE_TABLE", action = "CREATE")
    public ResponseEntity<ResponseDto<PremiumRateTableCsvUploadResultDto>> uploadCsv(UUID companyId, MultipartFile file) {
        BaseResponse<PremiumRateTableCsvUploadResultDto> responseObj = new BaseResponse<>();
        List<PremiumRateTableCsvUploadResultDto.RowError> errors = new ArrayList<>();
        List<PremiumRateTable> toSave = new ArrayList<>();
        try {
            if (file == null || file.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("File is required"));
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String headerLine = reader.readLine();
                if (headerLine == null || headerLine.isBlank()) {
                    return responseObj.render(responseObj.formErrorResponse("CSV is empty"));
                }
                String[] headers = parseCsvLine(headerLine);
                int totalRows = 0;
                java.util.Set<String> seenPlanMemberKeys = new java.util.HashSet<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    totalRows++;
                    if (line.isBlank()) continue;
                    try {
                        PremiumRateTable row = parseRow(companyId, headers, parseCsvLine(line), totalRows, errors);
                        if (row != null) {
                            String planType = row.getProductType() != null ? row.getProductType().trim().toUpperCase() : "";
                            String memberType = row.getMemberType() != null ? row.getMemberType().trim().toUpperCase() : "";
                            String key = planType + "|" + memberType;
                            if (!seenPlanMemberKeys.add(key)) {
                                errors.add(PremiumRateTableCsvUploadResultDto.RowError.builder()
                                        .rowIndex(totalRows)
                                        .message("Duplicate plan type + member type (e.g. same plan and SELF twice). Each combination must appear only once.")
                                        .build());
                            } else {
                                toSave.add(row);
                            }
                        }
                    } catch (Exception e) {
                        errors.add(PremiumRateTableCsvUploadResultDto.RowError.builder()
                                .rowIndex(totalRows)
                                .message(e.getMessage())
                                .build());
                    }
                }
                repository.saveAll(toSave);
                if (!toSave.isEmpty()) {
                    cacheService.invalidate(companyId);
                }
                PremiumRateTableCsvUploadResultDto result = PremiumRateTableCsvUploadResultDto.builder()
                        .totalRows(totalRows)
                        .insertedCount(toSave.size())
                        .errorCount(errors.size())
                        .errors(errors)
                        .build();
                return responseObj.render(responseObj.formSuccessResponse("CSV processed", result));
            }
        } catch (Exception e) {
            log.error("uploadCsv premium rate table error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("CSV upload failed: " + e.getMessage()));
        }
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if ((c == ',' && !inQuotes) || c == '\t') {
                result.add(cell.toString().trim());
                cell = new StringBuilder();
            } else {
                cell.append(c);
            }
        }
        result.add(cell.toString().trim());
        return result.toArray(new String[0]);
    }

    private PremiumRateTable parseRow(UUID companyId, String[] headers, String[] values, int rowNum,
            List<PremiumRateTableCsvUploadResultDto.RowError> errors) {
        if (values.length < headers.length - 2) {
            errors.add(PremiumRateTableCsvUploadResultDto.RowError.builder()
                    .rowIndex(rowNum)
                    .message("Column count mismatch")
                    .build());
            return null;
        }
        String productType = getVal(headers, values, "product_type", "productType");
        String rateStr = getVal(headers, values, "rate", "rate");
        String effectiveFromStr = getVal(headers, values, "effective_from", "effectiveFrom");
        if (productType == null || productType.isBlank()) {
            errors.add(PremiumRateTableCsvUploadResultDto.RowError.builder().rowIndex(rowNum).message("product_type required").build());
            return null;
        }
        if (rateStr == null || rateStr.isBlank()) {
            errors.add(PremiumRateTableCsvUploadResultDto.RowError.builder().rowIndex(rowNum).message("rate required").build());
            return null;
        }
        if (effectiveFromStr == null || effectiveFromStr.isBlank()) {
            errors.add(PremiumRateTableCsvUploadResultDto.RowError.builder().rowIndex(rowNum).message("effective_from required").build());
            return null;
        }
        BigDecimal rate;
        try {
            rate = new BigDecimal(rateStr.trim());
        } catch (NumberFormatException e) {
            errors.add(PremiumRateTableCsvUploadResultDto.RowError.builder().rowIndex(rowNum).message("Invalid rate").build());
            return null;
        }
        LocalDate effectiveFrom = parseDateFlexible(effectiveFromStr.trim());
        if (effectiveFrom == null) {
            errors.add(PremiumRateTableCsvUploadResultDto.RowError.builder().rowIndex(rowNum).message("Invalid effective_from date (use yyyy-MM-dd or dd/MM/yyyy)").build());
            return null;
        }
        String effectiveToStr = getVal(headers, values, "effective_to", "effectiveTo");
        LocalDate effectiveTo = null;
        if (effectiveToStr != null && !effectiveToStr.isBlank()) {
            effectiveTo = parseDateFlexible(effectiveToStr.trim());
        }
        String memberType = getVal(headers, values, "member_type", "memberType");
        String pricingModelStr = getVal(headers, values, "pricing_model", "pricingModel");
        PricingModel pricingModel = null;
        if (pricingModelStr != null && !pricingModelStr.isBlank()) {
            try {
                pricingModel = PricingModel.fromValue(pricingModelStr);
            } catch (IllegalArgumentException ignored) {
            }
        }
        String rateSourceStr = getVal(headers, values, "rate_source", "rateSource");
        RateSource rateSource = null;
        if (rateSourceStr != null && !rateSourceStr.isBlank()) {
            try {
                rateSource = RateSource.fromValue(rateSourceStr);
            } catch (IllegalArgumentException ignored) {
            }
        }
        Integer ageBandMin = parseInteger(getVal(headers, values, "age_band_min", "ageBandMin"));
        Integer ageBandMax = parseInteger(getVal(headers, values, "age_band_max", "ageBandMax"));
        BigDecimal sumInsuredAmount = parseDecimal(getVal(headers, values, "sum_insured_amount", "sumInsuredAmount"));
        Integer familySizeMin = parseInteger(getVal(headers, values, "family_size_min", "familySizeMin"));
        Integer familySizeMax = parseInteger(getVal(headers, values, "family_size_max", "familySizeMax"));
        Boolean gstInclusive = "true".equalsIgnoreCase(getVal(headers, values, "gst_inclusive", "gstInclusive"));
        BigDecimal gstPercentage = parseDecimal(getVal(headers, values, "gst_percentage", "gstPercentage"));
        String policyIdStr = getVal(headers, values, "policy_id", "policyId");
        Long policyId = null;
        if (policyIdStr != null && !policyIdStr.isBlank()) {
            try {
                policyId = Long.parseLong(policyIdStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return PremiumRateTable.builder()
                .organizationId(companyId)
                .policyId(policyId)
                .productType(productType)
                .memberType(memberType)
                .ageBandMin(ageBandMin)
                .ageBandMax(ageBandMax)
                .rate(rate)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .pricingModel(pricingModel)
                .sumInsuredAmount(sumInsuredAmount)
                .familySizeMin(familySizeMin)
                .familySizeMax(familySizeMax)
                .rateSource(rateSource)
                .gstInclusive(Boolean.TRUE.equals(gstInclusive))
                .gstPercentage(gstPercentage)
                .isDeleted(false)
                .build();
    }

    private static String getVal(String[] headers, String[] values, String... keys) {
        for (String key : keys) {
            for (int i = 0; i < headers.length && i < values.length; i++) {
                if (key.equalsIgnoreCase(headers[i].trim())) {
                    String v = values[i];
                    return (v != null && !v.isBlank()) ? v.trim() : null;
                }
            }
        }
        return null;
    }

    private static Integer parseInteger(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse a date string accepting multiple formats (e.g. yyyy-MM-dd, dd/MM/yyyy, dd/MM/yy from Excel).
     * Returns null if none of the formats match.
     */
    private static LocalDate parseDateFlexible(String s) {
        if (s == null || s.isBlank()) return null;
        String trimmed = s.trim();
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("d/M/yy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
        };
        for (DateTimeFormatter f : formatters) {
            try {
                return LocalDate.parse(trimmed, f);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }
}
