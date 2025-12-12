package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.dto.BulkEmployeeDeletionRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeeUploadDto;
import com.vimainsurance.vimaadmin.dto.EmployeeUploadResponse;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.mapper.EmployeeToDeals;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.enums.NomineeRelationship;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmployeeService {

    @Autowired
    private  Validator validator;

    @Autowired
    private IDealsRepository dealsRepository;


    @Autowired
    private EmployeeBatchService employeeBatchService;

       
    @Autowired
    private Javers javers;


    public EmployeeUploadResponse validateEmployee(List<EmployeeUploadDto> employeeUploadDtoList, Organization organization) {
        List<String> errors = new ArrayList<>();
        Map<String, List<EmployeeUploadDto>> groupedEmployeeByEmployeeId = groupByEmployeeId(employeeUploadDtoList);
        for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedEmployeeByEmployeeId.entrySet()) {
            String employeeId = entry.getKey();
            List<EmployeeUploadDto> employeeUploadDtoListByEmployeeId = entry.getValue();
            

            //validate counts: 1 Self, 1 Spouse, 1 Father, 1 Mother, 1 Father in law, 1 Mother in law, max 4 Children
            long selfCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> "Self".equalsIgnoreCase(e.getRelationship()))
                .count();
            long spouseCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> "Spouse".equalsIgnoreCase(e.getRelationship()))
                .count();
            long fatherCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> "Father".equalsIgnoreCase(e.getRelationship()))
                .count();
            long motherCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> "Mother".equalsIgnoreCase(e.getRelationship()))
                .count();
            long fatherInLawCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> {
                    String rel = e.getRelationship();
                    return rel != null && (rel.equalsIgnoreCase("Father in law") || 
                           rel.equalsIgnoreCase("FatherInLaw") || 
                           rel.equalsIgnoreCase("FATHER_IN_LAW"));
                })
                .count();
            long motherInLawCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> {
                    String rel = e.getRelationship();
                    return rel != null && (rel.equalsIgnoreCase("Mother in law") || 
                           rel.equalsIgnoreCase("MotherInLaw") || 
                           rel.equalsIgnoreCase("MOTHER_IN_LAW"));
                })
                .count();
            // Count children (only Child1, Child2, Child3, Child4 with explicit indices)
            long childCount = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> {
                    String rel = e.getRelationship();
                    if (rel == null) return false;
                    int idx = extractChildIndexFromString(rel);
                    // Only count if it has a valid index (1-4)
                    return idx > 0 && idx <= 4;
                })
                .count();
            
            // Note: Sequential validation for children is done during upload
            // to check against existing children in database, not in the upload itself.
            // This allows updating individual children (e.g., Child3 alone) if they exist.
            
            if (selfCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Self is allowed");
            }
            if (spouseCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Spouse is allowed");
            }
            if (fatherCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Father is allowed");
            }
            if (motherCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Mother is allowed");
            }
            if (fatherInLawCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Father in law is allowed");
            }
            if (motherInLawCount > 1) {
                errors.add("employeeId: " + employeeId + " - Only one Mother in law is allowed");
            }
            if (childCount > 4) {
                errors.add("employeeId: " + employeeId + " - Maximum 4 Children are allowed");
            }
            
            // Validate no duplicate child indices (e.g., multiple Child1 for same employee)
            Map<Integer, Long> childIndexCounts = employeeUploadDtoListByEmployeeId.stream()
                .filter(e -> {
                    String rel = e.getRelationship();
                    if (rel == null) return false;
                    int idx = extractChildIndexFromString(rel);
                    return idx > 0 && idx <= 4;
                })
                .collect(Collectors.groupingBy(
                    e -> extractChildIndexFromString(e.getRelationship()),
                    Collectors.counting()
                ));
            
            for (Map.Entry<Integer, Long> childIndexEntry : childIndexCounts.entrySet()) {
                if (childIndexEntry.getValue() > 1) {
                    errors.add("employeeId: " + employeeId + " - Duplicate Child" + childIndexEntry.getKey() + 
                        " found. Only one Child" + childIndexEntry.getKey() + " is allowed per employee");
                }
            }
            
            // Validate each employee in the group
            for (EmployeeUploadDto employeeUploadDto : employeeUploadDtoListByEmployeeId) {
                String relationship = employeeUploadDto.getRelationship();
                if(relationship == null || relationship.isBlank()) {
                    errors.add("employeeId: " + employeeId + " - Relationship is required");
                    continue;
                }
                
                // Validate that relationship is one of the allowed values
                if (!isValidRelationship(relationship)) {
                    errors.add("employeeId: " + employeeId + " - Invalid relationship: '" + relationship + 
                        "'. Allowed relationships: Self, Spouse, Father, Mother, Father in law, Mother in law, Child1, Child2, Child3, Child4");
                    continue;
                }
                
                // Validate Self relationship with bean validation
                if ("Self".equalsIgnoreCase(relationship)) {
                    Set<ConstraintViolation<EmployeeUploadDto>> violations = validator.validate(employeeUploadDto);
                    if (!violations.isEmpty()) {
                        errors.add("employeeId: " + employeeId + " - " + violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", ")));
                    }
                } 
                // Validate Child relationship - must have explicit index (Child1, Child2, Child3, or Child4)
                else if (relationship != null && relationship.toUpperCase().startsWith("CHILD")) {
                    // Extract child index
                    int childIndex = extractChildIndexFromString(relationship);
                    
                    // Validate that explicit index is provided (1-4)
                    if (childIndex == 0) {
                        errors.add("employeeId: " + employeeId + " - Child relationship must have explicit index. Use Child1, Child2, Child3, or Child4");
                    } else if (childIndex > 4) {
                        errors.add("employeeId: " + employeeId + " - Child index cannot be greater than 4. Maximum allowed: Child4");
                    } else {
                        // Validate age only if index is valid
                    try {
                        LocalDate dateOfBirth = LocalDate.parse(employeeUploadDto.getDateOfBirth());
                        int age = LocalDate.now().getYear() - dateOfBirth.getYear();
                        
                        // Adjust age if birthday hasn't occurred this year
                        LocalDate now = LocalDate.now();
                        if (dateOfBirth.plusYears(age).isAfter(now)) {
                            age--;
                        }
                        
                        // Child age should not be greater than or equal to 25 (i.e., must be < 25)
                        if (age >= 25) {
                            errors.add("employeeId: " + employeeId + " - Child age must be less than 25 years old");
                        }
                    } catch (Exception e) {
                        errors.add("employeeId: " + employeeId + " - Invalid date of birth format: " + employeeUploadDto.getDateOfBirth());
                    }
                }
                }
                else if ("Spouse".equalsIgnoreCase(relationship) || 
                        (relationship != null && relationship.toUpperCase().startsWith("SPOUSE"))) {
                            LocalDate dateOfBirth = LocalDate.parse(employeeUploadDto.getDateOfBirth());
                            int age = LocalDate.now().getYear() - dateOfBirth.getYear();
                            
                            // Adjust age if birthday hasn't occurred this year
                            LocalDate now = LocalDate.now();
                            if (dateOfBirth.plusYears(age).isAfter(now)) {
                                age--;
                            }
                            
                            // Spouse age should be greater than 18 (i.e., must be greater than 18)
                            if (age < 18) {
                                errors.add("employeeId: " + employeeId + " - Spouse age must be greater than 18 years old");
                            }
                }
              
            }
        }
        // List<Deals> existingDeals = dealsRepository.findByEmployeeNumberInAndOrganizationId(getEmployeeIds(employeeUploadDtoList), organization.getOrganizationId());
        //     Set<String> existingEmployeeIds = existingDeals.stream()
        //     .map(Deals::getEmployeeNumber)
        //     .filter(Objects::nonNull)
        //     .collect(Collectors.toSet());
        //     if(!existingDeals.isEmpty()) {
        //         errors.addAll(existingDeals.stream().distinct().map(d -> "employeeId: " + d.getEmployeeNumber() + " - Employee already exists").collect(Collectors.toList()));
        //     }
        //     if (existingEmployeeIds.size() < getEmployeeIds(employeeUploadDtoList).size()) {
                List<String> phonesToCheck = employeeUploadDtoList.stream()
                    .filter(e -> "Self".equalsIgnoreCase(e.getRelationship()))
                    .map(EmployeeUploadDto::getMobile)
                    .filter(Objects::nonNull)
                    .filter(phone -> !phone.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
                
                List<String> emailsToCheck = employeeUploadDtoList.stream()
                    .filter(e -> "Self".equalsIgnoreCase(e.getRelationship()))
                    .map(EmployeeUploadDto::getEmail)
                    .filter(Objects::nonNull)
                    .filter(email -> !email.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
                
                // if (!phonesToCheck.isEmpty() || !emailsToCheck.isEmpty()) {
                //     List<Deals> existingDealsByPhoneAndEmail = dealsRepository.findByEmployeePhoneAndEmployeeEmail(
                //         phonesToCheck, emailsToCheck, organization.getOrganizationId());
                    
                //     // Add errors for duplicates found by phone/email
                //     existingDealsByPhoneAndEmail.stream()
                //         .map(Deals::getEmployeeNumber)
                //         .filter(Objects::nonNull)
                //         .distinct()
                //         .forEach(empId -> 
                //             errors.add("employeeId: " + empId + " - Employee already exists by phone or email"));
                // }
            // }
        EmployeeUploadResponse response = new EmployeeUploadResponse();
        int totalEmployees = selfCount(employeeUploadDtoList).intValue();
        response.setTotalRows(employeeUploadDtoList.size());
        response.setTotalEmployees(totalEmployees);
        response.setTotalDependents(dependentCount(employeeUploadDtoList).intValue());
        response.setSuccessCount(totalEmployees - errors.size());
        response.setErrorCount(errors.size());
        response.setErrors(errors);
        response.setMessage(errors.isEmpty() ? "No Validation errors!" : "Validation errors");

        return response;
    }

    /**
     * Groups employee upload DTOs by employee ID
     * 
     * @param employeeUploadDtoList List of employee upload DTOs
     * @return Map where key is employeeId and value is list of DTOs with that employeeId
     */
    public Map<String, List<EmployeeUploadDto>> groupByEmployeeId(List<EmployeeUploadDto> employeeUploadDtoList) {
        if (employeeUploadDtoList == null || employeeUploadDtoList.isEmpty()) {
            return new LinkedHashMap<>();
        }
        
        return employeeUploadDtoList.stream()
            .collect(Collectors.groupingBy(
                dto -> dto.getEmployeeId() != null ? dto.getEmployeeId() : "",
                LinkedHashMap::new,  // Preserves insertion order
                Collectors.toList()
        ));
    }
    
    /**
     * Groups bulk employee deletion request DTOs by employee ID
     * 
     * @param bulkEmployeeDeletionRequestDtoList List of bulk employee deletion request DTOs
     * @return Map where key is employeeId and value is list of DTOs with that employeeId
     */
    public Map<String, List<BulkEmployeeDeletionRequestDto>> groupByEmployeeIdForBulkEmployeeDeletion(List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoList) {
        if (bulkEmployeeDeletionRequestDtoList == null || bulkEmployeeDeletionRequestDtoList.isEmpty()) {
            return new LinkedHashMap<>();
        }
        
        return bulkEmployeeDeletionRequestDtoList.stream()
            .collect(Collectors.groupingBy(
                dto -> dto.getEmployeeId() != null ? dto.getEmployeeId() : "",
                LinkedHashMap::new,  // Preserves insertion order   
                Collectors.toList()
        ));
    }


    /*
    Employee Save Logic
    - Validate the employees using validateEmployee method
    - If there are errors, return the errors in the EmployeeUploadResponse
    - If there are no errors, save the employees using the dealsRepository.saveAll method
    - Return the response in the EmployeeUploadResponse
    */
    // public EmployeeUploadResponse uploadEmployees(List<EmployeeUploadDto> employeeUploadDtoList, Organization organization) {
    //     try {
    //         EmployeeUploadResponse response = new EmployeeUploadResponse();
    //         EmployeeUploadResponse validateResponse = validateEmployee(employeeUploadDtoList, organization);
    //         response.setTotalRows(validateResponse.getTotalRows());
    //         response.setTotalEmployees(validateResponse.getTotalEmployees());
    //         response.setTotalDependents(validateResponse.getTotalDependents());
    //         response.setSuccessCount(validateResponse.getSuccessCount());
    //         response.setErrorCount(validateResponse.getErrorCount());
    //         response.setErrors(validateResponse.getErrors());
    //         response.setMessage(validateResponse.getMessage());
           
    //         if(validateResponse.getSuccessCount() > 0) {
    //             List<Deals> dealsList = EmployeeToDeals.mapToDealsList(employeeUploadDtoList, organization);
    //             // for (Deals deal : dealsList) {
    //                 // Optional<Deals> existingDealOptional = dealsRepository.findByEmployeeNumberAndOrganizationId(deal.getEmployeeNumber(), organization.getOrganizationId());
    //                 // if(existingDealOptional.isPresent()) {
    //                 //     Deals existingDeal = existingDealOptional.get();
    //                 //     existingDeal.setFirstName(deal.getFirstName());
    //                 //     existingDeal.setLastName(deal.getLastName());
    //                 //     existingDeal.setEmail(deal.getEmail());
    //                 //     existingDeal.setPhone(deal.getPhone());
    //                 //     existingDeal.setDateOfBirth(deal.getDateOfBirth());
    //                 //     existingDeal.setGender(deal.getGender());
    //                 //     existingDeal.setAddress(deal.getAddress());
    //                 //     existingDeal.setCity(deal.getCity());
    //                 //     existingDeal.setState(deal.getState());
    //                 //     existingDeal.setPincode(deal.getPincode());
    //                 //     existingDeal.setAccountType(deal.getAccountType());
    //                 //     existingDeal.setStatus(deal.getStatus());
    //                 //     existingDeal.setEmployeeNumber(deal.getEmployeeNumber());
    //                 //     existingDeal.setRelationship(deal.getRelationship());
    //                 //     existingDeal.setDesignation(deal.getDesignation());
    //                 //     existingDeal.setDateOfJoining(deal.getDateOfJoining());
    //                 //     existingDeal.setPrimaryIndividual(deal.getPrimaryIndividual());
    //                 //     existingDeal.setUpdatedAt(LocalDateTime.now());
    //                 //     dealsRepository.save(existingDeal);
    //                 // }
    //                 // else {
    //                     dealsRepository.saveAll(dealsList);
    //                 // }
    //             }
            

    //         return response;
    //     }
    //     catch (Exception e) {
    //         return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), e.getMessage(), 0, 0);
    //     }
    // }

    // @Transactional(rollbackFor = Exception.class)
    // public EmployeeUploadResponse uploadEmployees(List<EmployeeUploadDto> employeeUploadDtoList, Organization organization) {
    //     try {
    //         EmployeeUploadResponse response = new EmployeeUploadResponse();
    //         EmployeeUploadResponse validateResponse = validateEmployee(employeeUploadDtoList, organization);
            
    //         // If validation failed, return validation response
    //         if (validateResponse.getErrorCount() > 0) {
    //             return validateResponse;
    //         }
            
    //         // Group by employeeId to handle primary + dependents together
    //         Map<String, List<EmployeeUploadDto>> groupedByEmployeeId = groupByEmployeeId(employeeUploadDtoList);
    //         List<Deals> dealsToCreate = new ArrayList<>();
    //         List<Deals> dealsToUpdate = new ArrayList<>();
    //         List<Deals> dealsToSave = new ArrayList<>();
    //         int updatedCount = 0;
    //         int createdCount = 0;
            
    //         List<String> allEmployeeIds = new ArrayList<>(groupedByEmployeeId.keySet());
    //         List<Deals> existingPrimaries = dealsRepository.findByEmployeeNumberInAndOrganizationIdAndRelationship(
    //             allEmployeeIds, organization.getOrganizationId(), NomineeRelationship.SELF.getValue());
    //         Map<String, Deals> primaryEmployeeMap = existingPrimaries.stream()
    //             .collect(Collectors.toMap(Deals::getEmployeeNumber, d -> d, (d1, d2) -> d1));
            
    //         List<String> phonesToCheckBulk = new ArrayList<>();
    //         List<String> emailsToCheckBulk = new ArrayList<>();
    //         Map<String, EmployeeUploadDto> phoneToEmployeeMap = new HashMap<>();
    //         Map<String, EmployeeUploadDto> emailToEmployeeMap = new HashMap<>();
            
    //         for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedByEmployeeId.entrySet()) {
    //             String employeeId = entry.getKey();
    //             List<EmployeeUploadDto> employeeGroup = entry.getValue();
    //             EmployeeUploadDto selfDto = employeeGroup.stream()
    //                 .filter(e -> "Self".equalsIgnoreCase(e.getRelationship()))
    //                 .findFirst()
    //                 .orElse(null);
                
    //             if (selfDto != null && !primaryEmployeeMap.containsKey(employeeId)) {
    //                 String phone = selfDto.getMobile() != null && !selfDto.getMobile().trim().isEmpty() 
    //                     ? selfDto.getMobile().trim() : null;
    //                 String email = selfDto.getEmail() != null && !selfDto.getEmail().trim().isEmpty() 
    //                     ? selfDto.getEmail().trim() : null;
                    
    //                 if (phone != null) {
    //                     phonesToCheckBulk.add(phone);
    //                     phoneToEmployeeMap.put(phone, selfDto);
    //                 }
    //                 if (email != null) {
    //                     emailsToCheckBulk.add(email);
    //                     emailToEmployeeMap.put(email, selfDto);
    //                 }
    //             }
    //         }
            
    //         Set<String> existingPhones = new HashSet<>();
    //         Set<String> existingEmails = new HashSet<>();
    //         if (!phonesToCheckBulk.isEmpty() || !emailsToCheckBulk.isEmpty()) {
    //             List<Deals> existingDuplicates = dealsRepository.findByEmployeePhoneAndEmployeeEmail(
    //                 phonesToCheckBulk, emailsToCheckBulk, organization.getOrganizationId());
                
    //             existingPhones = existingDuplicates.stream()
    //                 .map(Deals::getPhone)
    //                 .filter(Objects::nonNull)
    //                 .map(String::trim)
    //                 .collect(Collectors.toSet());
                
    //             existingEmails = existingDuplicates.stream()
    //                 .map(Deals::getEmail)
    //                 .filter(Objects::nonNull)
    //                 .map(String::trim)
    //                 .collect(Collectors.toSet());
    //         }
            
    //         List<Deals> primaryEmployeesForDependents = new ArrayList<>();
            
    //         // Process each employee group
    //         for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedByEmployeeId.entrySet()) {
    //             String employeeId = entry.getKey();
    //             List<EmployeeUploadDto> employeeGroup = entry.getValue();
                
    //             // Find or create primary employee (Self)
    //             Deals primaryEmployee = null;
    //             EmployeeUploadDto selfDto = employeeGroup.stream()
    //                 .filter(e -> "Self".equalsIgnoreCase(e.getRelationship()))
    //                 .findFirst()
    //                 .orElse(null);
                
    //             if (selfDto != null) {
    //                 // Check if primary employee exists (from bulk fetch)
    //                 Deals existingPrimary = primaryEmployeeMap.get(employeeId);
                    
    //                 if (existingPrimary != null) {
    //                     primaryEmployee = existingPrimary;
    //                     // Ensure individualId is preserved (should already be set from database)
    //                     if (primaryEmployee.getIndividualId() == null) {
    //                         log.error("Existing primary employee {} has no individualId! This should not happen.", employeeId);
    //                     }
    //                     updateDealFromDto(primaryEmployee, selfDto, organization);
    //                     primaryEmployee.setRelationship(mapRelationshipToNomineeRelationship("Self", 0));
    //                     primaryEmployee.setUpdatedAt(LocalDateTime.now());
    //                     dealsToSave.add(primaryEmployee);

    //                     updatedCount++;
    //                     log.debug("Updating existing primary employee: {} with individualId: {}", 
    //                         employeeId, primaryEmployee.getIndividualId());
    //                 } else {
    //                     // Check for duplicates (from bulk check)
    //                     List<String> duplicateErrors = new ArrayList<>();
    //                     String phone = selfDto.getMobile() != null && !selfDto.getMobile().trim().isEmpty() 
    //                         ? selfDto.getMobile().trim() : null;
    //                     String email = selfDto.getEmail() != null && !selfDto.getEmail().trim().isEmpty() 
    //                         ? selfDto.getEmail().trim() : null;
                        
    //                     if (phone != null && existingPhones.contains(phone)) {
    //                         duplicateErrors.add("employeeId: " + employeeId + " - Phone number already exists: " + phone);
    //                     }
                        
    //                     if (email != null && existingEmails.contains(email)) {
    //                         duplicateErrors.add("employeeId: " + employeeId + " - Email already exists: " + email);
    //                     }
                        
    //                     if (!duplicateErrors.isEmpty()) {
    //                         validateResponse.getErrors().addAll(duplicateErrors);
    //                         return validateResponse;
    //                     }
                        
    //                     // Create new primary employee
    //                     primaryEmployee = EmployeeToDeals.mapToDeals(selfDto, organization);
    //                     primaryEmployee.setRelationship(mapRelationshipToNomineeRelationship("Self", 0));
    //                     primaryEmployee.setCreatedAt(LocalDateTime.now());
    //                     primaryEmployee.setUpdatedAt(LocalDateTime.now());
    //                     // Ensure individualId is null for new records
    //                     primaryEmployee.setIndividualId(null);
    //                     createdCount++;
    //                     dealsToSave.add(primaryEmployee);
    //                     log.debug("Creating new primary employee: {} (individualId: null)", employeeId);
    //                 }
    //             } else {
    //                 // No Self in upload - check if employee already exists (from bulk fetch)
    //                 Deals existingEmployee = primaryEmployeeMap.get(employeeId);
                    
    //                 if (existingEmployee != null) {
    //                     if (existingEmployee.getIsPrimaryMember() != null && existingEmployee.getIsPrimaryMember()) {
    //                         // It's a primary employee - use it as primaryEmployee
    //                         primaryEmployee = existingEmployee;
    //                         log.debug("Found existing primary employee for dependents: {}", employeeId);
    //                     } else {
    //                         // It's a dependent - find its primary individual
    //                         if (existingEmployee.getPrimaryIndividual() != null) {
    //                             primaryEmployee = existingEmployee.getPrimaryIndividual();
    //                             log.debug("Found existing primary employee through dependent: {}", employeeId);
    //                         } else {
    //                             validateResponse.getErrors().add("employeeId: " + employeeId + " - Employee exists but is a dependent without primary individual");
    //                             return validateResponse;
    //                         }
    //                     }
    //                     // Add to list for bulk dependent fetch (even though we're not updating it)
    //                     primaryEmployeesForDependents.add(primaryEmployee);
    //                 } else {
    //                     // No Self in upload and employee doesn't exist - error
    //                     validateResponse.getErrors().add("employeeId: " + employeeId + " - Employee not found. Cannot add dependents without primary employee (Self). Please include Self in the upload or ensure the employee exists in the database.");
    //                     return validateResponse;
    //                 }
    //             }
    //         }
            
    //         // Build a combined map of employeeId to primary employee (including both existing and newly created)
    //         Map<String, Deals> allPrimaryEmployeesMap = new HashMap<>(primaryEmployeeMap);
    //         // Add newly created primary employees from dealsToCreate
    //         for (Deals deal : dealsToCreate) {
    //             if (deal.getRelationship() != null && NomineeRelationship.SELF.getValue().equals(deal.getRelationship()) 
    //                 && deal.getEmployeeNumber() != null) {
    //                 allPrimaryEmployeesMap.put(deal.getEmployeeNumber(), deal);
    //             }
    //         }
    //         // Add existing primary employees that were found when processing dependents-only uploads
    //         // (they're in primaryEmployeesForDependents but might not be in allPrimaryEmployeesMap yet)
    //         for (Deals primary : primaryEmployeesForDependents) {
    //             if (primary != null && primary.getEmployeeNumber() != null 
    //                 && !allPrimaryEmployeesMap.containsKey(primary.getEmployeeNumber())) {
    //                 allPrimaryEmployeesMap.put(primary.getEmployeeNumber(), primary);
    //             }
    //         }
            
    //         // Fetch existing dependents only for primary employees that already exist in DB (have individualId)
    //         List<UUID> primaryIndividualIds = primaryEmployeesForDependents.stream()
    //             .filter(p -> p.getIndividualId() != null)
    //             .map(Deals::getIndividualId)
    //             .collect(Collectors.toList());
            
    //         Map<UUID, List<Deals>> dependentsByPrimaryId = new HashMap<>();
    //         if (!primaryIndividualIds.isEmpty()) {
    //             List<Deals> allExistingDependents = dealsRepository.findByPrimaryIndividualIdIn(primaryIndividualIds);
    //             dependentsByPrimaryId = allExistingDependents.stream()
    //                 .filter(d -> d.getPrimaryIndividual() != null && d.getPrimaryIndividual().getIndividualId() != null)
    //                 .collect(Collectors.groupingBy(d -> d.getPrimaryIndividual().getIndividualId()));
    //         }
            
    //         // Process dependents for each employee group
    //         for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedByEmployeeId.entrySet()) {
    //             String employeeId = entry.getKey();
    //             List<EmployeeUploadDto> employeeGroup = entry.getValue();
                
    //             // Get primary employee from the combined map (includes both existing and newly created)
    //             Deals primaryEmployee = allPrimaryEmployeesMap.get(employeeId);
                
    //             if (primaryEmployee != null) {
    //                 // Get existing dependents for this primary employee (from bulk fetch)
    //                 // For newly created employees (individualId is null), there are no existing dependents
    //                 List<Deals> existingDependents = new ArrayList<>();
    //                 if (primaryEmployee.getIndividualId() != null) {
    //                     existingDependents = dependentsByPrimaryId.getOrDefault(
    //                         primaryEmployee.getIndividualId(), new ArrayList<>());
    //                 }
                    
    //                 // Group existing dependents by relationship type
    //                 Map<String, List<Deals>> existingDependentsByRelationship = existingDependents.stream()
    //                     .filter(d -> d.getRelationship() != null)
    //                     .collect(Collectors.groupingBy(
    //                         d -> d.getRelationship().toUpperCase(),
    //                         Collectors.toList()
    //                     ));
                    
    //                 // Process dependents from DTO
    //                 for (EmployeeUploadDto dependentDto : employeeGroup) {
    //                     if (!"Self".equalsIgnoreCase(dependentDto.getRelationship())) {
    //                         String inputRelationship = dependentDto.getRelationship();
                            
    //                         // Map relationship to NomineeRelationship enum value
    //                         String mappedRelationship;
    //                         Deals existingDependent = null;
                            
    //                         if (inputRelationship != null && inputRelationship.toUpperCase().startsWith("CHILD")) {
    //                             // Extract child index from relationship (Child1 -> 1, Child2 -> 2, etc.)
    //                             final int childIndexFromJson = extractChildIndexFromString(inputRelationship);
                                
    //                             // Validate explicit index is provided
    //                             if (childIndexFromJson == 0) {
    //                                 validateResponse.getErrors().add("employeeId: " + employeeId + 
    //                                     " - Child relationship must have explicit index. Use Child1, Child2, Child3, or Child4");
    //                                 return validateResponse;
    //                             }
                                
    //                             if (childIndexFromJson > 4) {
    //                                 validateResponse.getErrors().add("employeeId: " + employeeId + 
    //                                     " - Child index cannot be greater than 4. Maximum allowed: Child4");
    //                                 return validateResponse;
    //                             }
                                
    //                             // Explicit index provided (Child1, Child2, etc.)
    //                             mappedRelationship = mapRelationshipToNomineeRelationship(inputRelationship, childIndexFromJson);
                                
    //                             // Check if this specific child index already exists
    //                             List<Deals> existingWithSameRelationship = existingDependentsByRelationship.getOrDefault(
    //                                 mappedRelationship.toUpperCase(), new ArrayList<>());
                                
    //                             if (!existingWithSameRelationship.isEmpty()) {
    //                                 // Child with this index already exists in database - will update it
    //                                 existingDependent = existingWithSameRelationship.get(0);
    //                                 log.debug("Found existing child with index {} for employee {}, will update", 
    //                                     childIndexFromJson, employeeId);
    //                             } else {
    //                                 // New child insert - validate that previous children exist
    //                                 // Check both database AND current upload batch
    //                                 for (int i = 1; i < childIndexFromJson; i++) {
    //                                     final int prevChildIndex = i; // Make final for lambda
    //                                     String prevChildRel;
    //                                     try {
    //                                         prevChildRel = NomineeRelationship.valueOf("CHILD" + prevChildIndex).getValue();
    //                                     } catch (IllegalArgumentException e) {
    //                                         prevChildRel = "CHILD" + prevChildIndex;
    //                                     }
                                        
    //                                     // Check if previous child exists in database
    //                                     boolean existsInDatabase = existingDependentsByRelationship.containsKey(prevChildRel.toUpperCase());
                                        
    //                                     // Check if previous child exists in current upload batch
    //                                     boolean existsInUpload = employeeGroup.stream()
    //                                         .anyMatch(dto -> {
    //                                             if (dto.getRelationship() == null) return false;
    //                                             int idx = extractChildIndexFromString(dto.getRelationship());
    //                                             return idx == prevChildIndex;
    //                                         });
                                        
    //                                     if (!existsInDatabase && !existsInUpload) {
    //                                         validateResponse.getErrors().add("employeeId: " + employeeId + 
    //                                             " - Cannot insert Child" + childIndexFromJson + " without Child" + prevChildIndex);
    //                                         return validateResponse;
    //                                     }
    //                                 }
    //                                 log.debug("Validated previous children exist (in database or upload), will create new Child{} for employee {}", 
    //                                     childIndexFromJson, employeeId);
    //                             }
    //                         } else {
    //                             // For non-child relationships (Spouse, Father, Mother), map directly
    //                             mappedRelationship = mapRelationshipToNomineeRelationship(inputRelationship, 0);
                                
    //                             // Check if dependent with same relationship exists
    //                             List<Deals> existingWithSameRelationship = existingDependentsByRelationship.getOrDefault(
    //                                 mappedRelationship.toUpperCase(), new ArrayList<>());
                                
    //                             if (!existingWithSameRelationship.isEmpty()) {
    //                                 // For relationships that should be unique (Spouse, Father, Mother), take the first one
    //                                 existingDependent = existingWithSameRelationship.get(0);
    //                             }
    //                         }
                            
    //                         if (existingDependent != null) {
    //                             // Update existing dependent with same relationship
    //                             // Ensure individualId is preserved (should already be set from database)
    //                             if (existingDependent.getIndividualId() == null) {
    //                                 log.error("Existing dependent {} for employee {} has no individualId! This should not happen.", 
    //                                     inputRelationship, employeeId);
    //                             }
    //                             updateDealFromDto(existingDependent, dependentDto, organization);
    //                             existingDependent.setRelationship(mappedRelationship);
    //                             existingDependent.setPrimaryIndividual(primaryEmployee);
    //                             existingDependent.setUpdatedAt(LocalDateTime.now());
    //                             dealsToSave.add(existingDependent);
    //                             updatedCount++;
    //                             log.debug("Updating existing dependent: {} - {} (mapped to {}) with individualId: {}", 
    //                                 employeeId, inputRelationship, mappedRelationship, existingDependent.getIndividualId());
    //                         } else {
    //                             // Create new dependent
    //                             Deals newDependent = EmployeeToDeals.mapToDeals(dependentDto, organization);
    //                             newDependent.setEmployeeNumber(employeeId);
    //                             newDependent.setRelationship(mappedRelationship);
    //                             newDependent.setPrimaryIndividual(primaryEmployee);
    //                             newDependent.setCreatedAt(LocalDateTime.now());
    //                             newDependent.setUpdatedAt(LocalDateTime.now());
    //                             // Ensure individualId is null for new records
    //                             dealsToSave.add(newDependent);
    //                             createdCount++;
    //                             log.debug("Creating new dependent: {} - {} (mapped to {}) (individualId: null)", 
    //                                 employeeId, inputRelationship, mappedRelationship);
    //                         }
    //                     }
    //                 }
    //             }
    //         }
            
    //         int batchSize = 1000; 
    //         int totalSaved = 0;

    //         // Batch insert new deals and batch update existing deals
    //         // int batchSize = 500; // Match JPA batch_size configuration
    //         // int totalInserted = 0;
    //         // int totalUpdated = 0;
            
    //         // // Safety check: Filter out any deals with individualId from dealsToCreate (should only be in dealsToUpdate)
    //         // List<Deals> validDealsToCreate = dealsToSave.stream()
    //         //     .filter(d -> d.getIndividualId() == null)
    //         //     .collect(Collectors.toList());
            
    //         // // Safety check: Filter out any deals without individualId from dealsToUpdate (should only be in dealsToCreate)
    //         // List<Deals> validDealsToUpdate = dealsToSave.stream()
    //         //     .filter(d -> d.getIndividualId() != null)
    //         //     .collect(Collectors.toList());
            
    //         // if (validdealsToSave.size() != dealsToSave.size()) {
    //         //     log.warn("Found {} deals with individualId in dealsToCreate list, filtering them out", 
    //         //         dealsToSave.size() - validdealsToSave.size());
    //         // }
            
    //         // if (validdealsToSave.size() != dealsToSave.size()) {
    //         //     log.warn("Found {} deals without individualId in dealsToUpdate list, filtering them out", 
    //         //         dealsToSave.size() - validdealsToSave.size());
    //         // }
            
    //         // log.info("Saving {} deals ({} new, {} updated) in batches of {}", 
    //         //     validdealsToSave.size() + validdealsToSave.size(), validdealsToSave.size(), validdealsToSave.size(), batchSize);
            
    //         // Batch insert new deals (only those without individualId)
    //         // if (!validdealsToSave.isEmpty()) {
    //         //     totalInserted = employeeBatchService.batchInsertDeals(validDealsToCreate, organization, batchSize);
    //         //     log.info("Successfully batch inserted {} new deals", totalInserted);
    //         // }
            
    //         // Batch update existing deals (only those with individualId)
    //         // if (!validdealsToSave.isEmpty()) {
    //         //     totalUpdated = employeeBatchService.batchUpdateDeals(validDealsToUpdate, organization, batchSize);
    //         //     log.info("Successfully batch updated {} deals", totalUpdated);
    //         // }
    //         for (int i = 0; i < dealsToSave.size(); i += batchSize) {
    //             int end = Math.min(i + batchSize, dealsToSave.size());
    //             List<Deals> batch = dealsToSave.subList(i, end);
    //             dealsRepository.saveAll(batch);
    //             totalSaved += batch.size();
    //         }
            
    //         log.info("Successfully saved {} deals ({} new, {} updated)", totalSaved, createdCount, updatedCount);
    //         return new EmployeeUploadResponse(totalSaved, createdCount, updatedCount, new ArrayList<>(), "Employees processed successfully", selfCount(employeeUploadDtoList).intValue(), dependentCount(employeeUploadDtoList).intValue());
    //     }
    //     catch (Exception e) {
    //         log.error("Error uploading employees: {}", e.getMessage(), e);
    //         throw new RuntimeException("Failed to save employees: " + e.getMessage(), e);
    //     }
    // }
    
    @Transactional(rollbackFor = Exception.class)
    public EmployeeUploadResponse uploadEmployees(List<EmployeeUploadDto> employeeUploadDtoList, Organization organization) {
        try {
          EmployeeUploadResponse response = new EmployeeUploadResponse();
          EmployeeUploadResponse validateResponse = validateEmployee(employeeUploadDtoList, organization);
          if (validateResponse.getErrorCount() > 0)
            return validateResponse; 
          Map<String, List<EmployeeUploadDto>> groupedByEmployeeId = groupByEmployeeId(employeeUploadDtoList);
          List<Deals> dealsToSave = new ArrayList<>();
          int updatedCount = 0;
          int createdCount = 0;
          List<String> allEmployeeIds = new ArrayList<>(groupedByEmployeeId.keySet());
          List<Deals> existingPrimaries = this.dealsRepository.findByEmployeeNumberInAndOrganizationIdAndRelationship(allEmployeeIds, organization
              .getOrganizationId(), NomineeRelationship.SELF.getValue());
          Map<String, Deals> primaryEmployeeMap = (Map<String, Deals>)existingPrimaries.stream().collect(Collectors.toMap(Deals::getEmployeeNumber, d -> d, (d1, d2) -> d1));
          List<String> phonesToCheckBulk = new ArrayList<>();
          List<String> emailsToCheckBulk = new ArrayList<>();
          Map<String, EmployeeUploadDto> phoneToEmployeeMap = new HashMap<>();
          Map<String, EmployeeUploadDto> emailToEmployeeMap = new HashMap<>();
          for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedByEmployeeId.entrySet()) {
            String employeeId = entry.getKey();
            List<EmployeeUploadDto> employeeGroup = entry.getValue();
            EmployeeUploadDto selfDto = employeeGroup.stream().filter(e -> "Self".equalsIgnoreCase(e.getRelationship())).findFirst().orElse(null);
            if (selfDto != null && !primaryEmployeeMap.containsKey(employeeId)) {
              String phone = (selfDto.getMobile() != null && !selfDto.getMobile().trim().isEmpty()) ? selfDto.getMobile().trim() : null;
              String email = (selfDto.getEmail() != null && !selfDto.getEmail().trim().isEmpty()) ? selfDto.getEmail().trim() : null;
              if (phone != null) {
                phonesToCheckBulk.add(phone);
                phoneToEmployeeMap.put(phone, selfDto);
              } 
              if (email != null) {
                emailsToCheckBulk.add(email);
                emailToEmployeeMap.put(email, selfDto);
              } 
            } 
          } 
          Set<String> existingPhones = new HashSet<>();
          Set<String> existingEmails = new HashSet<>();
          if (!phonesToCheckBulk.isEmpty() || !emailsToCheckBulk.isEmpty()) {
            List<Deals> existingDuplicates = this.dealsRepository.findByEmployeePhoneAndEmployeeEmail(phonesToCheckBulk, emailsToCheckBulk, organization
                .getOrganizationId());
            existingPhones = (Set<String>)existingDuplicates.stream().map(Deals::getPhone).filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet());
            existingEmails = (Set<String>)existingDuplicates.stream().map(Deals::getEmail).filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet());
          } 
          List<Deals> primaryEmployeesForDependents = new ArrayList<>();
          for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedByEmployeeId.entrySet()) {
            String employeeId = entry.getKey();
            List<EmployeeUploadDto> employeeGroup = entry.getValue();
            Deals primaryEmployee = null;
            EmployeeUploadDto selfDto = employeeGroup.stream().filter(e -> "Self".equalsIgnoreCase(e.getRelationship())).findFirst().orElse(null);
            if (selfDto != null) {
              Deals existingPrimary = primaryEmployeeMap.get(employeeId);
              if (existingPrimary != null) {
                primaryEmployee = existingPrimary;
                updateDealFromDto(primaryEmployee, selfDto, organization);
                Diff diff = javers.compare(existingPrimary, primaryEmployee);
                if(diff.hasChanges()) {
                primaryEmployee.setRelationship(mapRelationshipToNomineeRelationship("Self", 0));
                updatedCount++;
                log.debug("Updating existing primary employee: {}", employeeId);
                }
              } else {
                List<String> duplicateErrors = new ArrayList<>();
                String phone = (selfDto.getMobile() != null && !selfDto.getMobile().trim().isEmpty()) ? selfDto.getMobile().trim() : null;
                String email = (selfDto.getEmail() != null && !selfDto.getEmail().trim().isEmpty()) ? selfDto.getEmail().trim() : null;
                if (phone != null && existingPhones.contains(phone))
                  duplicateErrors.add("employeeId: " + employeeId + " - Phone number already exists: " + phone); 
                if (email != null && existingEmails.contains(email))
                  duplicateErrors.add("employeeId: " + employeeId + " - Email already exists: " + email); 
                if (!duplicateErrors.isEmpty()) {
                  validateResponse.getErrors().addAll(duplicateErrors);
                  return validateResponse;
                } 
                primaryEmployee = EmployeeToDeals.mapToDeals(selfDto, organization);
                primaryEmployee.setRelationship(mapRelationshipToNomineeRelationship("Self", 0));
                primaryEmployee.setCreatedAt(LocalDateTime.now());
                createdCount++;
                log.debug("Creating new primary employee: {}", employeeId);
              } 
              primaryEmployee.setUpdatedAt(LocalDateTime.now());
              dealsToSave.add(primaryEmployee);
              primaryEmployeesForDependents.add(primaryEmployee);
              continue;
            } 
            Deals existingEmployee = primaryEmployeeMap.get(employeeId);
            if (existingEmployee != null) {
              if (existingEmployee.getIsPrimaryMember() != null && existingEmployee.getIsPrimaryMember().booleanValue()) {
                primaryEmployee = existingEmployee;
                log.debug("Found existing primary employee for dependents: {}", employeeId);
              } else if (existingEmployee.getPrimaryIndividual() != null) {
                primaryEmployee = existingEmployee.getPrimaryIndividual();
                log.debug("Found existing primary employee through dependent: {}", employeeId);
              } else {
                validateResponse.getErrors().add("employeeId: " + employeeId + " - Employee exists but is a dependent without primary individual");
                return validateResponse;
              } 
              primaryEmployeesForDependents.add(primaryEmployee);
              continue;
            } 
            validateResponse.getErrors().add("employeeId: " + employeeId + " - Employee not found. Cannot add dependents without primary employee (Self). Please include Self in the upload or ensure the employee exists in the database.");
            return validateResponse;
          } 
          Map<String, Deals> allPrimaryEmployeesMap = new HashMap<>(primaryEmployeeMap);
          for (Deals deal : dealsToSave) {
            if (deal.getRelationship() != null && NomineeRelationship.SELF.getValue().equals(deal.getRelationship()) && deal
              .getEmployeeNumber() != null)
              allPrimaryEmployeesMap.put(deal.getEmployeeNumber(), deal); 
          } 
          for (Deals primary : primaryEmployeesForDependents) {
            if (primary != null && primary.getEmployeeNumber() != null && 
              !allPrimaryEmployeesMap.containsKey(primary.getEmployeeNumber()))
              allPrimaryEmployeesMap.put(primary.getEmployeeNumber(), primary); 
          } 
          List<UUID> primaryIndividualIds = (List<UUID>)primaryEmployeesForDependents.stream().filter(p -> (p.getIndividualId() != null)).map(Deals::getIndividualId).collect(Collectors.toList());
          Map<UUID, List<Deals>> dependentsByPrimaryId = new HashMap<>();
          if (!primaryIndividualIds.isEmpty()) {
            List<Deals> allExistingDependents = this.dealsRepository.findByPrimaryIndividualIdIn(primaryIndividualIds);
            dependentsByPrimaryId = (Map<UUID, List<Deals>>)allExistingDependents.stream().filter(d -> (d.getPrimaryIndividual() != null && d.getPrimaryIndividual().getIndividualId() != null)).collect(Collectors.groupingBy(d -> d.getPrimaryIndividual().getIndividualId()));
          } 
          for (Map.Entry<String, List<EmployeeUploadDto>> entry : groupedByEmployeeId.entrySet()) {
            String employeeId = entry.getKey();
            List<EmployeeUploadDto> employeeGroup = entry.getValue();
            Deals primaryEmployee = allPrimaryEmployeesMap.get(employeeId);
            if (primaryEmployee != null) {
              List<Deals> existingDependents = new ArrayList<>();
              if (primaryEmployee.getIndividualId() != null)
                existingDependents = dependentsByPrimaryId.getOrDefault(primaryEmployee
                    .getIndividualId(), new ArrayList<>()); 
              Map<String, List<Deals>> existingDependentsByRelationship = (Map<String, List<Deals>>)existingDependents.stream().filter(d -> (d.getRelationship() != null)).collect(Collectors.groupingBy(d -> d.getRelationship().toUpperCase(), 
                    
                    Collectors.toList()));
              for (EmployeeUploadDto dependentDto : employeeGroup) {
                if (!"Self".equalsIgnoreCase(dependentDto.getRelationship())) {
                  String mappedRelationship, inputRelationship = dependentDto.getRelationship();
                  Deals existingDependent = null;
                  if (inputRelationship != null && inputRelationship.toUpperCase().startsWith("CHILD")) {
                    int childIndexFromJson = extractChildIndexFromString(inputRelationship);
                    if (childIndexFromJson == 0) {
                      validateResponse.getErrors().add("employeeId: " + employeeId + " - Child relationship must have explicit index. Use Child1, Child2, Child3, or Child4");
                      return validateResponse;
                    } 
                    if (childIndexFromJson > 4) {
                      validateResponse.getErrors().add("employeeId: " + employeeId + " - Child index cannot be greater than 4. Maximum allowed: Child4");
                      return validateResponse;
                    } 
                    mappedRelationship = mapRelationshipToNomineeRelationship(inputRelationship, childIndexFromJson);
                    List<Deals> existingWithSameRelationship = existingDependentsByRelationship.getOrDefault(mappedRelationship
                        .toUpperCase(), new ArrayList<>());
                    if (!existingWithSameRelationship.isEmpty()) {
                      existingDependent = existingWithSameRelationship.get(0);
                      log.debug("Found existing child with index {} for employee {}, will update", 
                          Integer.valueOf(childIndexFromJson), employeeId);
                    } else {
                      for (int j = 1; j < childIndexFromJson; j++) {
                        String prevChildRel;
                        int prevChildIndex = j;
                        try {
                          prevChildRel = NomineeRelationship.valueOf("CHILD" + prevChildIndex).getValue();
                        } catch (IllegalArgumentException e) {
                          prevChildRel = "CHILD" + prevChildIndex;
                        } 
                        boolean existsInDatabase = existingDependentsByRelationship.containsKey(prevChildRel.toUpperCase());
                        boolean existsInUpload = employeeGroup.stream().anyMatch(dto -> {
                              if (dto.getRelationship() == null)
                                return false; 
                              int idx = extractChildIndexFromString(dto.getRelationship());
                              return (idx == prevChildIndex);
                            });
                        if (!existsInDatabase && !existsInUpload) {
                          validateResponse.getErrors().add("employeeId: " + employeeId + " - Cannot insert Child" + childIndexFromJson + " without Child" + prevChildIndex);
                          return validateResponse;
                        } 
                      } 
                      log.debug("Validated previous children exist (in database or upload), will create new Child{} for employee {}", 
                          Integer.valueOf(childIndexFromJson), employeeId);
                    } 
                  } else {
                    mappedRelationship = mapRelationshipToNomineeRelationship(inputRelationship, 0);
                    List<Deals> existingWithSameRelationship = existingDependentsByRelationship.getOrDefault(mappedRelationship
                        .toUpperCase(), new ArrayList<>());
                    if (!existingWithSameRelationship.isEmpty())
                      existingDependent = existingWithSameRelationship.get(0); 
                  } 
                  if (existingDependent != null) {
                    Deals existingDependentToCompare = new Deals();
                    updateDealFromDto(existingDependentToCompare, dependentDto, organization);
                    existingDependentToCompare.setIndividualId(existingDependent.getIndividualId());
                    existingDependentToCompare.setCreatedAt(existingDependent.getCreatedAt());
                    existingDependentToCompare.setPrimaryIndividual(existingDependent.getPrimaryIndividual());
                    existingDependentToCompare.setRelationship(existingDependent.getRelationship());
                    existingDependentToCompare.setStatus(existingDependent.getStatus());
                    existingDependentToCompare.setUpdatedAt(existingDependent.getUpdatedAt());
                    Diff diff = javers.compare(existingDependent,existingDependentToCompare);
                    if(diff.hasChanges()) {
                    log.info("Differences found in existing dependent: {}", diff.prettyPrint());
                    existingDependentToCompare.setRelationship(mappedRelationship);
                    existingDependentToCompare.setPrimaryIndividual(primaryEmployee);
                    existingDependentToCompare.setUpdatedAt(LocalDateTime.now());
                    dealsToSave.add(existingDependentToCompare);
                    updatedCount++;
                    log.debug("Updating existing dependent: {} - {} (mapped to {})", new Object[] { employeeId, inputRelationship, mappedRelationship });
                    }
                  } else {
                  Deals newDependent = EmployeeToDeals.mapToDeals(dependentDto, organization);
                  newDependent.setEmployeeNumber(employeeId);
                  newDependent.setRelationship(mappedRelationship);
                  newDependent.setPrimaryIndividual(primaryEmployee);
                  newDependent.setCreatedAt(LocalDateTime.now());
                  newDependent.setUpdatedAt(LocalDateTime.now());
                  dealsToSave.add(newDependent);
                  createdCount++;
                  log.debug("Creating new dependent: {} - {} (mapped to {})", new Object[] { employeeId, inputRelationship, mappedRelationship });
                   } 
                  } 
              } 
            } 
          } 
          int batchSize = 1000;
          int totalSaved = 0;
          log.info("Saving {} deals ({} new, {} updated) in batches of {}", new Object[] { Integer.valueOf(dealsToSave.size()), Integer.valueOf(createdCount), Integer.valueOf(updatedCount), Integer.valueOf(batchSize) });
          int i;
          if(updatedCount > 0 || createdCount > 0) {
          for (i = 0; i < dealsToSave.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dealsToSave.size());
            List<Deals> batch = dealsToSave.subList(i, end);
            this.dealsRepository.saveAll(batch);
            totalSaved += batch.size();
          } 
        }
          log.info("Successfully saved {} deals ({} new, {} updated)", new Object[] { Integer.valueOf(totalSaved), Integer.valueOf(createdCount), Integer.valueOf(updatedCount) });
          response.setTotalRows(employeeUploadDtoList.size());
          response.setTotalEmployees(selfCount(employeeUploadDtoList).intValue());
          response.setTotalDependents(dependentCount(employeeUploadDtoList).intValue());
          response.setSuccessCount(totalSaved);
          response.setErrorCount(0);
          response.setErrors(new ArrayList());
          response.setMessage(String.format("Employees processed successfully: %d created, %d updated", new Object[] { Integer.valueOf(createdCount), Integer.valueOf(updatedCount) }));
          return response;
        } catch (Exception e) {
          log.error("Error uploading employees: {}", e.getMessage(), e);
          throw new RuntimeException("Failed to save employees: " + e.getMessage(), e);
        } 
      }

    
    public Long selfCount(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().filter(e -> Objects.equals(e.getRelationship(), "Self")).count();
    }

    public Long dependentCount(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().filter(e -> !Objects.equals(e.getRelationship(), "Self")).count();
    }


    public List<String> getEmployeeIds(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().distinct().map(EmployeeUploadDto::getEmployeeId).collect(Collectors.toList());
    }

    public List<String> getEmployeePhones(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().distinct().map(EmployeeUploadDto::getMobile).collect(Collectors.toList());
    }

    public List<String> getEmployeeEmails(List<EmployeeUploadDto> employeeUploadDtoList) {
        return employeeUploadDtoList.stream().distinct().map(EmployeeUploadDto::getEmail).collect(Collectors.toList());
    }

   public EmployeeUploadResponse validateBulkEmployeeDeletion(List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoList, Organization organization) {
    List<String> errors = new ArrayList<>();
    EmployeeUploadResponse response = new EmployeeUploadResponse();
    try{
        for (BulkEmployeeDeletionRequestDto bulkEmployeeDeletionRequestDto : bulkEmployeeDeletionRequestDtoList) {
            Set<ConstraintViolation<BulkEmployeeDeletionRequestDto>> violations = validator.validate(bulkEmployeeDeletionRequestDto);
            if (!violations.isEmpty()) {
                errors.addAll(violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList()));
            }
        }
        List<String> employeeIds = bulkEmployeeDeletionRequestDtoList.stream().map(BulkEmployeeDeletionRequestDto::getEmployeeId).collect(Collectors.toList());
        List<Deals> dealsList = dealsRepository.findByEmployeeNumberInAndOrganizationId(employeeIds, organization.getOrganizationId());
        if(dealsList.isEmpty()) {
            errors.addAll(employeeIds.stream().map(id -> "employeeId: " + id + " - Employee not found").collect(Collectors.toList()));
        }
        response.setTotalRows(bulkEmployeeDeletionRequestDtoList.size());
        response.setTotalEmployees(employeeIds.size());
        response.setTotalDependents(0);
        response.setSuccessCount(employeeIds.size() - errors.size());
        response.setErrorCount(errors.size());
        response.setErrors(errors);
        response.setMessage(errors.isEmpty() ? "No Validation errors!" : "Validation errors");
        return response;
    } catch (Exception e) {
        return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), e.getMessage(), 0, 0);
    }
   }

    public EmployeeUploadResponse deleteEmployee(List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoList, Organization organization) {
        try{
            Set<UUID> individualIdsToDelete = new HashSet<>();
            List<String> errors = new ArrayList<>();
            int deletedCount = 0;
            int employeeCount = 0;
            int dependentCount = 0;
            EmployeeUploadResponse validateResponse = validateBulkEmployeeDeletion(bulkEmployeeDeletionRequestDtoList, organization);
            if(validateResponse.getSuccessCount() == 0) {
                return validateResponse;
            }
            for (Map.Entry<String, List<BulkEmployeeDeletionRequestDto>> entry : groupByEmployeeIdForBulkEmployeeDeletion(bulkEmployeeDeletionRequestDtoList).entrySet()) {
                List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoListByEmployeeId = entry.getValue();
                for (BulkEmployeeDeletionRequestDto bulkEmployeeDeletionRequestDto : bulkEmployeeDeletionRequestDtoListByEmployeeId) {
                    if(bulkEmployeeDeletionRequestDto.getRelationship().equalsIgnoreCase("Self")) {
                        Optional<Deals> deal = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(bulkEmployeeDeletionRequestDto.getEmployeeId(), organization.getOrganizationId(), "SELF");
                        if(deal.isPresent()) {
                            List<Deals> dependents = dealsRepository.findByPrimaryIndividualIdIn(List.of(deal.get().getIndividualId()));
                            dependents.forEach(dependent -> individualIdsToDelete.add(dependent.getIndividualId()));
                            individualIdsToDelete.add(deal.get().getIndividualId());
                            employeeCount++;
                            dependentCount += dependents.size();
                        }
                        else {
                            errors.add("employeeId: " + bulkEmployeeDeletionRequestDto.getEmployeeId() + " - Employee not found");
                        }
                    } else {
                        final int childIndexFromJson = extractChildIndexFromString(bulkEmployeeDeletionRequestDto.getRelationship());
                        String relationship = mapRelationshipToNomineeRelationship(bulkEmployeeDeletionRequestDto.getRelationship(), childIndexFromJson);
                        Optional<Deals> deal = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(bulkEmployeeDeletionRequestDto.getEmployeeId(), organization.getOrganizationId(), relationship);
                        if(deal.isPresent()) {
                            if(individualIdsToDelete.contains(deal.get().getIndividualId())) {
                                dependentCount++;
                            }
                            individualIdsToDelete.add(deal.get().getIndividualId());
                        }
                        else {
                            errors.add("employeeId: " + bulkEmployeeDeletionRequestDto.getEmployeeId() + " - Dependent not found");
                        }
                    }
                }
            }
            if(individualIdsToDelete.isEmpty()) {
                return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), "No individuals to delete", 0, 0);
            }
            if(!errors.isEmpty()) {
                return new EmployeeUploadResponse(0, 0, errors.size(), errors, "Errors occurred while deleting employees", 0, 0);
            }
            List<Deals> dealsToDelete = dealsRepository.findByIndividualIdIn(new ArrayList<>(individualIdsToDelete));
            dealsToDelete.forEach(deal -> deal.setStatus(AccountStatus.PENDING_DELETE));
            dealsRepository.saveAll(dealsToDelete);
            return new EmployeeUploadResponse(dealsToDelete.size(), deletedCount, 0, new ArrayList<>(), "Employees" + "(" + deletedCount + ")" + " and dependents" + "(" + dependentCount + ")" + " deleted successfully", employeeCount, dependentCount);
        }
        catch (Exception e) { 
            return new EmployeeUploadResponse(0, 0, 0, new ArrayList<>(), e.getMessage(), 0, 0);
        }
    }   



    private void updateDealFromDto(Deals existingDeal, EmployeeUploadDto dto, Organization organization) {
        // Parse name
        String name = dto.getName() != null ? dto.getName().trim() : "";
        String[] nameParts = name.split("\\s+", 2);
        existingDeal.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            existingDeal.setLastName(nameParts[1]);
        } else {
            existingDeal.setLastName(null);
        }
        
        // Update fields
        existingDeal.setDateOfBirth(parseDate(dto.getDateOfBirth()));
        existingDeal.setGender(dto.getGender() != null ? dto.getGender().trim() : null);
        existingDeal.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        existingDeal.setPhone(dto.getMobile() != null ? dto.getMobile().trim() : null);
        existingDeal.setDateOfJoining(parseDate(dto.getDateOfJoining()));
        existingDeal.setDesignation(dto.getDesignation() != null ? dto.getDesignation().trim() : null);
        // Note: Relationship mapping is handled in uploadEmployees method
        existingDeal.setOrganization(organization);
        existingDeal.setUpdatedAt(LocalDateTime.now());
        existingDeal.setFullName(dto.getName());
        existingDeal.setMaritalStatus(dto.getMaritalStatus());
        existingDeal.setSumInsured(dto.getSumInsured());
        
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
            existingDeal.setEmployeeNumber(dto.getEmployeeId());
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts child index from relationship string
     * Examples: "Child1" -> 1, "Child2" -> 2, "Child" -> 0 (no index)
     */
    private int extractChildIndexFromString(String relationship) {
        if (relationship == null || relationship.trim().isEmpty()) {
            return 0;
        }
        String rel = relationship.trim();
        
        // Check if it's "Child" without index
        if ("Child".equalsIgnoreCase(rel)) {
            return 0;
        }
        
        // Check if it starts with "Child" and has a number
        if (rel.toUpperCase().startsWith("CHILD")) {
            String remaining = rel.substring(5).trim(); // Remove "Child" prefix
            if (remaining.isEmpty()) {
                return 0;
            }
            try {
                // Try to parse the number (e.g., "1", "2", "3", "4")
                return Integer.parseInt(remaining);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        return 0;
    }

    /**
     * Maps input relationship string to NomineeRelationship enum value (as string)
     * Maps: Self -> SELF, Spouse -> SPOUSE, Father -> FATHER, Mother -> MOTHER, 
     *       Father in law -> FATHER_IN_LAW, Mother in law -> MOTHER_IN_LAW,
     *       Child -> CHILD1/CHILD2/CHILD3/CHILD4
     */
    private String mapRelationshipToNomineeRelationship(String relationship, int childIndex) {
        if (relationship == null || relationship.trim().isEmpty()) {
            return null;
        }
        String rel = relationship.trim();
        
        if ("Self".equalsIgnoreCase(rel)) {
            return NomineeRelationship.SELF.getValue();
        } else if ("Spouse".equalsIgnoreCase(rel)) {
            return NomineeRelationship.SPOUSE.getValue();
        } else if ("Father".equalsIgnoreCase(rel)) {
            return NomineeRelationship.FATHER.getValue();
        } else if ("Mother".equalsIgnoreCase(rel)) {
            return NomineeRelationship.MOTHER.getValue();
        } else if (rel.equalsIgnoreCase("Father in law") || rel.equalsIgnoreCase("FatherInLaw") || 
                   rel.equalsIgnoreCase("FATHER_IN_LAW")) {
            return NomineeRelationship.FATHER_IN_LAW.getValue();
        } else if (rel.equalsIgnoreCase("Mother in law") || rel.equalsIgnoreCase("MotherInLaw") || 
                   rel.equalsIgnoreCase("MOTHER_IN_LAW")) {
            return NomineeRelationship.MOTHER_IN_LAW.getValue();
        } else if ("Child".equalsIgnoreCase(rel) || rel.toUpperCase().startsWith("CHILD")) {
            // Map Child to CHILD1, CHILD2, CHILD3, or CHILD4 based on index
            return switch (childIndex) {
                case 1 -> NomineeRelationship.CHILD1.getValue();
                case 2 -> NomineeRelationship.CHILD2.getValue();
                case 3 -> NomineeRelationship.CHILD3.getValue();
                case 4 -> NomineeRelationship.CHILD4.getValue();
                default -> NomineeRelationship.CHILD1.getValue(); // Fallback
            };
        }
        
        // If we reach here, the relationship is not supported
        throw new IllegalArgumentException("Unsupported relationship: " + relationship + 
            ". Allowed relationships: Self, Spouse, Father, Mother, Father in law, Mother in law, Child1, Child2, Child3, Child4");
    }
    
    /**
     * Validates if the relationship is one of the allowed values
     * Allowed: Self, Spouse, Father, Mother, Father in law, Mother in law, Child1, Child2, Child3, Child4
     */
    private boolean isValidRelationship(String relationship) {
        if (relationship == null || relationship.trim().isEmpty()) {
            return false;
        }
        String rel = relationship.trim();
        
        // Check for allowed relationships
        if ("Self".equalsIgnoreCase(rel)) {
            return true;
        } else if ("Spouse".equalsIgnoreCase(rel)) {
            return true;
        } else if ("Father".equalsIgnoreCase(rel)) {
            return true;
        } else if ("Mother".equalsIgnoreCase(rel)) {
            return true;
        } else if (rel.equalsIgnoreCase("Father in law") || rel.equalsIgnoreCase("FatherInLaw") || 
                   rel.equalsIgnoreCase("FATHER_IN_LAW")) {
            return true;
        } else if (rel.equalsIgnoreCase("Mother in law") || rel.equalsIgnoreCase("MotherInLaw") || 
                   rel.equalsIgnoreCase("MOTHER_IN_LAW")) {
            return true;
        } else if (rel.toUpperCase().startsWith("CHILD")) {
            // Check if it's Child1, Child2, Child3, or Child4
            int childIndex = extractChildIndexFromString(rel);
            return childIndex >= 1 && childIndex <= 4;
        }
        
        return false;
    }
}
