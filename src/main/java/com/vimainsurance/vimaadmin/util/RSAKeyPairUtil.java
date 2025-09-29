package com.vimainsurance.vimaadmin.util;

import org.springframework.stereotype.Component;
import java.util.Base64;

@Component
public class RSAKeyPairUtil {
    
    public String createTimestampChallenge() {
        long timestamp = System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(String.valueOf(timestamp).getBytes());
    }
    
    public boolean validateTimestampChallenge(String challenge, long maxAgeMs) {
        try {
            String timestampStr = new String(Base64.getDecoder().decode(challenge));
            long timestamp = Long.parseLong(timestampStr);
            long currentTime = System.currentTimeMillis();
            return (currentTime - timestamp) <= maxAgeMs;
        } catch (Exception e) {
            return false;
        }
    }
} 