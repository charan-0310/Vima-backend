package com.vimainsurance.vimaadmin.dto;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BaseResponse<T> {

	public ResponseDto<T> formSuccessResponse(String message, T result) {
		return new ResponseDto<>(message, result);
	}

	public ResponseDto<T> formSuccessResponse(String message, T result, long totalRecords) {
		return new ResponseDto<>(message, result, totalRecords);
	}

	public ResponseDto<T> formSuccessResponse(String message) {
		return new ResponseDto<>(message, null);
	}

	public ResponseDto<T> formErrorResponse(Integer errorCode, String message) {
		return new ResponseDto<>(errorCode, message);
	}
	
	public ResponseDto<T> formErrorResponse(String message) {
		return new ResponseDto<>(0, message);
	}
	
	public ResponseDto<List<String>> formErrorResponse(String message, List<String> errors) {
		return new ResponseDto<>(400, message, (List<String>) errors);
	}

	public ResponseDto<T> formErrorResponse(String message, T payload) {
		return new ResponseDto<>(400,message, payload);
	}

	/** Error response with optional {@link ResponseDto#errorKey} for API clients. */
	public ResponseDto<T> formErrorResponseWithKey(Integer errorCode, String message, T payload, String errorKey) {
		return new ResponseDto<>(errorCode, message, payload, errorKey);
	}

	public ResponseEntity<ResponseDto<T>> render(ResponseDto<T> response) {
		if (response.getErrorCode() != null) {
			ResponseDto<T> errorResponse = new ResponseDto<>(response.getErrorCode(), response.getMessage(), response.getPayload());
			errorResponse.setErrorKey(response.getErrorKey());
			return renderError(errorResponse);
		}
		// Return the same instance so optional fields (e.g. ledgerSummary, allowedOrganizationIds)
		// are not dropped when serializing success responses.
		return renderSuccess(response);
	}

	private ResponseEntity<ResponseDto<T>> renderSuccess(ResponseDto<T> response) {
		return ResponseEntity.ok(response);
	}

	private ResponseEntity<ResponseDto<T>> renderError(ResponseDto<T> response) {
		Integer errorCode = response.getErrorCode();
		HttpStatus httpStatus;
		
		if (errorCode != null) {
			// Map error codes to HTTP status codes
			switch (errorCode) {
				case 401:
					httpStatus = HttpStatus.UNAUTHORIZED;
					break;
				case 403:
					httpStatus = HttpStatus.FORBIDDEN;
					break;
				case 404:
					httpStatus = HttpStatus.NOT_FOUND;
					break;
				case 409:
					httpStatus = HttpStatus.CONFLICT;
					break;
				case 422:
					httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
					break;
				case 429:
					httpStatus = HttpStatus.TOO_MANY_REQUESTS;
					break;
				case 500:
					httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
					break;
				case 502:
					httpStatus = HttpStatus.BAD_GATEWAY;
					break;
				case 503:
					httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
					break;
				default:
					// Default to 400 for other error codes
					httpStatus = HttpStatus.BAD_REQUEST;
					break;
			}
		} else {
			httpStatus = HttpStatus.BAD_REQUEST;
		}
		
		return ResponseEntity.status(httpStatus).body(response);
	}

}
