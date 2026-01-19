package com.vimainsurance.vimaadmin.service.serviceimpl;

import org.springframework.stereotype.Service;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.HRDashBoardResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationActivityDto;
import com.vimainsurance.vimaadmin.service.IHRService;
import com.vimainsurance.vimaadmin.util.TenantContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;

import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EndorsementActivityDto;
import com.vimainsurance.vimaadmin.specification.DealsSpecification;
import com.vimainsurance.vimaadmin.specification.EndorsementSpecification;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.TreeMap;

import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.dto.OrganizationActivityDto.MonthlyEndorsementActivityDto;
import com.vimainsurance.vimaadmin.dto.ClaimsActivityDto;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.EndorsementType;
import java.math.BigDecimal;

@Service
@Slf4j
public class HRServiceImpl implements IHRService {

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Autowired
    private IEndorsementRepository endorsementRepository;


    @Autowired
    private IDealsRepository dealsRepository;

    @Override
    public ResponseEntity<ResponseDto<HRDashBoardResponseDto>> getHrDashboard(LocalDateTime startDate, LocalDateTime endDate, String companyId) {
        log.info("[correlationId:{}] getOrganizationActivity called with startDate: {}, endDate: {}", 
                MDC.get("correlationId"), startDate, endDate);
        BaseResponse<HRDashBoardResponseDto> responseObj = new BaseResponse<>();
        try {
            // Multi-tenant: restrict by organization IDs from JWT
            List<String> orgIds = new ArrayList<>();
            if(companyId != null && !companyId.isEmpty()){
                orgIds.add(companyId);
            }
            else{
                Map<String, List<String>> tenantMap = TenantContext.getCurrentTenant();
                orgIds = (tenantMap != null) ? tenantMap.get("organizationIds") : null;
            }
           if(orgIds == null || orgIds.isEmpty()){
            return responseObj.render(responseObj.formErrorResponse("No organization IDs found"));
           }
           
           // Batch fetch all organizations at once for better performance
           List<UUID> organizationUuids = orgIds.stream()
               .map(UUID::fromString)
               .collect(Collectors.toList());
           
           List<Organization> organizations = organizationRepository.findAllById(organizationUuids);
           
           if (organizations.isEmpty()) {
               return responseObj.render(responseObj.formErrorResponse("No organizations found"));
           }
           
           // Create a map for quick lookup
           Map<UUID, Organization> orgMap = organizations.stream()
               .collect(Collectors.toMap(Organization::getOrganizationId, Function.identity()));
           
           // Filter valid organizations
           List<UUID> validOrgIds = organizationUuids.stream()
               .filter(orgMap::containsKey)
               .collect(Collectors.toList());
           
           // Initialize aggregated totals using AtomicLong for thread-safe accumulation
           AtomicLong totalEmployees = new AtomicLong(0L);
           AtomicLong totalDependents = new AtomicLong(0L);
           AtomicLong totalEndorsementCount = new AtomicLong(0L);
           AtomicLong totalCompletedEndorsements = new AtomicLong(0L);
           AtomicLong totalPendingEndorsements = new AtomicLong(0L);
           AtomicLong totalAdditions = new AtomicLong(0L);
           AtomicLong totalDeletions = new AtomicLong(0L);
           AtomicLong totalInactives = new AtomicLong(0L);
           
           if (!validOrgIds.isEmpty()) {
               // Process organizations in parallel - aggregate all counts
               validOrgIds.parallelStream()
                   .forEach(orgId -> {
                       // Execute all count queries concurrently for this organization
                       Long employees = dealsRepository.count(DealsSpecification.countEmployeesByOrganizationId(orgId));
                       Long dependents = dealsRepository.count(DealsSpecification.countDependentsByOrganizationId(orgId));
                       Long endorsementCount = endorsementRepository.count(EndorsementSpecification.countByOrganizationId(orgId));
                       Long completedEndorsements = endorsementRepository.count(EndorsementSpecification.countCompletedByOrganizationId(orgId));
                       Long pendingEndorsements = endorsementRepository.count(EndorsementSpecification.countPendingByOrganizationId(orgId));
                       Long additions = dealsRepository.countDealsForEndorsementAdditions(List.of(orgId), null, null);
                       Long deletions = dealsRepository.countDealsForEndorsementDeletions(List.of(orgId), EndorsementType.DELETION.name(), null, null);
                       Long inactives = (long) dealsRepository.findByOrganizationIdAndStatusIn(orgId, List.of(AccountStatus.INACTIVE)).size();
                       // Aggregate counts thread-safely
                       totalEmployees.addAndGet(employees != null ? employees : 0L);
                       totalDependents.addAndGet(dependents != null ? dependents : 0L);
                       totalEndorsementCount.addAndGet(endorsementCount != null ? endorsementCount : 0L);
                       totalCompletedEndorsements.addAndGet(completedEndorsements != null ? completedEndorsements : 0L);
                       totalPendingEndorsements.addAndGet(pendingEndorsements != null ? pendingEndorsements : 0L);
                       totalAdditions.addAndGet(additions != null ? additions : 0L);
                       totalDeletions.addAndGet(deletions != null ? deletions : 0L);
                       totalInactives.addAndGet(inactives != null ? inactives : 0L);
                   });
           }
           
           // Build aggregated DTOs
           long totalEmp = totalEmployees.get();
           long totalDep = totalDependents.get();
           long totalActiveLives = totalEmp + totalDep;
           
           EndorsementActivityDto endorsementActivityDto = new EndorsementActivityDto();
           endorsementActivityDto.setEndorsementCount(totalEndorsementCount.get());
           endorsementActivityDto.setCompletedEndorsements(totalCompletedEndorsements.get());
           endorsementActivityDto.setPendingEndorsements(totalPendingEndorsements.get());
           
           // Get monthly endorsement activity (additions and deletions)
           List<MonthlyEndorsementActivityDto> monthlyEndorsementActivity = getMonthlyEndorsementActivity(
               validOrgIds, startDate, endDate);
           
           OrganizationActivityDto organizationActivityDto = new OrganizationActivityDto();
           organizationActivityDto.setActiveLives(totalActiveLives);
           organizationActivityDto.setTotalInactives(totalInactives.get());
           organizationActivityDto.setTotalAdditions(totalAdditions.get());
           organizationActivityDto.setTotalDeletions(totalDeletions.get());
           organizationActivityDto.setEmployees(totalEmp);
           organizationActivityDto.setDependents(totalDep);
           organizationActivityDto.setEndorsementActivity(endorsementActivityDto);
           organizationActivityDto.setMonthlyEndorsementActivity(monthlyEndorsementActivity);
           HRDashBoardResponseDto hrDashboardResponse = new HRDashBoardResponseDto();
           hrDashboardResponse.setOrganizationActivityDto(organizationActivityDto);
           
           // TODO: Implement claims activity service logic
           // This will be implemented later to fetch:
           // - Total claims count and amount
           // - Monthly claims data (count and amount per month)
           // - Filter by organization IDs from tenant context
           // - Filter by date range (startDate, endDate)
           // Example implementation:
           // ClaimsActivityDto claimsActivityDto = getClaimsActivity(validOrgIds, startDate, endDate);
           
           // For now, return empty claims data
           ClaimsActivityDto claimsActivityDto = new ClaimsActivityDto();
           claimsActivityDto.setMonthlyClaimsActivity(new ArrayList<ClaimsActivityDto.MonthlyClaimsActivityDto>(Arrays.asList(new ClaimsActivityDto.MonthlyClaimsActivityDto("Jan 2026", 0L, BigDecimal.ZERO))));
           
           hrDashboardResponse.setClaimsActivityDto(claimsActivityDto);
           
           return responseObj.render(responseObj.formSuccessResponse("HR Dashboard", hrDashboardResponse));
        } catch (Exception e) {
            log.error("[correlationId:{}] Exception in getOrganizationActivity: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error Occured while getting HR Dashboard"));
        }
    }

    /**
     * Get monthly endorsement activity (additions and deletions) for given organizations and date range
     */
    private List<MonthlyEndorsementActivityDto> getMonthlyEndorsementActivity(
            List<UUID> organizationIds, LocalDateTime startDate, LocalDateTime endDate) {
        
        if (organizationIds == null || organizationIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Provide default dates if null (all dates)
        LocalDateTime effectiveStartDate = startDate != null ? startDate : LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        LocalDateTime effectiveEndDate = endDate != null ? endDate : LocalDateTime.of(9999, 12, 31, 23, 59, 59);
        
        // Get monthly additions
        List<Object[]> additions = endorsementRepository.getMonthlyEndorsementAdditions(
            organizationIds, effectiveStartDate, effectiveEndDate);
        
        // Get monthly deletions (status INACTIVE)
        List<Object[]> deletions = endorsementRepository.getMonthlyEndorsementDeletions(
            organizationIds, EndorsementType.DELETION, effectiveStartDate, effectiveEndDate);
        
        // Create maps for quick lookup: YearMonth -> count
        Map<YearMonth, Long> additionsMap = additions.stream()
            .collect(Collectors.toMap(
                arr -> YearMonth.of(((Number) arr[0]).intValue(), ((Number) arr[1]).intValue()),
                arr -> ((Number) arr[2]).longValue(),
                (v1, v2) -> v1 + v2,
                TreeMap::new
            ));
        
        Map<YearMonth, Long> deletionsMap = deletions.stream()
            .collect(Collectors.toMap(
                arr -> YearMonth.of(((Number) arr[0]).intValue(), ((Number) arr[1]).intValue()),
                arr -> ((Number) arr[2]).longValue(),
                (v1, v2) -> v1 + v2,
                TreeMap::new
            ));
        
        // Combine all months (from both additions and deletions)
        TreeMap<YearMonth, Long> allMonths = new TreeMap<>();
        allMonths.putAll(additionsMap);
        deletionsMap.forEach((month, count) -> allMonths.putIfAbsent(month, 0L));
        
        // Build DTOs
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        return allMonths.entrySet().stream()
            .map(entry -> {
                YearMonth yearMonth = entry.getKey();
                Long additionsCount = additionsMap.getOrDefault(yearMonth, 0L);
                Long deletionsCount = deletionsMap.getOrDefault(yearMonth, 0L);
                
                MonthlyEndorsementActivityDto dto = new MonthlyEndorsementActivityDto();
                dto.setMonth(yearMonth.format(formatter));
                dto.setAdditions(additionsCount);
                dto.setDeletions(deletionsCount);
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * Get claims activity for given organizations and date range
     * TODO: Implement this method to fetch claims data from repository
     * This method should:
     * 1. Query claims repository/specification for total counts and amounts
     * 2. Get monthly aggregated claims data (count and amount per month)
     * 3. Filter by organization IDs and date range
     * 4. Return ClaimsActivityDto with monthly breakdown
     * 
     * @param organizationIds List of organization UUIDs
     * @param startDate Start date for filtering (optional)
     * @param endDate End date for filtering (optional)
     * @return ClaimsActivityDto containing total and monthly claims data
     */
    // private ClaimsActivityDto getClaimsActivity(List<UUID> organizationIds, LocalDateTime startDate, LocalDateTime endDate) {
    //     // Implementation will be added later
    //     // Example structure:
    //     // - Get total claims count: claimsRepository.count(...)
    //     // - Get total amount: claimsRepository.sumAmount(...)
    //     // - Get monthly data: claimsRepository.getMonthlyClaimsData(...)
    //     // - Build ClaimsActivityDto with monthly breakdown
    //     return new ClaimsActivityDto();
    // }
}

