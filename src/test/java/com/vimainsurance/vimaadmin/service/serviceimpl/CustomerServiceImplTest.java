package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.vimainsurance.vimaadmin.dto.CustomerBulkDeleteRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private ICustomerRepository customerRepository;

    @Mock
    private IAdminUserRepository adminUserRepository;

    @Mock
    private IDocumentRepository documentRepository;

    @Mock
    private IDealsRepository dealsRepository;

    @Mock
    private JwtUserExtractor jwtUserExtractor;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CustomerRequestDto customerRequestDto;
    private Customer customer;
    private AdminUser adminUser;

    @BeforeEach
    void setUp() {
        // Setup mock authentication
        UserDetails userDetails = User.builder()
                .username("test-agent")
                .password("password")
                .authorities("SALES_AGENT")
                .build();
        
        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        
        // Setup admin user
        adminUser = new AdminUser();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("test-agent");
        adminUser.setRole("SALES_AGENT");

        // Setup customer request DTO
        customerRequestDto = new CustomerRequestDto();
        customerRequestDto.setCustId("C001");
        customerRequestDto.setFullName("Test Customer");
        customerRequestDto.setDateOfBirth(LocalDate.now());
        customerRequestDto.setGender("MALE");
        customerRequestDto.setPhoneNumber("1234567890");
        customerRequestDto.setEmail("test@example.com");
        customerRequestDto.setCity("Test City");
        customerRequestDto.setState("Test State");
        customerRequestDto.setOccupation("Test Occupation");
        customerRequestDto.setAnnualIncome(new BigDecimal("50000.00"));
        customerRequestDto.setDependentCount(2);
        customerRequestDto.setStatus("ACTIVE");
        customerRequestDto.setCreatedAt(LocalDateTime.now());
        customerRequestDto.setUpdatedAt(LocalDateTime.now());

        // Setup customer entity
        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustId("C001");
        customer.setFullName("Test Customer");
        customer.setDateOfBirth(LocalDate.now());
        customer.setGender("MALE");
        customer.setPhoneNumber("1234567890");
        customer.setEmail("test@example.com");
        customer.setCity("Test City");
        customer.setState("Test State");
        customer.setOccupation("Test Occupation");
        customer.setAnnualIncome(new BigDecimal("50000.00"));
        customer.setDependentCount(2);
        customer.setStatus("ACTIVE");
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setCreatedBy(adminUser);
        customer.setOwner(adminUser);
    }

    @Test
    void testUpdate_Success() {
        when(customerRepository.findByCustId(any())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        ResponseEntity<ResponseDto<String>> response = customerService.update(customerRequestDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(Constants.UPDATE_SUCCESS, response.getBody().getPayload());
    }

    @Test
    void testUpdate_CustomerNotFound() {
        when(customerRepository.findByCustId(any())).thenReturn(Optional.empty());

        ResponseEntity<ResponseDto<String>> response = customerService.update(customerRequestDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.UPDATE_FAILED, response.getBody().getMessage());
    }

    @Test
    void testDelete_Success() {
        when(customerRepository.findByCustId(any())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        ResponseEntity<ResponseDto<String>> response = customerService.delete(customerRequestDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(Constants.DELETE_MESSAGE, response.getBody().getPayload());
    }

    @Test
    void testDelete_CustomerNotFound() {
        when(customerRepository.findByCustId(any())).thenReturn(Optional.empty());

        ResponseEntity<ResponseDto<String>> response = customerService.delete(customerRequestDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.DELETE_FAILED, response.getBody().getMessage());
    }

    @Test
    void testGetAllCustomers_WithPagination() {
        List<Customer> customers = new ArrayList<>();
        customers.add(customer);
        Page<Customer> customerPage = new PageImpl<>(customers);
        when(customerRepository.findAll(any(Pageable.class))).thenReturn(customerPage);

        ResponseEntity<ResponseDto<List<CustomerResponseDto>>> response = customerService.getAllCustomers("", 0, 10, null, "asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(1, response.getBody().getPayload().size());
        assertEquals("C001", response.getBody().getPayload().get(0).getCustId());
    }

    @Test
    void testGetAllCustomers_WithoutPagination() {
        List<Customer> customers = new ArrayList<>();
        customers.add(customer);
        // When page=-1 and rec=-1, the method calls findAll(Pageable) and expects a Page<Customer>
        Page<Customer> customerPage = new PageImpl<>(customers);
        when(customerRepository.findAll(any(Pageable.class))).thenReturn(customerPage);

        ResponseEntity<ResponseDto<List<CustomerResponseDto>>> response = customerService.getAllCustomers("", -1, -1, null, "asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(1, response.getBody().getPayload().size());
        assertEquals("C001", response.getBody().getPayload().get(0).getCustId());
    }

    @Test
    void testGetAllCustomers_WithSearch() {
        List<Customer> customers = new ArrayList<>();
        customers.add(customer);
        Page<Customer> customerPage = new PageImpl<>(customers);
        when(customerRepository.searchAllCustomers(any(String.class), any(Pageable.class))).thenReturn(customerPage);

        ResponseEntity<ResponseDto<List<CustomerResponseDto>>> response = customerService.getAllCustomers("John", 0, 10, null, "asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(1, response.getBody().getPayload().size());
        assertEquals("C001", response.getBody().getPayload().get(0).getCustId());
    }

    @Test
    void testGetAllCustomers_WithSorting() {
        List<Customer> customers = new ArrayList<>();
        customers.add(customer);
        Page<Customer> customerPage = new PageImpl<>(customers);
        when(customerRepository.findAll(any(Pageable.class))).thenReturn(customerPage);

        ResponseEntity<ResponseDto<List<CustomerResponseDto>>> response = customerService.getAllCustomers("", 0, 10, "fullName", "desc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(1, response.getBody().getPayload().size());
        assertEquals("C001", response.getBody().getPayload().get(0).getCustId());
    }

    @Test
    void testGetByCustId_Success() {
        when(customerRepository.findByCustId(any())).thenReturn(Optional.of(customer));
        
        // Mock resolved admin user
        AdminUser adminUser = new AdminUser();
        adminUser.setUsername("test-agent");
        adminUser.setRole("SALES_AGENT");
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(adminUser));
        
        // Mock JwtUserExtractor
        when(jwtUserExtractor.extractCurrentUsername()).thenReturn("test-agent");

        ResponseEntity<ResponseDto<CustomerResponseDto>> response = customerService.getByCustId("C001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals("C001", response.getBody().getPayload().getCustId());
    }

    @Test
    void testGetByCustId_NotFound() {
        when(customerRepository.findByCustId(any())).thenReturn(Optional.empty());
        
        // Mock JwtUserExtractor
        when(jwtUserExtractor.extractCurrentUsername()).thenReturn("test-agent");

        ResponseEntity<ResponseDto<CustomerResponseDto>> response = customerService.getByCustId("C001");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.RECORD_NOT_FOUND_MESSAGE, response.getBody().getMessage());
    }

    // Test cases for update method - Exception handling
    @Test
    void testUpdate_Exception() {
        when(customerRepository.findByCustId(any())).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<ResponseDto<String>> response = customerService.update(customerRequestDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Database error", response.getBody().getMessage());
    }

    // Test cases for delete method - Exception handling
    @Test
    void testDelete_Exception() {
        when(customerRepository.findByCustId(any())).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<ResponseDto<String>> response = customerService.delete(customerRequestDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Database error", response.getBody().getMessage());
    }

    // Test cases for getAllCustomers method - Exception handling
    @Test
    void testGetAllCustomers_Exception() {
        // When page=-1 and rec=-1, the method calls findAll(Pageable)
        when(customerRepository.findAll(any(Pageable.class))).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<ResponseDto<List<CustomerResponseDto>>> response = customerService.getAllCustomers("", -1, -1, null, "asc");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Database error", response.getBody().getMessage());
    }

    // Test cases for getByCustId method - Access denied scenario
    @Test
    void testGetByCustId_AccessDenied() {
        AdminUser differentOwner = new AdminUser();
        differentOwner.setUsername("different-owner");
        differentOwner.setId(UUID.randomUUID());
        
        Customer customerWithDifferentOwner = new Customer();
        customerWithDifferentOwner.setId(UUID.randomUUID());
        customerWithDifferentOwner.setCustId("C001");
        customerWithDifferentOwner.setOwner(differentOwner);
        
        when(customerRepository.findByCustId(any())).thenReturn(Optional.of(customerWithDifferentOwner));
        
        // Mock resolved admin user - user is not admin and doesn't own the customer
        AdminUser testAgent = new AdminUser();
        testAgent.setUsername("test-agent");
        testAgent.setRole("SALES_AGENT");
        testAgent.setId(UUID.randomUUID());
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(testAgent));
        
        // Mock JwtUserExtractor
        when(jwtUserExtractor.extractCurrentUsername()).thenReturn("test-agent");

        ResponseEntity<ResponseDto<CustomerResponseDto>> response = customerService.getByCustId("C001");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Access denied", response.getBody().getMessage());
    }

    @Test
    void testGetByCustId_AdminAccess() {
        AdminUser differentOwner = new AdminUser();
        differentOwner.setUsername("different-owner");
        differentOwner.setId(UUID.randomUUID());
        
        Customer customerWithDifferentOwner = new Customer();
        customerWithDifferentOwner.setId(UUID.randomUUID());
        customerWithDifferentOwner.setCustId("C001");
        customerWithDifferentOwner.setOwner(differentOwner);
        
        when(customerRepository.findByCustId(any())).thenReturn(Optional.of(customerWithDifferentOwner));
        
        // Mock resolved admin user - user is ADMIN and should have access
        AdminUser adminUser = new AdminUser();
        adminUser.setUsername("admin-user");
        adminUser.setRole("ADMIN");
        adminUser.setId(UUID.randomUUID());
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(adminUser));
        
        // Mock JwtUserExtractor
        when(jwtUserExtractor.extractCurrentUsername()).thenReturn("admin-user");

        ResponseEntity<ResponseDto<CustomerResponseDto>> response = customerService.getByCustId("C001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals("C001", response.getBody().getPayload().getCustId());
    }

    // Test cases for getByCustId method - Exception handling
    @Test
    void testGetByCustId_Exception() {
        // Mock JwtUserExtractor
        when(jwtUserExtractor.extractCurrentUsername()).thenReturn("test-agent");
        
        when(customerRepository.findByCustId(any())).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<ResponseDto<CustomerResponseDto>> response = customerService.getByCustId("C001");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Database error", response.getBody().getMessage());
    }

    // Test cases for bulkDelete method - Success
    @Test
    void testBulkDelete_Success() {
        CustomerBulkDeleteRequestDto bulkDeleteDto = new CustomerBulkDeleteRequestDto();
        bulkDeleteDto.setUsername("test-agent");
        bulkDeleteDto.setCustomerIds(Arrays.asList("C001", "C002"));
        
        List<Customer> customersToDelete = Arrays.asList(customer, customer);
        
        when(adminUserRepository.findByUsername(any())).thenReturn(Optional.of(adminUser));
        when(customerRepository.findAllByCustIdIn(anyList())).thenReturn(customersToDelete);
        when(customerRepository.save(any())).thenReturn(customer);

        ResponseEntity<ResponseDto<String>> response = customerService.bulkDelete(bulkDeleteDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Customers deleted successfully", response.getBody().getMessage());
    }

    // Test cases for bulkDelete method - Agent not found
    @Test
    void testBulkDelete_AgentNotFound() {
        CustomerBulkDeleteRequestDto bulkDeleteDto = new CustomerBulkDeleteRequestDto();
        bulkDeleteDto.setUsername("non-existent-agent");
        bulkDeleteDto.setCustomerIds(Arrays.asList("C001"));
        
        when(adminUserRepository.findByUsername(any())).thenReturn(Optional.empty());

        ResponseEntity<ResponseDto<String>> response = customerService.bulkDelete(bulkDeleteDto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Agent not found", response.getBody().getMessage());
    }

    // Test cases for bulkDelete method - Customer not found
    @Test
    void testBulkDelete_CustomerNotFound() {
        CustomerBulkDeleteRequestDto bulkDeleteDto = new CustomerBulkDeleteRequestDto();
        bulkDeleteDto.setUsername("test-agent");
        bulkDeleteDto.setCustomerIds(Arrays.asList("C001", "C002"));
        
        when(adminUserRepository.findByUsername(any())).thenReturn(Optional.of(adminUser));
        when(customerRepository.findAllByCustIdIn(anyList())).thenReturn(Arrays.asList(customer)); // Only one customer found

        ResponseEntity<ResponseDto<String>> response = customerService.bulkDelete(bulkDeleteDto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("One or more customers not found", response.getBody().getMessage());
    }

    // Test cases for bulkDelete method - Permission denied
    @Test
    void testBulkDelete_PermissionDenied() {
        CustomerBulkDeleteRequestDto bulkDeleteDto = new CustomerBulkDeleteRequestDto();
        bulkDeleteDto.setUsername("test-agent");
        bulkDeleteDto.setCustomerIds(Arrays.asList("C001", "C002"));
        
        AdminUser differentOwner = new AdminUser();
        differentOwner.setId(UUID.randomUUID());
        
        Customer customerWithDifferentOwner = new Customer();
        customerWithDifferentOwner.setId(UUID.randomUUID());
        customerWithDifferentOwner.setCustId("C002");
        customerWithDifferentOwner.setOwner(differentOwner);
        
        List<Customer> customersToDelete = Arrays.asList(customer, customerWithDifferentOwner);
        
        when(adminUserRepository.findByUsername(any())).thenReturn(Optional.of(adminUser));
        when(customerRepository.findAllByCustIdIn(anyList())).thenReturn(customersToDelete);

        ResponseEntity<ResponseDto<String>> response = customerService.bulkDelete(bulkDeleteDto);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("You don't have permission to delete one or more customers", response.getBody().getMessage());
    }

    // Test cases for bulkDelete method - Exception handling
    @Test
    void testBulkDelete_Exception() {
        CustomerBulkDeleteRequestDto bulkDeleteDto = new CustomerBulkDeleteRequestDto();
        bulkDeleteDto.setUsername("test-agent");
        bulkDeleteDto.setCustomerIds(Arrays.asList("C001"));
        
        when(adminUserRepository.findByUsername(any())).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<ResponseDto<String>> response = customerService.bulkDelete(bulkDeleteDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Database error", response.getBody().getMessage());
    }

    // Test for customerToDeals with dependents - simplified version
    // Note: This test requires proper mocking setup and may need to be implemented
    // based on the actual service dependencies and their behavior
} 