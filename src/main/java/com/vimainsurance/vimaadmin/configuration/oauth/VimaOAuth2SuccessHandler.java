package com.vimainsurance.vimaadmin.configuration.oauth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.vimainsurance.vimaadmin.dto.LoginResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class VimaOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private IAdminUserRepository userRepository;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        AdminUser user = userRepository.findByEmail(email).orElseThrow();
        System.out.println((String)oauthUser.getAttribute("sub"));
        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getRole());

       String redirectUrl = UriComponentsBuilder
        .fromUriString("http://localhost:8080/oauth2")
        .fragment("accessToken=" + accessToken + "&refreshToken=" + refreshToken)
        .build()
        .toUriString();

       response.sendRedirect(redirectUrl);
       
     }
}
