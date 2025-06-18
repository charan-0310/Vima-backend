package com.vimainsurance.vimaadmin.service.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.LoginRequestDto;
import com.vimainsurance.vimaadmin.dto.LoginResponseDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenRequestDto;
import com.vimainsurance.vimaadmin.dto.RefreshTokenResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.service.IAuthService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUtil;
import com.vimainsurance.vimaadmin.util.ZohoUtil;

@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ZohoUtil zohoUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    // @Autowired
    // private IZohoAuthService zohoAuthService;

    @Override
    public ResponseEntity<ResponseDto<LoginResponseDto>> login(LoginRequestDto requestDto) {
        BaseResponse<LoginResponseDto> responseObj = new BaseResponse<>();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        requestDto.getUsername(),
                        requestDto.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtUtil.generateToken(requestDto.getUsername(), authentication.getAuthorities().iterator().next().getAuthority());

            // Initialize Zoho CRM after successful authentication
            try {
                // zohoAuthService.initializeZohoCRM();
                // zohoUtil.refreshZohoAccessToken();
            } catch (Exception e) {
                // Log the error but don't fail the login
                System.err.println("Failed to initialize Zoho CRM: " + e.getMessage());
            }

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new LoginResponseDto(token, "Bearer")));
        } catch (Exception ex) {
            return responseObj.render(responseObj.formErrorResponse(ex.getMessage()));
        }    
    }

    @Override
    public ResponseEntity<ResponseDto<RefreshTokenResponseDto>> getRefreshToken(RefreshTokenRequestDto requestDto) {
        BaseResponse<RefreshTokenResponseDto> responseObj = new BaseResponse<>();
        try {
            String refreshToken = requestDto.getRefreshtoken();

            if (!jwtUtil.validateToken(refreshToken)) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            String username = jwtUtil.extractUsername(refreshToken);
            AdminUser adminUser = adminUserRepository.findByUsername(username).orElseThrow();    

            // Refresh Zoho token along with JWT token
            try {
                // zohoAuthService.refreshZohoToken();
                // zohoUtil.refreshZohoAccessToken();
            } catch (Exception e) {
                // Log the error but don't fail the token refresh
                System.err.println("Failed to refresh Zoho token: " + e.getMessage());
            }

            String newAccessToken = jwtUtil.generateToken(username, adminUser.getRole());

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new RefreshTokenResponseDto(newAccessToken)));

        } catch (Exception ex) {
            return responseObj.render(responseObj.formErrorResponse(ex.getMessage()));
        }
    }

}
