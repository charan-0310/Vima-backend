package com.vimainsurance.vimaadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.digit.DigitPolicyStatusReqDto;
import com.vimainsurance.vimaadmin.dto.digit.QQGlowWrapper;
import com.vimainsurance.vimaadmin.dto.QuickQuoteRequestDto;
import com.vimainsurance.vimaadmin.mapper.VendorDigitRequestMapper;
import com.vimainsurance.vimaadmin.service.IDigitService;
import com.vimainsurance.vimaadmin.dto.CommonResponseDto;
import com.vimainsurance.vimaadmin.dto.QuotesRequestDto;
import com.vimainsurance.vimaadmin.service.IQuoteService;
import java.time.LocalDate;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/digit")
public class DigitController {

    @Autowired
    private IDigitService digitService;
    
    @Autowired
    private VendorDigitRequestMapper vendorDigitRequestMapper;

    @Autowired
    private IQuoteService quoteService;

    @PostMapping("/auth")
    public Object getAuth() {
        return digitService.getAuth();
    }

    @PostMapping("/quickquote")
    public Object getQuickQuote(@RequestBody QuickQuoteRequestDto requestDto, @RequestParam(required = false) String custId) {
        QQGlowWrapper glowWrapper = (QQGlowWrapper) vendorDigitRequestMapper.map(requestDto);
        CommonResponseDto response = digitService.getQuickQuote(glowWrapper);

        if (custId != null && !custId.isBlank()) {
            try {
                QuotesRequestDto qr = new QuotesRequestDto();
                qr.setQuoteId(response.getPayload().get("applicationId").toString());
                qr.setCoverageAmount(requestDto.getSumAssured() != null && !requestDto.getSumAssured().isEmpty() ? String.valueOf(requestDto.getSumAssured().get(0)) : null);
                qr.setBestPremium(extractBestPremium(response.getPayload()));
                qr.setStatus("Payment Pending");
                qr.setCreatedDate(LocalDate.now());
                qr.setCustomer(custId);
                // qr.setCompanies(null); // set if needed
                quoteService.create(qr);
            } catch (Exception ignore) {
            }
        }

        return response;
    }

    private String extractBestPremium(Map<String, Object> payload) {
        if (payload == null) return null;
        Object v = payload.get("bestPremium");
        if (v == null) v = payload.get("premium");
        if (v == null) v = payload.get("totalPremium");
        if (v == null) v = payload.get("grossPremium");
        return v != null ? String.valueOf(v) : null;
    }

    @GetMapping("/redirectionurl")
    public Object getRedirectionurl(@RequestParam String applicationId) {
        return digitService.getAgentRedirectionUrl(applicationId);
    }

    @GetMapping("/payment")
    public Object getPayment(@RequestParam String applicationId) {
        return digitService.getPaymentUrl(applicationId);
    }

    @PostMapping("/status")
    public Object getStatus(@RequestBody DigitPolicyStatusReqDto reqDto) {
        return digitService.getPolicyStatus(reqDto);
    }

    @PostMapping("/video")
    public Object getStatus(@RequestParam String applicationId) {
        return digitService.getVideoVerification(applicationId);
    }

    @PostMapping("/policy/pdf")
    public Object getPolicyPdf(@RequestParam String policyNumber) {
        return digitService.getPolicyPdf(policyNumber);
    }
    @PostMapping("/quote/pdf")
    public Object getQuotePdf(@RequestParam String applicationId) {
        return digitService.getQuotePdf(applicationId);
    }
    
    @PostMapping("/test/mapping")
    public Object testMapping(@RequestBody QuickQuoteRequestDto requestDto) {
        return vendorDigitRequestMapper.map(requestDto);
    }
}
