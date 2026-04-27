package com.vimainsurance.vimaadmin.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DealsRequestDto {
private String firstName;
private String lastName;
private String fullName;
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
private String designation;
private LocalDate dateOfJoining;
private Boolean isPrimaryMember;
private UUID primaryIndividualId;
private String username;
private String passwordHash;
private String preferredLanguage;
private UUID leadId;
private String custId;
private String maritalStatus;
private String sumInsured;
private BigDecimal ctc;
private String healthId;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
}
