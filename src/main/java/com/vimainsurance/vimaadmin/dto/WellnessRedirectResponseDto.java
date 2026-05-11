package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessRedirectResponseDto {
    /**
     * Final URL when set. When {@link #partnerExchangeToken} is set (browser-exchange mode), this may be null
     * until the SPA completes the partner POST.
     */
    private String redirectUrl;
    private String opensIn;

    /** Present when {@code mantracare.employee-redirect-browser-exchange=true}: SPA POSTs token to {@link #partnerExchangePostUrl}. */
    private String partnerExchangeToken;

    private String partnerExchangePostUrl;

    private String partnerSessionCookie;
}
