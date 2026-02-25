package com.vimainsurance.vimaadmin.dto.claim;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryCreateRequest {
    private String queryText;
    /** Query date (ISO date or date-time). Service uses date part for ClaimQuery.queryDate. */
    private LocalDate queryDate;
    private LocalDateTime queryDateTimestamp;
    /** Optional insurer system reference. */
    private String insurerSysId;
    /** When true, only create the query record; do not change claim status (e.g. when status will be set to INFO_REQUESTED/REJECTED_BY_ADMIN via status API). */
    private Boolean createOnlyWithoutStatusChange;
}
