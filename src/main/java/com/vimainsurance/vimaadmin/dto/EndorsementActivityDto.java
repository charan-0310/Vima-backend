package com.vimainsurance.vimaadmin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndorsementActivityDto {
    private Long endorsementCount;
    private Long completedEndorsements;
    private Long pendingEndorsements;
}
