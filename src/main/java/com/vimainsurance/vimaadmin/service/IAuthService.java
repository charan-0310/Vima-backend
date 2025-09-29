package com.vimainsurance.vimaadmin.service;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ChallengeLoginRequestDto;
import com.vimainsurance.vimaadmin.dto.ChallengeRequestDto;
import com.vimainsurance.vimaadmin.dto.ChallengeResponseDto;
import com.vimainsurance.vimaadmin.dto.LoginRequestDto;
import com.vimainsurance.vimaadmin.dto.LoginResponseDto;
import com.vimainsurance.vimaadmin.dto.PublicKeyResponseDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenRequestDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IAuthService {
    ResponseEntity<ResponseDto<LoginResponseDto>> login(LoginRequestDto requestDto);
    public ResponseEntity<ResponseDto<RefreshTokenResponseDto>> getRefreshToken(RefreshTokenRequestDto requestDto);
    
    // Challenge-response authentication methods
    ResponseEntity<ResponseDto<ChallengeResponseDto>> getChallenge(ChallengeRequestDto requestDto);
    ResponseEntity<ResponseDto<LoginResponseDto>> challengeLogin(ChallengeLoginRequestDto requestDto);

}
