package com.vimainsurance.vimaadmin.dto;
public class ResponseDto<T> {

	private String message;

	private Integer errorCode = null;

	private T payload;

	private long totalRecords = 0;

	public ResponseDto() {
		// Default constructor for serialization
	}

	public ResponseDto(String message, T payload) {
		this.message = message;
		this.payload = payload;
	}

	public ResponseDto(String message, T payload, long totalRecords) {
		this.message = message;
		this.totalRecords = totalRecords;
		this.payload = payload;
	}

	public ResponseDto(Integer errorCode, String message) {
		this.errorCode = errorCode;
		this.message = message;
	}

	public ResponseDto(Integer errorCode, String message, T payload) {
		this.message = message;
		this.payload = payload;
		this.errorCode = errorCode;
	}

	public ResponseDto(String message, T payload, long totalRecords, Integer errorCode) {
		this.message = message;
		this.payload = payload;
		this.totalRecords = totalRecords;
		this.errorCode = errorCode;
	}

	// Getter methods
	public String getMessage() {
		return message;
	}

	public Integer getErrorCode() {
		return errorCode;
	}

	public T getPayload() {
		return payload;
	}

	public long getTotalRecords() {
		return totalRecords;
	}

	// Setter methods
	public void setMessage(String message) {
		this.message = message;
	}

	public void setErrorCode(Integer errorCode) {
		this.errorCode = errorCode;
	}

	public void setPayload(T payload) {
		this.payload = payload;
	}

	public void setTotalRecords(long totalRecords) {
		this.totalRecords = totalRecords;
	}

}
