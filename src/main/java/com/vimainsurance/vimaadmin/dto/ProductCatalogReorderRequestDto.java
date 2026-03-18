package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogReorderRequestDto {

    @Valid
    @NotEmpty(message = "items is required")
    private List<ReorderItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderItem {
        @NotNull
        private UUID id;
        @NotNull
        private Integer displayOrder;
    }
}
