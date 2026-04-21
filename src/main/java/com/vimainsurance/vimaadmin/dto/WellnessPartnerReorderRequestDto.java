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
public class WellnessPartnerReorderRequestDto {

    @NotNull(message = "organizationId is required")
    private UUID organizationId;

    @Valid
    @NotEmpty(message = "partnerOrders is required")
    private List<PartnerOrderItem> partnerOrders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnerOrderItem {
        @NotNull(message = "id is required")
        private UUID id;
        @NotNull(message = "displayOrder is required")
        private Integer displayOrder;
    }
}
