package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Customer;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICustomerRepository;
import com.vimainsurance.vimaadmin.service.IZohoCRMService;
import com.vimainsurance.vimaadmin.util.Constants;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private ICustomerRepository customerRepository;

    @Mock
    private IAdminUserRepository adminUserRepository;

    @Mock
    private IZohoCRMService zohoCRMService;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CustomerRequestDto customerRequestDto;
    private Customer customer;
    private AdminUser adminUser;

    @BeforeEach
    void setUp() {
        // Setup admin user
        adminUser = new AdminUser();
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
    void testCreate_Success() {
        when(customerRepository.findByPhoneNumber(any())).thenReturn(Optional.empty());
        when(adminUserRepository.findByUsername(any())).thenReturn(Optional.of(adminUser));
        when(customerRepository.save(any())).thenReturn(customer);

        ResponseEntity<ResponseDto<String>> response = customerService.create(customerRequestDto, "test-agent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(Constants.SAVE_SUCCESS, response.getBody().getPayload());
    }

    @Test
    void testCreate_PhoneNumberExists() {
        when(customerRepository.findByPhoneNumber(any())).thenReturn(Optional.of(customer));

        ResponseEntity<ResponseDto<String>> response = customerService.create(customerRequestDto, "test-agent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals("Already Existed", response.getBody().getPayload());
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
    void testFindByAgent_Success() {
        List<Customer> customers = new ArrayList<>();
        customers.add(customer);
        when(adminUserRepository.findByUsername(any())).thenReturn(Optional.of(adminUser));
        when(customerRepository.findActiveByCreatedBy(any(), any())).thenReturn(new PageImpl<>(customers));
        
        ResponseEntity<ResponseDto<List<CustomerResponseDto>>> response = customerService.findByAgent("test-agent", "test-search", 0, 10, "test-sortBy", "test-sortDirection");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(1, response.getBody().getPayload().size());
    }

    @Test
    void testGetAllCustomers_WithPagination() {
        List<Customer> customers = new ArrayList<>();
        customers.add(customer);
        Page<Customer> customerPage = new PageImpl<>(customers);
        when(customerRepository.findAll(any(Pageable.class))).thenReturn(customerPage);

        ResponseEntity<ResponseDto<List<CustomerResponseDto>>> response = customerService.getAllCustomers(0, 10);

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
        when(customerRepository.findAll()).thenReturn(customers);

        ResponseEntity<ResponseDto<List<CustomerResponseDto>>> response = customerService.getAllCustomers(-1, -1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals(1, response.getBody().getPayload().size());
        assertEquals("C001", response.getBody().getPayload().get(0).getCustId());
    }

    @Test
    void testGetByCustId_Success() {
        when(customerRepository.findByCustId(any())).thenReturn(Optional.of(customer));

        ResponseEntity<ResponseDto<CustomerResponseDto>> response = customerService.getByCustId("C001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.SUCCESS, response.getBody().getMessage());
        assertEquals("C001", response.getBody().getPayload().getCustId());
    }

    @Test
    void testGetByCustId_NotFound() {
        when(customerRepository.findByCustId(any())).thenReturn(Optional.empty());

        ResponseEntity<ResponseDto<CustomerResponseDto>> response = customerService.getByCustId("C001");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Constants.RECORD_NOT_FOUND_MESSAGE, response.getBody().getMessage());
    }
} 