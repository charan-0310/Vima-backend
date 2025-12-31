package com.vimainsurance.vimaadmin.dto;


import org.springframework.http.ResponseEntity;
import java.util.List;

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

	public ResponseDto<T> formErrorResponse(String message, T payload) {
		return new ResponseDto<>(400,message, payload);
	}

	public ResponseEntity<ResponseDto<T>> render(ResponseDto<T> response) {
		if (response.getErrorCode() != null) {
			ResponseDto<T> errorResponse = new ResponseDto<>(response.getErrorCode(), response.getMessage());
			return renderError(errorResponse);
		}
		ResponseDto<T> successResponse = new ResponseDto<>(response.getMessage(), response.getPayload(), response.getTotalRecords());
		return renderSuccess(successResponse);
	}

	private ResponseEntity<ResponseDto<T>> renderSuccess(ResponseDto<T> response) {
		return ResponseEntity.ok(response);
	}

	private ResponseEntity<ResponseDto<T>> renderError(ResponseDto<T> response) {
		return ResponseEntity.badRequest().body(response);
	}

}
