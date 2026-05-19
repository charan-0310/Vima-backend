package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigResponseDto;
import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentContext;
import com.vimainsurance.vimaadmin.dto.EnrollmentContextDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentOrganizationPolicyDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionSummaryDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.mapper.EnrollmentSubmissionMapper;
import com.vimainsurance.vimaadmin.mapper.EnrollmentWindowMapper;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.service.ICompanyEnrollmentConfigService;
import com.vimainsurance.vimaadmin.service.IEnrollmentService;
import com.vimainsurance.vimaadmin.service.IEnrollmentTokenService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class EnrollmentServiceImpl implements IEnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    @Autowired
    private IEnrollmentTokenService enrollmentTokenService;
    @Autowired
    private IEnrollmentWindowsRepository enrollmentWindowsRepository;
    @Autowired
    private IDealsRepository dealsRepository;
    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;
    @Autowired
    private IPolicyRepository policyRepository;
    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;
    @Autowired
    private ICompanyEnrollmentConfigService companyEnrollmentConfigService;

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<EnrollmentContextDto>> validateTokenAndGetContext(
            String token, HttpServletRequest request) {
        logger.info("[correlationId:{}] Enrollment validateTokenAndGetContext called", MDC.get("correlationId"));
        BaseResponse<EnrollmentContextDto> responseObj = new BaseResponse<>();
        EnrollmentContext context = enrollmentTokenService.validateToken(token, request);
        EnrollmentContextDto dto = buildEnrollmentContextDto(context);
        return responseObj.render(responseObj.formSuccessResponse("Token valid", dto));
    }

    @Override
    public ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getSubmissionsByToken(
            String token, HttpServletRequest request) {
        logger.info("[correlationId:{}] Enrollment getSubmissionsByToken called", MDC.get("correlationId"));
        BaseResponse<List<EnrollmentSubmissionResponseDto>> responseObj = new BaseResponse<>();
        EnrollmentContext context = enrollmentTokenService.validateToken(token, request);
        UUID employeeId = context.getEmployeeId();
        List<EnrollmentSubmission> submissions =
                enrollmentSubmissionRepository.findAllByEmployee_IndividualId(employeeId);
        List<EnrollmentSubmissionResponseDto> out = new ArrayList<>();
        for (EnrollmentSubmission submission : submissions) {
            out.add(EnrollmentSubmissionMapper.mapToResponseDto(submission));
        }
        return responseObj.render(responseObj.formSuccessResponse("Success", out, out.size()));
    }

    @Override
    public ResponseEntity<ResponseDto<CompanyEnrollmentConfigResponseDto>> getEnrollmentConfigByToken(
            String token, HttpServletRequest request) {
        BaseResponse<CompanyEnrollmentConfigResponseDto> responseObj = new BaseResponse<>();
        EnrollmentContext context = enrollmentTokenService.validateToken(token, request);
        Optional<EnrollmentWindows> windowOpt = enrollmentWindowsRepository.findById(context.getWindowId());
        if (windowOpt.isEmpty() || windowOpt.get().getOrganization() == null) {
            return responseObj.render(responseObj.formErrorResponse(400, "Organization not found"));
        }
        UUID orgId = windowOpt.get().getOrganization().getOrganizationId();
        CompanyEnrollmentConfigResponseDto config = companyEnrollmentConfigService.getConfigForCompany(orgId);
        return responseObj.render(responseObj.formSuccessResponse("OK", config));
    }

    private EnrollmentContextDto buildEnrollmentContextDto(EnrollmentContext context) {
        UUID enrollmentWindowId = context.getWindowId();
        UUID employeeId = context.getEmployeeId();

        EnrollmentWindows enrollmentWindow = enrollmentWindowsRepository.findById(enrollmentWindowId)
                .orElseThrow(() -> new IllegalStateException("Enrollment window not found"));
        EnrollmentWindowResponseDto enrollmentWindowDto = EnrollmentWindowMapper.mapToResponseDto(enrollmentWindow);

        Deals employeeDeal = dealsRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException("Employee not found"));
        DealsResponseDto employeeDto = mapDealToResponseDto(employeeDeal);

        EnrollmentSubmission submission = enrollmentSubmissionRepository.findById(context.getSubmissionId())
                .orElseThrow(() -> new IllegalStateException("Submission not found"));

        EnrollmentContextDto dto = new EnrollmentContextDto();
        dto.setEnrollmentWindowId(enrollmentWindowId);
        dto.setEmployeeId(employeeId);
        dto.setSubmissionId(submission.getId());
        dto.setInvitationId(context.getInvitationId());
        dto.setEnrollmentWindow(enrollmentWindowDto);
        dto.setEmployee(employeeDto);
        dto.setSubmissionSummary(toSubmissionSummary(submission));

        if (enrollmentWindow.getOrganization() != null) {
            UUID orgId = enrollmentWindow.getOrganization().getOrganizationId();
            List<Policy> policies = policyRepository.findByOrganizationId(orgId);
            List<EnrollmentOrganizationPolicyDto> policyDtos = new ArrayList<>();
            for (Policy p : policies) {
                EnrollmentOrganizationPolicyDto pd = new EnrollmentOrganizationPolicyDto();
                pd.setPolicyId(p.getPolicyId());
                pd.setPolicyNumber(p.getPolicyNumber());
                pd.setProductType(p.getProductType() != null ? p.getProductType().name() : null);
                pd.setSumInsured(p.getSumInsured());
                pd.setCoverageAmount(p.getSumInsured());
                pd.setCoverageType(p.getCoverageType() != null ? p.getCoverageType().name() : null);
                pd.setSumInsuredMultiplier(p.getSumInsuredMultiplier());
                pd.setInsurerName(p.getInsuranceProviderId() != null
                        ? insuranceProviderRepository.findById(p.getInsuranceProviderId())
                                .orElseThrow(() -> new RuntimeException("Insurance provider not found"))
                                .getProviderName()
                        : null);
                LocalDate effStart = p.getEffectiveFrom() != null ? p.getEffectiveFrom() : p.getStartDate();
                pd.setEffectiveFrom(effStart != null ? effStart.toString() : null);
                pd.setPolicyStatus(p.getStatus() != null ? p.getStatus().name() : null);
                policyDtos.add(pd);
            }
            dto.setOrganizationPolicies(policyDtos);
            dto.setCompanyEnrollmentConfig(companyEnrollmentConfigService.getConfigForCompany(orgId));
        } else {
            dto.setOrganizationPolicies(new ArrayList<>());
        }
        return dto;
    }

    private DealsResponseDto mapDealToResponseDto(Deals deal) {
        DealsResponseDto dto = new DealsResponseDto();
        dto.setIndividualId(deal.getIndividualId());
        dto.setFirstName(deal.getFirstName());
        dto.setLastName(deal.getLastName());
        dto.setFullName(deal.getFullName());
        dto.setEmail(deal.getEmail());
        dto.setPhone(deal.getPhone());
        dto.setDateOfBirth(deal.getDateOfBirth());
        dto.setGender(deal.getGender());
        dto.setPanNumber(deal.getPanNumber());
        dto.setAadhaarNumber(deal.getAadhaarNumber());
        dto.setAddress(deal.getAddress());
        dto.setCity(deal.getCity());
        dto.setState(deal.getState());
        dto.setPincode(deal.getPincode());
        dto.setEmployeeNumber(deal.getEmployeeNumber());
        dto.setRelationship(deal.getRelationship());
        dto.setActualRelationship(deal.getActualRelationship());
        dto.setDesignation(deal.getDesignation());
        dto.setDateOfJoining(deal.getDateOfJoining());
        dto.setIsPrimaryMember(deal.getIsPrimaryMember());
        dto.setUsername(deal.getUsername());
        dto.setPreferredLanguage(deal.getPreferredLanguage());
        dto.setLeadId(deal.getLeadId());
        dto.setCustId(deal.getCustId());
        dto.setMaritalStatus(deal.getMaritalStatus());
        dto.setSumInsured(deal.getSumInsured());
        dto.setCreatedAt(deal.getCreatedAt());
        dto.setUpdatedAt(deal.getUpdatedAt());
        dto.setHealthId(deal.getHealthId());
        dto.setPolicies(null);
        if (deal.getAccountType() != null) {
            dto.setAccountType(deal.getAccountType().getValue());
        }
        if (deal.getStatus() != null) {
            dto.setAccountStatus(deal.getStatus().getValue());
        }
        return dto;
    }

    private static EnrollmentSubmissionSummaryDto toSubmissionSummary(EnrollmentSubmission submission) {
        EnrollmentSubmissionSummaryDto summary = new EnrollmentSubmissionSummaryDto();
        summary.setSubmissionId(submission.getId());
        EnrollementStatus st = submission.getStatus();
        summary.setWorkflowStatus(st != null ? st.getValue() : null);
        String stage = submission.getStage();
        summary.setCurrentStep(stage != null ? stage : "");
        summary.setSubmittedAt(submission.getSubmittedAt());
        summary.setDerivedLifecycle(deriveEnrollmentLifecycle(submission));
        return summary;
    }

    private static String deriveEnrollmentLifecycle(EnrollmentSubmission submission) {
        EnrollementStatus st = submission.getStatus();
        if (st == null) {
            return "UNKNOWN";
        }
        String pd = submission.getPersonalDetails();
        boolean started = pd != null && !pd.isBlank() && !"{}".equals(pd.trim());
        switch (st) {
            case APPROVED:
                return "APPROVED";
            case REJECTED:
                return "REJECTED";
            case ENDORSED:
                return "ENDORSED";
            case SUBMITTED:
                return "SUBMITTED";
            case PENDING_APPROVAL:
                return "PENDING_APPROVAL";
            case DRAFT:
                return started ? "IN_PROGRESS" : "NOT_STARTED";
            default:
                return st.getValue();
        }
    }
}
