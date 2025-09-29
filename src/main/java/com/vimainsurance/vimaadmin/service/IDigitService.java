package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.CommonResponseDto;
import com.vimainsurance.vimaadmin.dto.digit.DigitPolicyStatusReqDto;
import com.vimainsurance.vimaadmin.dto.digit.QQGlowWrapper;

public interface IDigitService {
    public CommonResponseDto getAuth();

    public CommonResponseDto getQuickQuote(QQGlowWrapper glowWrapper);

    public CommonResponseDto getPolicyStatus(DigitPolicyStatusReqDto reqDto);

    public CommonResponseDto getAgentRedirectionUrl(String applicationId);

    public CommonResponseDto getPaymentUrl(String applicationId);

    public CommonResponseDto getVideoVerification(String applicationId);

    public CommonResponseDto getPolicyPdf(String polciyNumber);

    public CommonResponseDto getQuotePdf(String applicationId);


}
