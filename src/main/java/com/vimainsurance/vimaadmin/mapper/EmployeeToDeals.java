package com.vimainsurance.vimaadmin.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.dto.EmployeeUploadDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.entity.Organization;

public class EmployeeToDeals {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Deals mapToDeals(EmployeeUploadDto employeeUploadDto, Organization organization) {
        Deals deals = new Deals();
        
        // Employee Number (from employeeId)
        deals.setEmployeeNumber(employeeUploadDto.getEmployeeId());
        
        // Parse name - split by whitespace (first part is firstName, rest is lastName)
        String name = employeeUploadDto.getName() != null ? employeeUploadDto.getName().trim() : "";
        String[] nameParts = name.split("\\s+", 2);
        deals.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            deals.setLastName(nameParts[1]);
        }
        deals.setFullName(employeeUploadDto.getName() != null ? employeeUploadDto.getName().trim() : "");
        // Date of Birth (parse from String to LocalDate)
        deals.setDateOfBirth(parseDate(employeeUploadDto.getDateOfBirth()));
        
        // Gender
        deals.setGender(employeeUploadDto.getGender() != null ? employeeUploadDto.getGender().trim() : null);
        
        // Email
        deals.setEmail(employeeUploadDto.getEmail() != null ? employeeUploadDto.getEmail().trim() : null);
        
        // Phone (from mobile)
        deals.setPhone(employeeUploadDto.getMobile() != null ? employeeUploadDto.getMobile().trim() : null);
        
        // Date of Joining (parse from String to LocalDate)
        deals.setDateOfJoining(parseDate(employeeUploadDto.getDateOfJoining()));
        
        // Designation
        deals.setDesignation(employeeUploadDto.getDesignation() != null ? employeeUploadDto.getDesignation().trim() : null);

        deals.setDepartment(employeeUploadDto.getDepartment() != null ? employeeUploadDto.getDepartment(): null);

        
        // Relationship - will be mapped to NomineeRelationship enum in EmployeeService
        // For now, keep the original relationship string (mapping happens in uploadEmployees)
        deals.setRelationship(employeeUploadDto.getRelationship() != null ? employeeUploadDto.getRelationship().trim() : null);
        // Actual relationship (e.g. Son, Daughter) when we normalize to CHILD1–CHILD4
        if (employeeUploadDto.getActualRelationship() != null && !employeeUploadDto.getActualRelationship().isBlank()) {
            deals.setActualRelationship(employeeUploadDto.getActualRelationship().trim());
        }

        // Account Type - CORPORATE_EMPLOYEE for Self, CORPORATE_DEPENDENT for others
        String relationship = employeeUploadDto.getRelationship() != null ? 
            employeeUploadDto.getRelationship().trim() : "";
        if ("Self".equalsIgnoreCase(relationship)) {
            deals.setAccountType(AccountType.CORPORATE_EMPLOYEE);
            deals.setIsPrimaryMember(true);
            // Employee Number only for Self
            deals.setEmployeeNumber(employeeUploadDto.getEmployeeId());
        } else {
            deals.setAccountType(AccountType.CORPORATE_DEPENDENT);
            deals.setIsPrimaryMember(false);
            // Dependents should NOT have employeeNumber
            deals.setEmployeeNumber(null);
        }

        // Marital Status
        deals.setMaritalStatus(employeeUploadDto.getMaritalStatus() != null ? 
            employeeUploadDto.getMaritalStatus().trim() : null);
        
        // Sum Insured
        deals.setSumInsured(employeeUploadDto.getSumInsured() != null ? 
            employeeUploadDto.getSumInsured().trim() : null);
        
        // Department - Note: Deals entity doesn't have department field, 
        // but we can store it in remarks or another field if needed
        // For now, we'll skip it as it's not in the entity
        
        deals.setOrganization(organization);
        
