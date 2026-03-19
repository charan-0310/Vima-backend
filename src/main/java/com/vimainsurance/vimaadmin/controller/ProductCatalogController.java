package com.vimainsurance.vimaadmin.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ProductCatalogReorderRequestDto;
import com.vimainsurance.vimaadmin.dto.ProductCatalogRequestDto;
import com.vimainsurance.vimaadmin.dto.ProductCatalogResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IProductCatalogService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/product-catalog")
public class ProductCatalogController {

    private final IProductCatalogService productCatalogService;

    public ProductCatalogController(IProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<ProductCatalogResponseDto>>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveTo) {
        return productCatalogService.list(organizationId, productType, isActive, effectiveFrom, effectiveTo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> getById(@PathVariable UUID id) {
        return productCatalogService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> create(@RequestBody @Valid ProductCatalogRequestDto dto) {
        return productCatalogService.create(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody @Valid ProductCatalogRequestDto dto) {
        return productCatalogService.update(id, dto);
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<ProductCatalogResponseDto>>> reorder(@RequestBody @Valid ProductCatalogReorderRequestDto dto) {
        return productCatalogService.reorder(dto);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> activate(@PathVariable UUID id) {
        return productCatalogService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<ProductCatalogResponseDto>> deactivate(@PathVariable UUID id) {
        return productCatalogService.deactivate(id);
    }
}
