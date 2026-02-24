package com.vimainsurance.vimaadmin.dto.claim;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for GET claim documents: list with download URLs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDocumentListResponse {
    private List<ClaimDocumentDto> documents;
}
