package com.vimainsurance.vimaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for updating quote status
 */
@Getter
@Setter
public class QuoteStatusUpdateDto {
    
    /**
     * The quote ID to update
     */
    @NotBlank(message = "Quote ID is required")
    private String quoteId;
    
    /**
     * The new status to set
     */
    @NotBlank(message = "Status is required")
    private String status;

}
