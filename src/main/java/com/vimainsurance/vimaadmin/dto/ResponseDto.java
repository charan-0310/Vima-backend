package com.vimainsurance.vimaadmin.dto;
public class ResponseDto<T> {

	private String message;

	private Integer errorCode = null;

	/**
	 * Optional machine-readable error key for clients (e.g. NO_RATE_TABLE_CONFIGURED).
	 * Null on success and for legacy numeric-only errors.
	 */
	private String errorKey;

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

	public ResponseDto(Integer errorCode, String message, T payload, String errorKey) {
		this.message = message;
		this.payload = payload;
		this.errorCode = errorCode;
		this.errorKey = errorKey;
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

	public String getErrorKey() {
		return errorKey;
	}

	public void setErrorKey(String errorKey) {
		this.errorKey = errorKey;
	}

	public void setPayload(T payload) {
		this.payload = payload;
	}

	public void setTotalRecords(long totalRecords) {
		this.totalRecords = totalRecords;
	}

}
