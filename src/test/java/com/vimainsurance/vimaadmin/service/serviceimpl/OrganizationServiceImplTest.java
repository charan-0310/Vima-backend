package com.vimainsurance.vimaadmin.service.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.OrganizationRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.CostSharingRule;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.CoverageCategory;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICostSharingRuleRepository;
import com.vimainsurance.vimaadmin.repository.IDealEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IS3Service;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.KeyCloakUtil;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

    @Mock
    private EmployeeService employeeService;
    @Mock
    private IOrganizationRepository organizationRepository;
    @Mock
    private IDocumentRepository documentRepository;
    @Mock
    private IAdminUserRepository adminUserRepository;
    @Mock
    private IDocumentService documentService;
    @Mock
    private IDealsRepository dealsRepository;
    @Mock
    private IS3Service s3Service;
    @Mock
    private JwtUserExtractor jwtUserExtractor;
    @Mock
    private org.springframework.core.env.Environment environment;
    @Mock
    private IDealEndorsementRepository dealEndorsementRepository;
    @Mock
    private IPolicyRepository policyRepository;
    @Mock
    private KeyCloakUtil keycloakUtil;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock
    private ICostSharingRuleRepository costSharingRuleRepository;

    @InjectMocks
    private OrganizationServiceImpl service;

    private OrganizationRequestDto request;
    private Organization savedOrg;

    @BeforeEach
    void setUp() {
        request = new OrganizationRequestDto();
        request.setOrganizationName("Acme Ltd");
        request.setGstin("GST123");
        request.setPanNumber("PAN123");
        request.setPrimaryContactName("Admin");
        request.setPrimaryContactEmail("admin@acme.com");
        request.setPrimaryContactPhone("9999999999");
        request.setRegisteredAddress("Bangalore");
        request.setStatus("ACTIVE");
        request.setIndustry("IT_SERVICES");

        savedOrg = new Organization();
        savedOrg.setOrganizationId(UUID.randomUUID());
        savedOrg.setOrganizationName("Acme Ltd");
    }

    @Test
    void create_shouldSeedEightDefaultCostSharingRules_whenNoExistingRules() {
        when(organizationRepository.findByOrganizationName("Acme Ltd")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrg);
        when(costSharingRuleRepository.existsByCompanyIdAndPlanTypeAndCoverageCategoryAndEffectiveFrom(
                eq(savedOrg.getOrganizationId()), any(String.class), any(CoverageCategory.class), any(LocalDate.class)))
                .thenReturn(false);

        ResponseEntity<ResponseDto<String>> response = service.create(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<CostSharingRule> captor = ArgumentCaptor.forClass(CostSharingRule.class);
        verify(costSharingRuleRepository, times(8)).save(captor.capture());
        List<CostSharingRule> inserted = captor.getAllValues();
        assertEquals(8, inserted.size());

        long hundredPercent = inserted.stream()
                .filter(r -> BigDecimal.valueOf(100).compareTo(r.getEmployerShareValue()) == 0)
                .count();
        long zeroPercent = inserted.stream()
                .filter(r -> BigDecimal.ZERO.compareTo(r.getEmployerShareValue()) == 0)
                .count();
        assertEquals(6, hundredPercent);
        assertEquals(2, zeroPercent);
        verify(keycloakUtil, times(1)).createGroup(any(String.class), any(Map.class));
    }

    @Test
    void create_shouldNotInsertDuplicateRules_whenDefaultsAlreadyExist() {
        when(organizationRepository.findByOrganizationName("Acme Ltd")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrg);
        when(costSharingRuleRepository.existsByCompanyIdAndPlanTypeAndCoverageCategoryAndEffectiveFrom(
                eq(savedOrg.getOrganizationId()), any(String.class), any(CoverageCategory.class), any(LocalDate.class)))
                .thenReturn(true);

        ResponseEntity<ResponseDto<String>> response = service.create(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(costSharingRuleRepository, never()).save(any(CostSharingRule.class));
        verify(keycloakUtil, times(1)).createGroup(any(String.class), any(Map.class));
    }
}
