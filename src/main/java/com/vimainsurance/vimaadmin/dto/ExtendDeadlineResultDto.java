package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of bulk extend-deadline: extended count and failed count. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendDeadlineResultDto {

    private int extended;
    private int failed;
}
