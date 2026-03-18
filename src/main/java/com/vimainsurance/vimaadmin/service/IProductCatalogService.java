package com.vimainsurance.vimaadmin.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ProductCatalogReorderRequestDto;
import com.vimainsurance.vimaadmin.dto.ProductCatalogRequestDto;
import com.vimainsurance.vimaadmin.dto.ProductCatalogResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IProductCatalogService {

    ResponseEntity<ResponseDto<List<ProductCatalogResponseDto>>> list(
            UUID organizationId,
            String productType,
            Boolean isActive,
            LocalDate effectiveFrom,
            LocalDate effectiveTo);

    ResponseEntity<ResponseDto<ProductCatalogResponseDto>> getById(UUID id);

    ResponseEntity<ResponseDto<ProductCatalogResponseDto>> create(ProductCatalogRequestDto dto);

    ResponseEntity<ResponseDto<ProductCatalogResponseDto>> update(UUID id, ProductCatalogRequestDto dto);

    ResponseEntity<ResponseDto<List<ProductCatalogResponseDto>>> reorder(ProductCatalogReorderRequestDto dto);

    ResponseEntity<ResponseDto<ProductCatalogResponseDto>> activate(UUID id);

    ResponseEntity<ResponseDto<ProductCatalogResponseDto>> deactivate(UUID id);
}
