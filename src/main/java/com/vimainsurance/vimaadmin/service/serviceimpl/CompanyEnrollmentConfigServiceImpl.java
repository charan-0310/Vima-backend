package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigRequestDto;
import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.CompanyEnrollmentConfig;
import com.vimainsurance.vimaadmin.repository.ICompanyEnrollmentConfigRepository;
import com.vimainsurance.vimaadmin.service.ICompanyEnrollmentConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyEnrollmentConfigServiceImpl implements ICompanyEnrollmentConfigService {

    private static final Logger log = LoggerFactory.getLogger(CompanyEnrollmentConfigServiceImpl.class);

    private final ICompanyEnrollmentConfigRepository repository;

    @Override
    public ResponseEntity<ResponseDto<CompanyEnrollmentConfigResponseDto>> getByCompanyId(UUID companyId) {
        BaseResponse<CompanyEnrollmentConfigResponseDto> responseObj = new BaseResponse<>();
        try {
            CompanyEnrollmentConfigResponseDto dto = getConfigForCompany(companyId);
            return responseObj.render(responseObj.formSuccessResponse("OK", dto));
        } catch (Exception e) {
            log.error("getByCompanyId enrollment config error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get enrollment config"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "company_enrollment_config", entityType = "COMPANY_ENROLLMENT_CONFIG", action = "UPDATE")
    public ResponseEntity<ResponseDto<CompanyEnrollmentConfigResponseDto>> createOrUpdate(UUID companyId, CompanyEnrollmentConfigRequestDto dto) {
        BaseResponse<CompanyEnrollmentConfigResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<CompanyEnrollmentConfig> opt = repository.findByOrganizationId(companyId);
            CompanyEnrollmentConfig entity;
            String action;
            if (opt.isPresent()) {
                entity = opt.get();
                action = "UPDATE";
                if (dto.getParentCoverageEnabled() != null) entity.setParentCoverageEnabled(dto.getParentCoverageEnabled());
                if (dto.getInLawCoverageEnabled() != null) entity.setInLawCoverageEnabled(dto.getInLawCoverageEnabled());
                if (dto.getMaxParents() != null) entity.setMaxParents(dto.getMaxParents());
                if (dto.getMaxInLaws() != null) entity.setMaxInLaws(dto.getMaxInLaws());
                if (dto.getParentAgeLimit() != null) entity.setParentAgeLimit(dto.getParentAgeLimit());
            } else {
                action = "CREATE";
                entity = CompanyEnrollmentConfig.builder()
                        .organizationId(companyId)
                        .parentCoverageEnabled(Boolean.TRUE.equals(dto.getParentCoverageEnabled()))
                        .inLawCoverageEnabled(Boolean.TRUE.equals(dto.getInLawCoverageEnabled()))
                        .maxParents(dto.getMaxParents() != null ? dto.getMaxParents() : 0)
                        .maxInLaws(dto.getMaxInLaws() != null ? dto.getMaxInLaws() : 0)
                        .parentAgeLimit(dto.getParentAgeLimit())
                        .build();
            }
            entity = repository.save(entity);
            return responseObj.render(responseObj.formSuccessResponse("Enrollment config saved", toResponseDto(entity)));
        } catch (Exception e) {
            log.error("createOrUpdate enrollment config error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to save enrollment config"));
        }
    }

    @Override
    public CompanyEnrollmentConfigResponseDto getConfigForCompany(UUID companyId) {
        return repository.findByOrganizationId(companyId)
                .map(CompanyEnrollmentConfigServiceImpl::toResponseDto)
                .orElseGet(() -> CompanyEnrollmentConfigResponseDto.builder()
                        .id(null)
                        .organizationId(companyId)
                        .parentCoverageEnabled(false)
                        .inLawCoverageEnabled(false)
                        .maxParents(0)
                        .maxInLaws(0)
                        .parentAgeLimit(null)
                        .createdAt(null)
                        .updatedAt(null)
                        .build());
    }

    private static CompanyEnrollmentConfigResponseDto toResponseDto(CompanyEnrollmentConfig e) {
        return CompanyEnrollmentConfigResponseDto.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .parentCoverageEnabled(e.getParentCoverageEnabled())
                .inLawCoverageEnabled(e.getInLawCoverageEnabled())
                .maxParents(e.getMaxParents())
                .maxInLaws(e.getMaxInLaws())
                .parentAgeLimit(e.getParentAgeLimit())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
