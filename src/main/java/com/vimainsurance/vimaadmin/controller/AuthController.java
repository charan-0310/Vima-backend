package com.vimainsurance.vimaadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.LoginRequestDto;
import com.vimainsurance.vimaadmin.dto.LoginResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IAuthService;



@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private IAuthService authService;

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String test(){
        return "Term Veera";
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDto<LoginResponseDto>>  login(@RequestBody LoginRequestDto reqDto){
    return authService.login(reqDto);
    }
}
