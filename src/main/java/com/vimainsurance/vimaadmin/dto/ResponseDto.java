package com.vimainsurance.vimaadmin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseDto<T> {

	private String message;

	private Integer errorCode = null;

	private T payload;

	private Integer totalRecords;

	public ResponseDto(String message, T payload) {
		this.message = message;
		this.payload = payload;
	}

	public ResponseDto(String message, T payload, Integer totalRecords) {
		this.message = message;
		this.payload = payload;
		this.totalRecords = totalRecords;
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

}
