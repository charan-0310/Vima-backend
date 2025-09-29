package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.vimainsurance.vimaadmin.dto.CommonResponseDto;
import com.vimainsurance.vimaadmin.dto.GenericRequestDto;
import com.vimainsurance.vimaadmin.dto.digit.DigitPolicyStatusReqDto;
import com.vimainsurance.vimaadmin.dto.digit.QQGlowWrapper;
import com.vimainsurance.vimaadmin.entity.Vendor;
import com.vimainsurance.vimaadmin.entity.VendorToken;
import com.vimainsurance.vimaadmin.mapper.VendorApiMapper;
import com.vimainsurance.vimaadmin.repository.VendorApiEndpointRepository;
import com.vimainsurance.vimaadmin.repository.VendorApiHeaderRepository;
import com.vimainsurance.vimaadmin.repository.VendorRepository;
import com.vimainsurance.vimaadmin.repository.VendorTokenRepository;
import com.vimainsurance.vimaadmin.service.IDigitService;
import com.vimainsurance.vimaadmin.service.VendorApiService;
import com.vimainsurance.vimaadmin.util.ConverterUtils;

import io.jsonwebtoken.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;


@Service
public class DigitServiceImpl implements IDigitService{

    @Autowired
    private  VendorRepository vendorRepository;
    
    @Autowired
    private  VendorApiEndpointRepository endpointRepository;
    
    @Autowired
    private  VendorApiHeaderRepository headerRepository;
    
    @Autowired
    private  VendorTokenRepository tokenRepository;
    
    @Autowired
    private  VendorApiMapper vendorApiMapper;
    
    @Autowired
    private  ObjectMapper objectMapper;
    
    @Autowired
    private  RestTemplate restTemplate;

    @Autowired
    private VendorApiService apiService;
    
    @Override
    public CommonResponseDto getAuth() {
        CommonResponseDto responseDto = new CommonResponseDto();
        try{
        GenericRequestDto requestDto = new GenericRequestDto();

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "Wcshe9z684HwbKGofOjQyw==");
        payload.put("password", "KHBQYOVYC7FdLmfjZuQBXO87MZEjj8DLogQh7CTj3zQ=");

        requestDto.setPayload(payload);
        responseDto =  apiService.callVendorApi("Digit", "digit_auth", true, requestDto).getBody();

        String accessToken = (String) responseDto.getPayload().get("access_token");
        String refreshToken = (String) responseDto.getPayload().get("refresh_token");
        Integer expiresIn = (Integer) responseDto.getPayload().get("expiresIn");
    
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
        Vendor vendor = vendorRepository.findByNameAndActiveTrue("Digit")
        .orElseThrow(() -> new RuntimeException("Vendor not found or inactive"));
        VendorToken token = new VendorToken();
        token.setVendor(vendor);
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());

        tokenRepository.save(token);

