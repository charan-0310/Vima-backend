package com.vimainsurance.vimaadmin.dto.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimLetter {
    private String letterType;
    private String base64Content;
    private String fileName;
}
