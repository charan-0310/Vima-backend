package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ProductCatalogReorderRequestDto;
import com.vimainsurance.vimaadmin.dto.ProductCatalogRequestDto;
import com.vimainsurance.vimaadmin.dto.ProductCatalogResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.entity.ProductCatalog;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.mapper.ProductCatalogMapper;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IProductCatalogRepository;
import com.vimainsurance.vimaadmin.service.IProductCatalogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements IProductCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogServiceImpl.class);

    private static final List<String> PARENT_PRODUCT_TYPES = List.of("PARENT_COVER", "PARENT_IN_LAW_COVER");

    private final IProductCatalogRepository repository;
    private final IPolicyRepository policyRepository;

    @Override
    public ResponseEntity<ResponseDto<List<ProductCatalogResponseDto>>> list(
            UUID organizationId,
            String productType,
            Boolean isActive,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {
        BaseResponse<List<ProductCatalogResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<ProductCatalog> list;
            if (organizationId == null) {
                list = repository.findAll();
            } else {
                boolean activeOnly = isActive == null || isActive;
                list = activeOnly
                        ? repository.findByOrganizationIdAndIsActive(organizationId, true)
                        : repository.findByOrganizationId(organizationId);
            }

            List<ProductCatalogResponseDto> dtos = list.stream()
                    .filter(pc -> organizationId == null || organizationId.equals(pc.getOrganizationId()))
                    .filter(pc -> isActive == null || Boolean.TRUE.equals(isActive) == Boolean.TRUE.equals(pc.getIsActive()))
                    .filter(pc -> productType == null || productType.isBlank() || productType.equalsIgnoreCase(pc.getProductType()))
                    .filter(pc -> effectiveFrom == null || !pc.getEffectiveFrom().isAfter(effectiveFrom))
                    .filter(pc -> effectiveTo == null || pc.getEffectiveTo() == null || !pc.getEffectiveTo().isBefore(effectiveTo))
                    .sorted((a, b) -> Integer.compare(a.getDisplayOrder() != null ? a.getDisplayOrder() : 0, b.getDisplayOrder() != null ? b.getDisplayOrder() : 0))
                    .map(ProductCatalogMapper::toResponseDto)
                    .toList();
            return responseObj.render(responseObj.formSuccessResponse("OK", dtos));
        } catch (Exception e) {
            log.error("list product catalog error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to list product catalog"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> getById(UUID id) {
        BaseResponse<ProductCatalogResponseDto> responseObj = new BaseResponse<>();
        try {
            return repository.findById(id)
                    .map(ProductCatalogMapper::toResponseDto)
                    .map(dto -> responseObj.render(responseObj.formSuccessResponse("OK", dto)))
                    .orElseGet(() -> responseObj.render(responseObj.formErrorResponse(404, "Product catalog not found")));
        } catch (Exception e) {
            log.error("getById product catalog error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to get product catalog"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "product_catalog", entityType = "PRODUCT_CATALOG", action = "CREATE")
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> create(ProductCatalogRequestDto dto) {
        BaseResponse<ProductCatalogResponseDto> responseObj = new BaseResponse<>();
        try {
            validateForCreateOrUpdate(dto, null);
            ProductCatalog entity = ProductCatalogMapper.toEntity(dto);
            entity = repository.save(entity);
            return responseObj.render(responseObj.formSuccessResponse("Product catalog created", ProductCatalogMapper.toResponseDto(entity)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("create product catalog error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to create product catalog"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "product_catalog", entityType = "PRODUCT_CATALOG", action = "UPDATE")
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> update(UUID id, ProductCatalogRequestDto dto) {
        BaseResponse<ProductCatalogResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<ProductCatalog> opt = repository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Product catalog not found"));
            }
            validateForCreateOrUpdate(dto, id);
            ProductCatalog entity = opt.get();
            if (!entity.getOrganizationId().equals(dto.getOrganizationId())) {
                return responseObj.render(responseObj.formErrorResponse(400, "organizationId cannot be changed"));
            }
            entity.setProductType(dto.getProductType());
            entity.setName(dto.getName());
            entity.setIsMandatory(dto.getIsMandatory() != null ? dto.getIsMandatory() : false);
            entity.setPricingModel(dto.getPricingModel());
            entity.setCoverageOptions(dto.getCoverageOptions());
            entity.setPremiumPreviewOptions(dto.getPremiumPreviewOptions());
            entity.setCoveredRelationships(dto.getCoveredRelationships());
            entity.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
            entity.setPolicyId(dto.getPolicyId());
            entity.setGradeFilter(dto.getGradeFilter());
            entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
            entity.setEffectiveFrom(dto.getEffectiveFrom());
            entity.setEffectiveTo(dto.getEffectiveTo());
            entity = repository.save(entity);
            return responseObj.render(responseObj.formSuccessResponse("Product catalog updated", ProductCatalogMapper.toResponseDto(entity)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("update product catalog error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to update product catalog"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "product_catalog", entityType = "PRODUCT_CATALOG", action = "UPDATE")
    public ResponseEntity<ResponseDto<List<ProductCatalogResponseDto>>> reorder(ProductCatalogReorderRequestDto dto) {
        BaseResponse<List<ProductCatalogResponseDto>> responseObj = new BaseResponse<>();
        try {
            for (ProductCatalogReorderRequestDto.ReorderItem item : dto.getItems()) {
                repository.updateDisplayOrder(item.getId(), item.getDisplayOrder());
            }
            UUID orgId = null;
            if (!dto.getItems().isEmpty()) {
                Optional<ProductCatalog> first = repository.findById(dto.getItems().get(0).getId());
                orgId = first.map(ProductCatalog::getOrganizationId).orElse(null);
            }
            if (orgId != null) {
                List<ProductCatalog> list = repository.findByOrganizationIdOrderByDisplayOrderAsc(orgId);
                List<ProductCatalogResponseDto> dtos = list.stream().map(ProductCatalogMapper::toResponseDto).toList();
                return responseObj.render(responseObj.formSuccessResponse("Order updated", dtos));
            }
            return responseObj.render(responseObj.formSuccessResponse("Order updated", List.of()));
        } catch (Exception e) {
            log.error("reorder product catalog error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to reorder product catalog"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "product_catalog", entityType = "PRODUCT_CATALOG", action = "UPDATE")
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> activate(UUID id) {
        return setActive(id, true);
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "product_catalog", entityType = "PRODUCT_CATALOG", action = "UPDATE")
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> deactivate(UUID id) {
        return setActive(id, false);
    }

    private ResponseEntity<ResponseDto<ProductCatalogResponseDto>> setActive(UUID id, boolean active) {
        BaseResponse<ProductCatalogResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<ProductCatalog> opt = repository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Product catalog not found"));
            }
            ProductCatalog entity = opt.get();
            entity.setIsActive(active);
            entity = repository.save(entity);
            return responseObj.render(responseObj.formSuccessResponse(active ? "Product catalog activated" : "Product catalog deactivated", ProductCatalogMapper.toResponseDto(entity)));
        } catch (Exception e) {
            log.error("setActive product catalog error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to update product catalog"));
        }
    }

    private void validateForCreateOrUpdate(ProductCatalogRequestDto dto, UUID existingId) {
        if (dto.getOrganizationId() == null) {
            throw new IllegalArgumentException("organizationId is required");
        }
        if (dto.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }

        if (PARENT_PRODUCT_TYPES.contains(dto.getProductType())) {
            List<Policy> activePolicies = policyRepository.findByOrganizationIdAndStatus(dto.getOrganizationId(), PolicyStatus.ACTIVE);
            boolean hasEsc = activePolicies.stream()
                    .anyMatch(p -> p.getProductType() != null && ("GMC".equalsIgnoreCase(p.getProductType().getValue()) || "GHI".equalsIgnoreCase(p.getProductType().getValue())));
            if (!hasEsc) {
                throw new IllegalArgumentException("PARENT add-on requires an active ESC (GMC/GHI) policy for the organization");
            }
        }

        if (dto.getPolicyId() != null) {
            Optional<Policy> policyOpt = policyRepository.findById(dto.getPolicyId());
            if (policyOpt.isEmpty()) {
                throw new IllegalArgumentException("policyId does not exist");
            }
            Policy policy = policyOpt.get();
            if (policy.getOrganizationId() == null || !policy.getOrganizationId().equals(dto.getOrganizationId())) {
                throw new IllegalArgumentException("policyId must belong to the same organization");
            }
        }
    }
}
