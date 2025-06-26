package com.vimainsurance.vimaadmin.service;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.LoginRequestDto;
import com.vimainsurance.vimaadmin.dto.LoginResponseDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenRequestDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IAuthService {
    ResponseEntity<ResponseDto<LoginResponseDto>> login(LoginRequestDto requestDto);
    public ResponseEntity<ResponseDto<RefreshTokenResponseDto>> getRefreshToken(RefreshTokenRequestDto requestDto);
}