        // Default values
        deals.setStatus(AccountStatus.PENDING_APPROVAL);
        deals.setPreferredLanguage("en");
        if(employeeUploadDto.getHealthId() != null && !employeeUploadDto.getHealthId().isEmpty()) {
            deals.setHealthId(employeeUploadDto.getHealthId().trim());
        }
        return deals;
    }

    /**
     * Helper method to parse date string to LocalDate
     * Matches the parseDate method in CsvDealsReaderUtil
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Maps a list of EmployeeUploadDto to a list of Deals
     * Note: This method signature seems incorrect - it should return List<Deals>
     */
    public static List<Deals> mapToDealsList(List<EmployeeUploadDto> employeeUploadDtoList, Organization organization) {
        return employeeUploadDtoList.stream()
            .map(employeeUploadDto -> EmployeeToDeals.mapToDeals(employeeUploadDto, organization))
            .collect(Collectors.toList());
    }

    /**
     * Update an existing Deals entity from EmployeeUploadDto
     * Maps all fields from EmployeeUploadDto to Deals entity
     * 
     * @param existingDeal The existing Deals entity to update
     * @param dto EmployeeUploadDto containing the new values
     * @param organization Organization entity
     * @param relationshipMap Optional map of employeeId+relationship to NomineeRelationship enum value (can be null)
     */
    public static void updateDealFromDto(Deals existingDeal, EmployeeUploadDto dto, Organization organization,
            Map<String, String> relationshipMap) {
        // Parse name - split by whitespace (first part is firstName, rest is lastName)
        String name = dto.getName() != null ? dto.getName().trim() : "";
        String[] nameParts = name.split("\\s+", 2);
        existingDeal.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            existingDeal.setLastName(nameParts[1]);
        } else {
            existingDeal.setLastName(null);
        }
        existingDeal.setFullName(name);

        // Update fields from DTO
        existingDeal.setDateOfBirth(parseDate(dto.getDateOfBirth()));
        existingDeal.setGender(dto.getGender() != null ? dto.getGender().trim() : null);
        existingDeal.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        existingDeal.setPhone(dto.getMobile() != null ? dto.getMobile().trim() : null);
        existingDeal.setDateOfJoining(parseDate(dto.getDateOfJoining()));
        existingDeal.setDesignation(dto.getDesignation() != null ? dto.getDesignation().trim() : null);
        existingDeal.setDepartment(dto.getDepartment() != null ? dto.getDepartment().trim() : null);
        // Marital Status
        existingDeal.setMaritalStatus(dto.getMaritalStatus() != null ? 
            dto.getMaritalStatus().trim() : null);
        
        // Sum Insured
        existingDeal.setSumInsured(dto.getSumInsured() != null ? 
            dto.getSumInsured().trim() : null);
        // Actual relationship (e.g. Son, Daughter)
        if (dto.getActualRelationship() != null && !dto.getActualRelationship().isBlank()) {
            existingDeal.setActualRelationship(dto.getActualRelationship().trim());
        }

        // Update relationship from map if available
        if (relationshipMap != null) {
            String key = dto.getEmployeeId() + "_" + dto.getRelationship();
            if (relationshipMap.containsKey(key)) {
                existingDeal.setRelationship(relationshipMap.get(key));
            }
        }

        existingDeal.setOrganization(organization);
        existingDeal.setUpdatedAt(LocalDateTime.now());

        // Update account type and primary member status based on relationship
        String relationship = dto.getRelationship() != null ? dto.getRelationship().trim() : "";
        if ("Self".equalsIgnoreCase(relationship)) {
            existingDeal.setAccountType(AccountType.CORPORATE_EMPLOYEE);
            existingDeal.setIsPrimaryMember(true);
            // Only update employeeNumber for Self relationship
            existingDeal.setEmployeeNumber(dto.getEmployeeId());
        } else {
            existingDeal.setAccountType(AccountType.CORPORATE_DEPENDENT);
            existingDeal.setIsPrimaryMember(false);
            // Dependents should NOT have employeeNumber
            existingDeal.setEmployeeNumber(null);
        }
    }

    /**
     * Update an existing Deals entity from EmployeeUploadDto (without relationship map)
     * Convenience method for simple updates
     * 
     * @param existingDeal The existing Deals entity to update
     * @param dto EmployeeUploadDto containing the new values
     * @param organization Organization entity
     */
    public static void updateDealFromDto(Deals existingDeal, EmployeeUploadDto dto, Organization organization) {
        updateDealFromDto(existingDeal, dto, organization, null);
    }
}