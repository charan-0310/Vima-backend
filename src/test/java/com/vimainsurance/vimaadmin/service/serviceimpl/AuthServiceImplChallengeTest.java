// package com.vimainsurance.vimaadmin.service.serviceimpl;

// import static org.junit.jupiter.api.Assertions.*;

// import java.util.concurrent.ConcurrentHashMap;
// import javax.crypto.Mac;
// import javax.crypto.spec.SecretKeySpec;
// import java.nio.charset.StandardCharsets;
// import java.security.InvalidKeyException;
// import java.security.NoSuchAlgorithmException;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;
// import org.springframework.http.ResponseEntity;

// import com.vimainsurance.vimaadmin.dto.ChallengeLoginRequestDto;
// import com.vimainsurance.vimaadmin.dto.ChallengeRequestDto;
// import com.vimainsurance.vimaadmin.dto.ChallengeResponseDto;
// import com.vimainsurance.vimaadmin.dto.ResponseDto;

// public class AuthServiceImplChallengeTest {

//     @InjectMocks
//     private AuthServiceImpl authService;

//     @Mock
//     private org.springframework.security.authentication.AuthenticationManager authenticationManager;

//     @BeforeEach
//     void setUp() {
//         MockitoAnnotations.openMocks(this);
//     }

//     @Test
//     void testGetChallenge() {
//         // Given
//         ChallengeRequestDto requestDto = new ChallengeRequestDto("testuser");

//         // When
//         ResponseEntity<ResponseDto<ChallengeResponseDto>> response = authService.getChallenge(requestDto);

//         // Then
//         assertNotNull(response);
//         assertNotNull(response.getBody());
//         assertNotNull(response.getBody().getPayload());
//         assertNotNull(response.getBody().getPayload().getChallenge());
//         assertFalse(response.getBody().getPayload().getChallenge().isEmpty());
//     }

//     @Test
//     void testChallengeLogin_Success() {
//         // Given
//         String username = "someuser";
//         String password = "userPassword";
        
//         // First get a challenge
//         ChallengeRequestDto challengeRequest = new ChallengeRequestDto(username);
//         ResponseEntity<ResponseDto<ChallengeResponseDto>> challengeResponse = authService.getChallenge(challengeRequest);
//         String challenge = challengeResponse.getBody().getPayload().getChallenge();
        
//         // Compute HMAC
//         String hashedPassword = computeHmacSha256(password, challenge);
        
//         ChallengeLoginRequestDto loginRequest = new ChallengeLoginRequestDto(username, hashedPassword, "valid-recaptcha-token");

//         // When
//         ResponseEntity<ResponseDto<String>> response = authService.challengeLogin(loginRequest);

//         // Then
//         assertNotNull(response);
//         assertNotNull(response.getBody());
//         // Note: This test will fail in real environment due to reCAPTCHA verification
//         // In a real test, you would mock the reCAPTCHA verification
//     }

//     @Test
//     void testChallengeLogin_InvalidCredentials() {
//         // Given
//         String username = "someuser";
        
//         // First get a challenge
//         ChallengeRequestDto challengeRequest = new ChallengeRequestDto(username);
//         ResponseEntity<ResponseDto<ChallengeResponseDto>> challengeResponse = authService.getChallenge(challengeRequest);
//         String challenge = challengeResponse.getBody().getPayload().getChallenge();
        
//         // Use wrong password
//         String wrongPassword = "wrongPassword";
//         String hashedPassword = computeHmacSha256(wrongPassword, challenge);
        
//         ChallengeLoginRequestDto loginRequest = new ChallengeLoginRequestDto(username, hashedPassword, "valid-recaptcha-token");

//         // When
//         ResponseEntity<ResponseDto<String>> response = authService.challengeLogin(loginRequest);

//         // Then
//         assertNotNull(response);
//         assertNotNull(response.getBody());
//         assertNotNull(response.getBody().getErrorCode());
//     }

//     @Test
//     void testChallengeLogin_NoChallenge() {
//         // Given
//         ChallengeLoginRequestDto loginRequest = new ChallengeLoginRequestDto("someuser", "somehash", "valid-recaptcha-token");

//         // When
//         ResponseEntity<ResponseDto<String>> response = authService.challengeLogin(loginRequest);

//         // Then
//         assertNotNull(response);
//         assertNotNull(response.getBody());
//         assertNotNull(response.getBody().getErrorCode());
//     }

//     @Test
//     void testChallengeLogin_InvalidUser() {
//         // Given
//         String username = "nonexistentuser";
        
//         // First get a challenge
//         ChallengeRequestDto challengeRequest = new ChallengeRequestDto(username);
//         ResponseEntity<ResponseDto<ChallengeResponseDto>> challengeResponse = authService.getChallenge(challengeRequest);
//         String challenge = challengeResponse.getBody().getPayload().getChallenge();
        
//         String hashedPassword = computeHmacSha256("somepassword", challenge);
        
//         ChallengeLoginRequestDto loginRequest = new ChallengeLoginRequestDto(username, hashedPassword, "valid-recaptcha-token");

//         // When
//         ResponseEntity<ResponseDto<String>> response = authService.challengeLogin(loginRequest);

//         // Then
//         assertNotNull(response);
//         assertNotNull(response.getBody());
//         assertNotNull(response.getBody().getErrorCode());
//     }

//     /**
//      * Helper method to compute HMAC-SHA256 (same as in AuthServiceImpl)
//      */
//     private String computeHmacSha256(String data, String salt) {
//         try {
//             Mac mac = Mac.getInstance("HmacSHA256");
//             SecretKeySpec secretKeySpec = new SecretKeySpec(data.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
//             mac.init(secretKeySpec);
            
//             byte[] hash = mac.doFinal(salt.getBytes(StandardCharsets.UTF_8));
//             return bytesToHex(hash);
            
//         } catch (NoSuchAlgorithmException | InvalidKeyException e) {
//             throw new RuntimeException("Failed to compute HMAC", e);
//         }
//     }
    
//     /**
//      * Helper method to convert byte array to hexadecimal string
//      */
//     private String bytesToHex(byte[] bytes) {
//         StringBuilder result = new StringBuilder();
//         for (byte b : bytes) {
//             result.append(String.format("%02x", b));
//         }
//         return result.toString();
//     }
// } 