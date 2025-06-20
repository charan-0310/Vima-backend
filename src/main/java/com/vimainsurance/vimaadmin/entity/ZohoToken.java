package com.vimainsurance.vimaadmin.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "zoho_token", schema = "admin")
public class ZohoToken {

        @Id
        private Long id = 1L;  // singleton row
    
        private String accessToken;
    
        private String refreshToken;
    
        private LocalDateTime expiryTime;
}
