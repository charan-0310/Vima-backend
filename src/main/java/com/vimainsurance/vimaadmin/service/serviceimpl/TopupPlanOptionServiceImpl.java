package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentContextDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.TopupPlanOptionRequestDto;
import com.vimainsurance.vimaadmin.dto.TopupPlanOptionResponseDto;
import com.vimainsurance.vimaadmin.dto.TopupOptionsResponseDto;
import com.vimainsurance.vimaadmin.entity.TopupPlanOption;
import com.vimainsurance.vimaadmin.mapper.TopupPlanOptionMapper;
import com.vimainsurance.vimaadmin.repository.ITopupPlanOptionRepository;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;
import com.vimainsurance.vimaadmin.service.IEnrollmentService;
import com.vimainsurance.vimaadmin.service.IPremiumCalculationService;
import com.vimainsurance.vimaadmin.service.ITopupPlanOptionService;
import com.vimainsurance.vimaadmin.service.TopupPlanOptionCacheService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopupPlanOptionServiceImpl implements ITopupPlanOptionService {

    private static final Logger log = LoggerFactory.getLogger(TopupPlanOptionServiceImpl.class);
    private static final List<String> VALID_PLAN_TYPES = List.of("TOP_UP", "SUPER_TOP_UP");
    private static final List<String> VALID_PRICING_MODELS = List.of("AGE_BANDED", "FLAT");

    private final ITopupPlanOptionRepository repository;
    private final TopupPlanOptionCacheService cacheService;
    private final IEnrollmentService enrollmentService;
    private final IPremiumCalculationService premiumCalculationService;
    private final IEmployeePolicyMapService employeePolicyMapService;

    @Override
    public ResponseEntity<ResponseDto<List<TopupPlanOptionResponseDto>>> listByCompany(UUID companyId, String planType) {
        BaseResponse<List<TopupPlanOptionResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<TopupPlanOption> list = planType != null && !planType.isBlank()
                    ? repository.findByCompanyIdAndPlanType(companyId, planType)
                    : repository.findByCompanyId(companyId);
            List<TopupPlanOptionResponseDto> dtos = list.stream()
                    .map(TopupPlanOptionMapper::toResponseDto)
                    .toList();
            return responseObj.render(responseObj.formSuccessResponse("OK", dtos));
        } catch (Exception e) {
            log.error("listByCompany topup options error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to list top-up options"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<TopupPlanOptionResponseDto>> getById(UUID id) {
        BaseResponse<TopupPlanOptionResponseDto> responseObj = new BaseResponse<>();
        try {
            return repository.findById(id)
                    .map(TopupPlanOptionMapper::toResponseDto)
                    .map(dto -> responseObj.render(responseObj.formSuccessResponse("OK", dto)))
                    .orElseGet(() -> responseObj.render(responseObj.formErrorResponse(404, "Top-up plan option not found")));
        } catch (Exception e) {
            log.error("getById topup option error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get top-up option"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "topup_plan_options", entityType = "TOPUP_PLAN_OPTION", action = "CREATE")
    public ResponseEntity<ResponseDto<TopupPlanOptionResponseDto>> create(TopupPlanOptionRequestDto dto) {
        BaseResponse<TopupPlanOptionResponseDto> responseObj = new BaseResponse<>();
        try {
            validatePlanTypeAndPricingModel(dto);
            TopupPlanOption entity = TopupPlanOptionMapper.toEntity(dto);
            entity = repository.save(entity);
            cacheService.invalidate(dto.getCompanyId());
            return responseObj.render(responseObj.formSuccessResponse("Top-up option created", TopupPlanOptionMapper.toResponseDto(entity)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("create topup option error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to create top-up option"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "topup_plan_options", entityType = "TOPUP_PLAN_OPTION", action = "UPDATE")
    public ResponseEntity<ResponseDto<TopupPlanOptionResponseDto>> update(UUID id, UUID companyId, TopupPlanOptionRequestDto dto) {
        BaseResponse<TopupPlanOptionResponseDto> responseObj = new BaseResponse<>();
        try {
            validatePlanTypeAndPricingModel(dto);
            Optional<TopupPlanOption> opt = repository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Top-up plan option not found"));
            }
            TopupPlanOption entity = opt.get();
            if (!entity.getCompanyId().equals(companyId)) {
                return responseObj.render(responseObj.formErrorResponse(403, "Option does not belong to this company"));
            }
            entity.setPolicyId(dto.getPolicyId());
            entity.setPlanType(dto.getPlanType());
            entity.setName(dto.getName());
            entity.setDescription(dto.getDescription());
            entity.setInsurerName(dto.getInsurerName());
            entity.setDeductibleAmount(dto.getDeductibleAmount());
            entity.setSumInsuredOptions(TopupPlanOptionMapper.toSumInsuredJson(dto.getSumInsuredOptions()));
            entity.setPricingModel(dto.getPricingModel());
            if (dto.getCoversDependents() != null) entity.setCoversDependents(dto.getCoversDependents());
            if (dto.getCoversParents() != null) entity.setCoversParents(dto.getCoversParents());
            entity.setEffectiveFrom(dto.getEffectiveFrom());
            entity.setEffectiveTo(dto.getEffectiveTo());
            entity = repository.save(entity);
            cacheService.invalidate(companyId);
            return responseObj.render(responseObj.formSuccessResponse("Top-up option updated", TopupPlanOptionMapper.toResponseDto(entity)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("update topup option error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to update top-up option"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "topup_plan_options", entityType = "TOPUP_PLAN_OPTION", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> softDelete(UUID id, UUID companyId) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<TopupPlanOption> opt = repository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Top-up plan option not found"));
            }
            if (!opt.get().getCompanyId().equals(companyId)) {
                return responseObj.render(responseObj.formErrorResponse(403, "Option does not belong to this company"));
            }
            repository.softDeleteById(id);
            cacheService.invalidate(companyId);
            return responseObj.render(responseObj.formSuccessResponse("Top-up option deleted", null));
        } catch (Exception e) {
            log.error("softDelete topup option error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to delete top-up option"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<TopupOptionsResponseDto>> getActiveOptionsWithPremiumPreview(String enrollmentToken) {
        BaseResponse<TopupOptionsResponseDto> responseObj = new BaseResponse<>();
        try {
            ResponseEntity<ResponseDto<EnrollmentContextDto>> contextResp = enrollmentService.validateTokenAndGetContext(enrollmentToken);
            if (contextResp.getBody() == null || contextResp.getBody().getErrorCode() != null) {
                String msg = contextResp.getBody() != null ? contextResp.getBody().getMessage() : "Invalid token";
                Integer code = contextResp.getBody() != null ? contextResp.getBody().getErrorCode() : 400;
                return responseObj.render(responseObj.formErrorResponse(code != null ? code : 400, msg));
            }
            EnrollmentContextDto ctx = contextResp.getBody().getPayload();
            if (ctx == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "Invalid context"));
            }
            UUID companyId = ctx.getEnrollmentWindow() != null ? ctx.getEnrollmentWindow().getOrganizationId() : null;
            if (companyId == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "Enrollment window or organization not found"));
            }
            // Base GMC required: employee must have at least one GMC mapping before showing top-up
            ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> mapResp =
                    employeePolicyMapService.getMappingsForEmployeeFamily(ctx.getEmployeeId());
            List<EmployeePolicyMapResponseDto> maps =
                    mapResp.getBody() != null && mapResp.getBody().getPayload() != null
                            ? mapResp.getBody().getPayload() : List.of();
            boolean hasGmc = maps.stream()
                    .filter(m -> companyId.equals(m.getOrganizationId()))
                    .anyMatch(m -> "GMC".equalsIgnoreCase(m.getProductType()));
            if (!hasGmc) {
                return responseObj.render(responseObj.formErrorResponse(400, "Base GMC coverage is required before adding top-up options"));
            }
            List<TopupPlanOption> options = cacheService.getActiveOptionsForCompany(companyId);
            LocalDate employeeDob = ctx.getEmployee() != null ? ctx.getEmployee().getDateOfBirth() : null;
            int employeeAge = employeeDob != null ? Period.between(employeeDob, LocalDate.now()).getYears() : 0;
            List<IPremiumCalculationService.MemberInfo> memberList = List.of(
                    new IPremiumCalculationService.MemberInfo("self", employeeAge, employeeDob));

            List<TopupOptionsResponseDto.TopupOptionWithPreview> withPreviews = new ArrayList<>();
            for (TopupPlanOption option : options) {
                Map<BigDecimal, BigDecimal> premiumPreview = new LinkedHashMap<>();
                List<BigDecimal> sumInsuredList = TopupPlanOptionMapper.fromSumInsuredJson(option.getSumInsuredOptions());
                for (BigDecimal si : sumInsuredList) {
                    try {
                        var breakdown = premiumCalculationService.calculatePlanPremium(
                                companyId, option.getPlanType(), "INDIVIDUAL", si, memberList);
                        premiumPreview.put(si, breakdown.premium());
                    } catch (Exception e) {
                        log.debug("No premium rate for top-up option {} sumInsured {}: {}", option.getId(), si, e.getMessage());
                    }
                }
                withPreviews.add(TopupOptionsResponseDto.TopupOptionWithPreview.builder()
                        .id(option.getId())
                        .planType(option.getPlanType())
                        .name(option.getName())
                        .deductibleAmount(option.getDeductibleAmount())
                        .sumInsuredOptions(sumInsuredList)
                        .premiumPreviewPerOption(premiumPreview)
                        .build());
            }
            TopupOptionsResponseDto result = TopupOptionsResponseDto.builder().options(withPreviews).build();
            return responseObj.render(responseObj.formSuccessResponse("OK", result));
        } catch (Exception e) {
            log.error("getActiveOptionsWithPremiumPreview error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get top-up options"));
        }
    }

    private void validatePlanTypeAndPricingModel(TopupPlanOptionRequestDto dto) {
        if (dto.getPlanType() != null && !VALID_PLAN_TYPES.contains(dto.getPlanType().toUpperCase())) {
            throw new IllegalArgumentException("planType must be one of: " + VALID_PLAN_TYPES);
        }
        if (dto.getPricingModel() != null && !VALID_PRICING_MODELS.contains(dto.getPricingModel().toUpperCase())) {
            throw new IllegalArgumentException("pricingModel must be one of: " + VALID_PRICING_MODELS);
        }
    }
}
