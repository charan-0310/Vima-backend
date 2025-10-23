package com.vimainsurance.vimaadmin.dto;
import java.time.LocalDateTime;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DealsRequestDto {
private String firstName;
private String lastName;
private String email;
private String phone;
private LocalDate dateOfBirth;
private String gender;
private String panNumber;
private String aadhaarNumber;
private String address;
private String city;
private String state;
private String pincode;
private String accountType;
private String accountStatus;
private String employeeNumber;
private String relationship;
private Boolean isPrimaryMember;
private String username;
private String passwordHash;
private String preferredLanguage;
private UUID leadId;
private String custId;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
}
