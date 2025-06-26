package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.configuration.ZohoConfig;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.ZohoSyncResponseDto;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.vimainsurance.vimaadmin.repository.ITokenRepository;
import com.vimainsurance.vimaadmin.service.IZohoCRMService;
import com.vimainsurance.vimaadmin.util.BidirectionalZohoSync;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.ZohoCRMUtil;
import com.vimainsurance.vimaadmin.util.ZohoSyncUtil;
import com.vimainsurance.vimaadmin.util.ZohoUtil;
import com.zoho.crm.api.HeaderMap;
import com.zoho.crm.api.Initializer;
import com.zoho.crm.api.ParameterMap;
import com.zoho.crm.api.record.APIException;
import com.zoho.crm.api.record.Record;
import com.zoho.crm.api.record.RecordOperations;
import com.zoho.crm.api.record.ResponseHandler;
import com.zoho.crm.api.record.ResponseWrapper;
import com.zoho.crm.api.util.Choice;

@Service
public class ZohoCRMServiceImpl implements IZohoCRMService {

    private static final String REQUIRED_FIELDS = 
        "Full_Name,Email,Phone,City,State,Occupation,Annual_Income," +
        "Created_Time,Modified_Time,Lead_Status,Owner.email";

    @Autowired
    private ZohoUtil zohoUtil;
    @Autowired
    private ZohoConfig zohoConfig;

    @Autowired
    private ITokenRepository tokenRepository;

    @Autowired
    private ICustomerRepository customerRepository;

    @Autowired
    private ZohoCRMUtil zohoCRMUtil;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private ZohoSyncUtil zohoSyncUtil;

    @Autowired
    private BidirectionalZohoSync bidirectionalSync;

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<ZohoSyncResponseDto>> syncZohoData() {
        BaseResponse<ZohoSyncResponseDto> responseObj = new BaseResponse<>();
        
        try {
            // Create a result object to track sync statistics
            class SyncResult {
                int successCount = 0;
                int failureCount = 0;
                List<String> errors = new ArrayList<>();
            }
            
            SyncResult result = new SyncResult();
            
            try {
                // Use the bidirectional sync
                BidirectionalZohoSync.SyncResult syncRes = bidirectionalSync.synchronize();
                
                // Aggregate results
                if (syncRes.zohoToDbResult != null) {
                    result.successCount += syncRes.zohoToDbResult.successCount;
                    result.failureCount += syncRes.zohoToDbResult.failureCount;
                    result.errors.addAll(syncRes.zohoToDbResult.errors);
                }
                
                if (syncRes.dbToZohoResult != null) {
                    result.successCount += syncRes.dbToZohoResult.successCount;
                    result.failureCount += syncRes.dbToZohoResult.failureCount;
                    result.errors.addAll(syncRes.dbToZohoResult.errors);
                }
                
                // Check if there were any failures
                if (result.failureCount > 0) {
                    StringBuilder errorMsg = new StringBuilder("Sync completed with errors:\n");
                    result.errors.forEach(error -> errorMsg.append("- ").append(error).append("\n"));
                    
                    return responseObj.render(responseObj.formSuccessResponse(
                        Constants.SUCCESS,
                        new ZohoSyncResponseDto(result.successCount, result.failureCount)                    ));
                }
                
                // Return success response with total count
                return responseObj.render(responseObj.formSuccessResponse(
                    Constants.SUCCESS,
                    new ZohoSyncResponseDto(result.successCount, result.failureCount)
                ));
                
            } catch (Exception e) {
                return responseObj.render(responseObj.formErrorResponse(500, "Error syncing data: " + e.getMessage()));
            }
        } catch (Exception e) {
            return responseObj.render(responseObj.formErrorResponse(500, "Error syncing data: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Object>> getLeadById(String leadId) {
        BaseResponse<Object> responseObj = new BaseResponse<>();
        try {
            RecordOperations recordOperations = new RecordOperations();
            if(Initializer.getInitializer() == null){
                zohoConfig.initializeZohoManually(null);
            }
            com.zoho.crm.api.util.APIResponse<ResponseHandler> apiResponse = recordOperations.getRecord(Long.parseLong(leadId), "Leads", new ParameterMap(), new HeaderMap());
            ResponseHandler response = apiResponse.getObject();
            
            if (response instanceof ResponseWrapper) {
                ResponseWrapper responseWrapper = (ResponseWrapper) response;
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseWrapper.getData()));
            } else if (response instanceof APIException) {
                APIException exception = (APIException) response;
                Choice<String> errorMessage = exception.getMessage();
                return responseObj.render(responseObj.formErrorResponse(errorMessage.getValue()));
            }
            
            return responseObj.render(responseObj.formErrorResponse("No data found"));
        } catch (Exception e) {
            return responseObj.render(responseObj.formErrorResponse("Error fetching lead: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Object>> getAllLeads(int page, int size) {
        BaseResponse<Object> responseObj = new BaseResponse<>();
        try {
        URL url = new URL("https://www.zohoapis.in/crm/v2/Leads");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Zoho-oauthtoken " + tokenRepository.findById(1L).get().getAccessToken());
        if(conn.getResponseCode() == 401){
           zohoUtil.refreshZohoAccessToken();
           getAllLeads(page, size);
        }

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed to fetch leads: HTTP error code : " + conn.getResponseCode());
        }
        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line);
            }
            System.out.println(output.toString());
        }
        if(!output.toString().isEmpty()){
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, output.toString()));
        }
            return responseObj.render(responseObj.formErrorResponse("No data found"));
        } catch (Exception e) {
            return responseObj.render(responseObj.formErrorResponse("Error fetching leads: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Object>> getAllLeadsByAgents(int page, int size, String email) {
        BaseResponse<Object> responseObj = new BaseResponse<>();
        try {
            zohoSyncUtil.initializeZohoCRM();
            
            // Use the search criteria format from ZohoSyncUtil
            String criteria = email != null ? "(Owner.email:equals:" + email + ")" : null;
            List<Record> records = zohoSyncUtil.fetchZohoRecords((page - 1) * size, size);
            
            if (records.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "No data found"));
            }
            
            List<CustomerResponseDto> customers = zohoSyncUtil.processZohoRecords(records);
            return responseObj.render(responseObj.formSuccessResponse(
                Constants.SUCCESS, 
                customers, 
                customers.size()
            ));
            
        } catch (Exception e) {
            return responseObj.render(responseObj.formErrorResponse(500, "Error fetching leads: " + e.getMessage()));
        }
    }
} 