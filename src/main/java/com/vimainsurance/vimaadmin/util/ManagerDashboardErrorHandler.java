package com.vimainsurance.vimaadmin.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.BaseResponse;

/**
 * Utility class for handling manager dashboard specific errors
 */
public class ManagerDashboardErrorHandler {
    
    public enum ErrorCode {
        MANAGER_NOT_FOUND("MANAGER_NOT_FOUND"),
        INVALID_PERIOD("INVALID_PERIOD"),
        UNAUTHORIZED_ACCESS("UNAUTHORIZED_ACCESS"),
        DATA_ACCESS_ERROR("DATA_ACCESS_ERROR"),
        CALCULATION_ERROR("CALCULATION_ERROR"),
        INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR");
        
        private final String code;
        
        ErrorCode(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    public static ResponseEntity<ResponseDto<Object>> handleManagerNotFound(String username) {
        BaseResponse<Object> responseObj = new BaseResponse<>();
        return responseObj.render(responseObj.formErrorResponse(createErrorResponse(ErrorCode.MANAGER_NOT_FOUND, 
                "Manager with username '" + username + "' not found",
                "The requested manager does not exist or is not active")));
    }
    
    public static ResponseEntity<ResponseDto<Object>> handleInvalidPeriod(String period) {
        BaseResponse<Object> responseObj = new BaseResponse<>();
        return responseObj.render(responseObj.formErrorResponse(createErrorResponse(ErrorCode.INVALID_PERIOD,
                "Invalid period: " + period,
                "Supported periods are: this_month, last_month, last_3_months, last_6_months, this_year")));
    }
    
    public static ResponseEntity<ResponseDto<Object>> handleUnauthorizedAccess(String username) {
        BaseResponse<Object> responseObj = new BaseResponse<>();
        return responseObj.render(responseObj.formErrorResponse(createErrorResponse(ErrorCode.UNAUTHORIZED_ACCESS,
                "Unauthorized access to manager dashboard",
                "User '" + username + "' does not have permission to access this manager's data")));
    }
    
    public static ResponseEntity<ResponseDto<Object>> handleDataAccessError(String operation, Exception e) {
        BaseResponse<Object> responseObj = new BaseResponse<>();
        return responseObj.render(responseObj.formErrorResponse(createErrorResponse(ErrorCode.DATA_ACCESS_ERROR,
                "Error accessing data for " + operation,
                e.getMessage())));
    }
    
    public static ResponseEntity<ResponseDto<Object>> handleCalculationError(String calculation, Exception e) {
        BaseResponse<Object> responseObj = new BaseResponse<>();
        return responseObj.render(responseObj.formErrorResponse(createErrorResponse(ErrorCode.CALCULATION_ERROR,
                "Error calculating " + calculation,
                e.getMessage())));
    }
    
    public static ResponseEntity<ResponseDto<Object>> handleInternalServerError(String operation, Exception e) {
        BaseResponse<Object> responseObj = new BaseResponse<>();
        return responseObj.render(responseObj.formErrorResponse(createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR,
                "Internal server error during " + operation,
                e.getMessage())));
    }
    
    private static String createErrorResponse(ErrorCode errorCode, String message, String details) {
        return String.format("{\"error\":{\"code\":\"%s\",\"message\":\"%s\",\"details\":\"%s\"}}", 
            errorCode.getCode(), message, details);
    }
}