        return responseDto;
        }
        catch(Exception e){
            return new CommonResponseDto(null, "Error Generating Auth", 500);
        }
    }

    @Override
    public CommonResponseDto getQuickQuote(QQGlowWrapper glowWrapper) {
        // TODO Auto-generated method stub

        CommonResponseDto responseDto = new CommonResponseDto();
        try{
            Vendor vendor = vendorRepository.findByNameAndActiveTrue("Digit")
            .orElseThrow(() -> new RuntimeException("Vendor not found or inactive"));
            GenericRequestDto requestDto = new GenericRequestDto();
            Map<String, Object> payload = ConverterUtils.convertToMap(glowWrapper);
            System.out.println(glowWrapper.getQqGlow().getBasicDetails().isSamePolicyHolder());;
            System.out.println(payload);
        

            VendorToken token = tokenRepository.findTopByVendorIdAndExpiresAtAfterOrderByExpiresAtDesc(
                    vendor.getId(), LocalDateTime.now())
                    .orElse(null);

            if(token == null) {
                getAuth();
            }
            requestDto.setPayload(payload);
       
            responseDto  =  apiService.callVendorApi("Digit", "digit_quick_quote", false, requestDto).getBody();
         
           return responseDto;

        } catch(Exception e){
            return new CommonResponseDto(null, "Error" + e.getMessage(), 500);
        }
    }

    
    public Map<String, Object> convertToMap(QQGlowWrapper wrapper) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    
        // Custom null serializer: replaces null with ""
        DefaultSerializerProvider.Impl sp = new DefaultSerializerProvider.Impl();
        sp.setNullValueSerializer(new JsonSerializer<Object>() {
            @Override
            public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                    throws IOException, java.io.IOException {
                jsonGenerator.writeString("");
            }
        });
        mapper.setSerializerProvider(sp);
    
        // First serialize to JSON string with nulls replaced
        try {
            String json = mapper.writeValueAsString(wrapper);
            // Then deserialize into Map
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error while converting to Map", e);
        }
    }

    @Override
    public CommonResponseDto getPolicyStatus(DigitPolicyStatusReqDto reqDto) {
        // TODO Auto-generated method stub
        CommonResponseDto responseDto = new CommonResponseDto();
        try {
            Vendor vendor = vendorRepository.findByNameAndActiveTrue("Digit")
            .orElseThrow(() -> new RuntimeException("Vendor not found or inactive"));
            GenericRequestDto requestDto = new GenericRequestDto();
            Map<String, Object> payload = new HashMap<>();

            Map<String, Object> applicationStatus = new HashMap<>();
            applicationStatus.put("policyNumber", reqDto.getPolicyNumber());
            applicationStatus.put("applicationId", reqDto.getApplicationId());
            payload.put("applicationStatus", applicationStatus);
            requestDto.setPayload(payload);
            VendorToken token = tokenRepository.findTopByVendorIdAndExpiresAtAfterOrderByExpiresAtDesc(
                vendor.getId(), LocalDateTime.now())
                .orElse(null);

            if(token == null) {
                getAuth();
            }
           
            requestDto.setPayload(payload);
   
            responseDto  =  apiService.callVendorApi("Digit", "digit_status", false, requestDto).getBody();
     
            return responseDto;
        } catch (Exception e) {
            return new CommonResponseDto(null, "Error" + e.getMessage(), 500);
        }
    }

    @Override
    public CommonResponseDto getAgentRedirectionUrl(String applicationId) {
        CommonResponseDto responseDto = new CommonResponseDto();
        try {
            Vendor vendor = vendorRepository.findByNameAndActiveTrue("Digit")
            .orElseThrow(() -> new RuntimeException("Vendor not found or inactive"));
            GenericRequestDto requestDto = new GenericRequestDto();
            Map<String, Object> payload = new HashMap<>();

            Map<String, Object> applicationStatus = new HashMap<>();
            applicationStatus.put("applicationId", applicationId);
            payload.put("redirectionUrl", applicationStatus);
            requestDto.setPayload(payload);
            VendorToken token = tokenRepository.findTopByVendorIdAndExpiresAtAfterOrderByExpiresAtDesc(
                vendor.getId(), LocalDateTime.now())
                .orElse(null);

            if(token == null) {
                getAuth();
            }
           
            requestDto.setPayload(payload);
   
            responseDto  =  apiService.callVendorApi("Digit", "digit_agent_redirect", false, requestDto).getBody();
     
            return responseDto;
        } catch (Exception e) {
            return new CommonResponseDto(null, "Error" + e.getMessage(), 500);
        }
      }

    @Override
    public CommonResponseDto getPaymentUrl(String applicationId) {
        CommonResponseDto responseDto = new CommonResponseDto();
        try {
            Vendor vendor = vendorRepository.findByNameAndActiveTrue("Digit")
            .orElseThrow(() -> new RuntimeException("Vendor not found or inactive"));
            GenericRequestDto requestDto = new GenericRequestDto();
            Map<String, Object> payload = new HashMap<>();

            Map<String, Object> paymentUrlMap = new HashMap<>();
            paymentUrlMap.put("applicationId", applicationId);
            paymentUrlMap.put("successUrl", "https://preprodl-pluslife.godigit.com/DigitPlus/#/retail-life/success-page?Payment=Success");
            paymentUrlMap.put("failureUrl", "https://preprodl-pluslife.godigit.com/DigitPlus/#/retail-life/success-page?Payment=Failed");

            payload.put("paymentLinkGeneration", paymentUrlMap);
            requestDto.setPayload(payload);
            VendorToken token = tokenRepository.findTopByVendorIdAndExpiresAtAfterOrderByExpiresAtDesc(
                vendor.getId(), LocalDateTime.now())
                .orElse(null);

            if(token == null) {
                getAuth();
            }
           
            requestDto.setPayload(payload);
            System.out.println(payload);
            responseDto  =  apiService.callVendorApi("Digit", "digit_payment", false, requestDto).getBody();
     
            return responseDto;
        } catch (Exception e) {
            return new CommonResponseDto(null, "Error" + e.getMessage(), 500);
        }    
    }

    @Override
    public CommonResponseDto getVideoVerification(String applicationId) {
        CommonResponseDto responseDto = new CommonResponseDto();
        try {
            Vendor vendor = vendorRepository.findByNameAndActiveTrue("Digit")
            .orElseThrow(() -> new RuntimeException("Vendor not found or inactive"));
            GenericRequestDto requestDto = new GenericRequestDto();
            Map<String, Object> payload = new HashMap<>();

            Map<String, Object> headerParamMap = new HashMap<>();
            headerParamMap.put("applicationid", applicationId);
            headerParamMap.put("personRole", "");
            Map<String, Object> pIVCLinkGeneration = new HashMap<>();
            pIVCLinkGeneration.put("headerParam", headerParamMap);
            payload.put("PIVCLinkGeneration", pIVCLinkGeneration);
            requestDto.setPayload(payload);
            VendorToken token = tokenRepository.findTopByVendorIdAndExpiresAtAfterOrderByExpiresAtDesc(vendor.getId(), LocalDateTime.now())
                .orElse(null);

            if(token == null) {
                getAuth();
            }
           
            requestDto.setPayload(payload);
   
            responseDto  =  apiService.callVendorApi("Digit", "digit_video", false, requestDto).getBody();
     
            return responseDto;
        } catch (Exception e) {
            return new CommonResponseDto(null, "Error" + e.getMessage(), 500);
        }    
    }

    @Override
    public CommonResponseDto getPolicyPdf(String policyNumber) {
        CommonResponseDto responseDto = new CommonResponseDto();
        try {
            Vendor vendor = vendorRepository.findByNameAndActiveTrue("Digit")
            .orElseThrow(() -> new RuntimeException("Vendor not found or inactive"));
            GenericRequestDto requestDto = new GenericRequestDto();
            Map<String, Object> payload = new HashMap<>();
            payload.put("documentType", "03");
            payload.put("policyNumber", policyNumber);
            VendorToken token = tokenRepository.findTopByVendorIdAndExpiresAtAfterOrderByExpiresAtDesc(vendor.getId(), LocalDateTime.now())
                .orElse(null);

            if(token == null) {
                getAuth();
            }
           
            requestDto.setPayload(payload);
   
            responseDto  =  apiService.callVendorApi("Digit", "digit_policy_pdf", false, requestDto).getBody();
     
            return responseDto;
        } catch (Exception e) {
            return new CommonResponseDto(null, "Error" + e.getMessage(), 500);
        }  
    }

    @Override
    public CommonResponseDto getQuotePdf(String applicationId) {
        CommonResponseDto responseDto = new CommonResponseDto();
        try {
            Vendor vendor = vendorRepository.findByNameAndActiveTrue("Digit")
            .orElseThrow(() -> new RuntimeException("Vendor not found or inactive"));
            GenericRequestDto requestDto = new GenericRequestDto();
            Map<String, Object> payload = new HashMap<>();

            Map<String, Object> applicationStatus = new HashMap<>();
            applicationStatus.put("applicationid", applicationId);
            payload.put("quotePDF", applicationStatus);
            requestDto.setPayload(payload);
            VendorToken token = tokenRepository.findTopByVendorIdAndExpiresAtAfterOrderByExpiresAtDesc(vendor.getId(), LocalDateTime.now())
                .orElse(null);

            if(token == null) {
                getAuth();
            }
           
            requestDto.setPayload(payload);
   
            responseDto  =  apiService.callVendorApi("Digit", "digit_quote_pdf", false, requestDto).getBody();
     
            return responseDto;
        } catch (Exception e) {
            return new CommonResponseDto(null, "Error" + e.getMessage(), 500);
        }  
    }


    


      

   
    
    
}
