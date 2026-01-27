package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthentikUserCreationDto {
    private String name;
    private String username;
    private String email;
    
    @JsonProperty("is_active")
    private Boolean isActive = true;
    
    private String type = "internal"; // Default user type
    
    private List<String> groups; // Array of group UUIDs
    
    private Map<String, Object> attributes; // Custom attributes for Authentik user
}

