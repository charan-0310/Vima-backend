package com.vimainsurance.vimaadmin.configuration.oauth;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.util.JwtUtil;
import com.vimainsurance.vimaadmin.util.ZohoUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class VimaOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    
    @Value("${redirect.url}")
    private String uiRedirectUrl;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ZohoUtil zohoUtil;
    
    @Autowired
    private IAdminUserRepository userRepository;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");

        Optional<AdminUser> userOptional = userRepository.findByEmail(email);
        if(!userOptional.isPresent()){
          String redirectUrl = UriComponentsBuilder
          .fromUriString(uiRedirectUrl+"/*")
          .build()
          .toUriString();
      
         response.sendRedirect(redirectUrl);
         return;
        }
        AdminUser user = userOptional.get();
        System.out.println((String)oauthUser.getAttribute("sub"));
        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getRole());
       String redirectUrl = UriComponentsBuilder
        .fromUriString(uiRedirectUrl+"/oauth2")
        .fragment("accessToken=" + accessToken + "&refreshToken=" + refreshToken)
        .build()
        .toUriString();
       System.out.print(redirectUrl);
       response.sendRedirect(redirectUrl);
       
     }
}
