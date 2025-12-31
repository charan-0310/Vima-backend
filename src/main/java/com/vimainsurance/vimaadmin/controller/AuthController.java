package com.vimainsurance.vimaadmin.controller;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ChallengeLoginRequestDto;
import com.vimainsurance.vimaadmin.dto.ChallengeRequestDto;
import com.vimainsurance.vimaadmin.dto.ChallengeResponseDto;
import com.vimainsurance.vimaadmin.dto.LoginRequestDto;
import com.vimainsurance.vimaadmin.dto.LoginResponseDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenRequestDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IAuthService;

import java.util.List;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private IAuthService authService;

    @Autowired
    private FeatureFlagService featureFlagService;

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String test(){
        logger.info("[correlationId:{}] /test endpoint called", MDC.get("correlationId"));
        return "Term Veera";
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDto<LoginResponseDto>>  login(@RequestBody LoginRequestDto reqDto){
        logger.info("[correlationId:{}] /login endpoint called", MDC.get("correlationId"));
        return authService.login(reqDto);
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<ResponseDto<RefreshTokenResponseDto>>  refreshToken(@RequestBody RefreshTokenRequestDto reqDto){
        logger.info("[correlationId:{}] /refresh-token endpoint called", MDC.get("correlationId"));
        return authService.getRefreshToken(reqDto);
    }

    /**
     * Challenge-response authentication endpoints
     */
    @GetMapping("/auth/challenge")
    public ResponseEntity<ResponseDto<ChallengeResponseDto>> getChallenge(@RequestParam String username) {
        logger.info("[correlationId:{}] /auth/challenge endpoint called for username", MDC.get("correlationId"));
        ChallengeRequestDto requestDto = new ChallengeRequestDto(username);
        return authService.getChallenge(requestDto);
    }
    
    @PostMapping("/auth/login")
    public ResponseEntity<ResponseDto<LoginResponseDto>> challengeLogin(@RequestBody ChallengeLoginRequestDto requestDto) {
        logger.info("[correlationId:{}] /auth/login endpoint called", MDC.get("correlationId"));
        return authService.challengeLogin(requestDto);
    }
    
    /**
     * Simple endpoint to get CSP headers with nonce
     * Returns no content - headers are added automatically by CorrelationIdFilter
     */
    @GetMapping("/nonce")
    public ResponseEntity<Void> getNonce() {
        logger.debug("[correlationId:{}] /nonce endpoint called", MDC.get("correlationId"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/me")
    public ResponseEntity<ResponseDto<List<FeatureFlagResponseDto>>> getFeatureFalgs() {
    //    logger.info("[correlationId:{}] /auth/challenge endpoint called for username", MDC.get("correlationId"));
        ChallengeRequestDto requestDto = new ChallengeRequestDto(username);
        return authService.getChallenge(requestDto);
    }
}
