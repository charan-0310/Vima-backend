// package com.vimainsurance.vimaadmin.service;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Optional;
// import java.util.UUID;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.http.ResponseEntity;

// import com.vimainsurance.vimaadmin.dto.ManagerDashboardResponseDto;
// import com.vimainsurance.vimaadmin.dto.ResponseDto;
// import com.vimainsurance.vimaadmin.entity.AdminUser;
// import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
// import com.vimainsurance.vimaadmin.service.serviceimpl.ManagerServiceImpl;
// import com.vimainsurance.vimaadmin.util.PeriodFilterUtil;

// @ExtendWith(MockitoExtension.class)
// class ManagerServiceTest {

//     @Mock
//     private IAdminUserRepository adminUserRepository;

//     @InjectMocks
//     private ManagerServiceImpl managerService;

//     private AdminUser testManager;
//     private UUID managerId;

//     @BeforeEach
//     void setUp() {
//         managerId = UUID.randomUUID();
//         testManager = new AdminUser();
//         testManager.setId(managerId);
//         testManager.setUsername("test_manager");
//         testManager.setFullName("Test Manager");
//         testManager.setRole("MANAGER");
//         testManager.setIsActive(true);
//     }

//     @Test
//     void testGetManagerDashboard_Success() {
//         // Arrange
//         String period = "this_month";
//         when(adminUserRepository.findByUsername("test_manager")).thenReturn(Optional.of(testManager));
        
//         // Mock repository calls for current period
//         when(adminUserRepository.countTotalLeadsForManager(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
//             .thenReturn(100L);
//         when(adminUserRepository.countTotalQuotesForManager(any(UUID.class), any(), any()))
//             .thenReturn(80L);
//         when(adminUserRepository.countTotalPoliciesForManager(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
//             .thenReturn(40L);
//         when(adminUserRepository.getTotalBusinessAmountForManager(any(UUID.class), any(), any()))
//             .thenReturn(new BigDecimal("5000000"));
//         when(adminUserRepository.countActiveAgentsForManager(any(UUID.class)))
//             .thenReturn(5L);
        
//         // Mock repository calls for previous period
//         when(adminUserRepository.countTotalLeadsForManager(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
//             .thenReturn(90L, 100L); // First call for previous, second for current
//         when(adminUserRepository.countTotalQuotesForManager(any(UUID.class), any(), any()))
//             .thenReturn(70L, 80L);
//         when(adminUserRepository.countTotalPoliciesForManager(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
//             .thenReturn(35L, 40L);
//         when(adminUserRepository.getTotalBusinessAmountForManager(any(UUID.class), any(), any()))
//             .thenReturn(new BigDecimal("4000000"), new BigDecimal("5000000"));
        
//         // Mock agent metrics
//         Object[] agentData = {"agent1", "Agent One", 20L, 16L, 8L, new BigDecimal("1000000")};
//         when(adminUserRepository.getAgentMetricsForManager(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
//             .thenReturn(Arrays.asList(agentData));

//         // Act
//         ResponseEntity<ResponseDto<ManagerDashboardResponseDto>> response = 
//             managerService.getManagerDashboard("test_manager", period);

//         // Assert
//         assertNotNull(response);
//         assertNotNull(response.getBody());
        
//         ManagerDashboardResponseDto dashboardData = response.getBody().getPayload();
//         assertNotNull(dashboardData);
//         assertNotNull(dashboardData.getTeamMetrics());
//         assertNotNull(dashboardData.getAgentMetrics());
        
//         // Verify team metrics
//         ManagerDashboardResponseDto.TeamMetrics teamMetrics = dashboardData.getTeamMetrics();
//         assertEquals(100, teamMetrics.getTotalLeads());
//         assertEquals(80, teamMetrics.getTotalQuotes());
//         assertEquals(40, teamMetrics.getTotalPolicies());
//         assertEquals(new BigDecimal("5000000"), teamMetrics.getTotalBusinessAmount());
//         assertEquals(5, teamMetrics.getActiveAgents());
        
//         // Verify growth calculations
//         assertTrue(teamMetrics.getLeadsGrowth() > 0);
//         assertTrue(teamMetrics.getQuotesGrowth() > 0);
//         assertTrue(teamMetrics.getPoliciesGrowth() > 0);
//         assertTrue(teamMetrics.getBusinessGrowth() > 0);
        
//         // Verify conversion rates
//         assertEquals(80.0, teamMetrics.getConversionRate(), 0.1);
//         assertEquals(50.0, teamMetrics.getClosingRate(), 0.1);
        
//         // Verify agent metrics
//         assertEquals(1, dashboardData.getAgentMetrics().size());
//         ManagerDashboardResponseDto.AgentMetrics agentMetric = dashboardData.getAgentMetrics().get(0);
//         assertEquals("Agent One", agentMetric.getAgentName());
//         assertEquals(20, agentMetric.getLeads());
//         assertEquals(16, agentMetric.getQuotes());
//         assertEquals(8, agentMetric.getPolicies());
//     }

//     @Test
//     void testGetManagerDashboard_ManagerNotFound() {
//         // Arrange
//         when(adminUserRepository.findByUsername("nonexistent_manager")).thenReturn(Optional.empty());

//         // Act
//         ResponseEntity<ResponseDto<ManagerDashboardResponseDto>> response = 
//             managerService.getManagerDashboard("nonexistent_manager", "this_month");

//         // Assert
//         assertNotNull(response);
//         assertNotNull(response.getBody());
//         assertFalse(response.getBody().isSuccess());
//         assertTrue(response.getBody().getMessage().contains("Manager not found"));
//     }

//     @Test
//     void testGetManagerDashboard_InvalidPeriod() {
//         // Arrange
//         when(adminUserRepository.findByUsername("test_manager")).thenReturn(Optional.of(testManager));

//         // Act
//         ResponseEntity<ResponseDto<ManagerDashboardResponseDto>> response = 
//             managerService.getManagerDashboard("test_manager", "invalid_period");

//         // Assert
//         assertNotNull(response);
//         assertNotNull(response.getBody());
//         assertFalse(response.getBody().isSuccess());
//         assertTrue(response.getBody().getMessage().contains("Invalid period"));
//     }

//     @Test
//     void testGetManagerDashboard_ExceptionHandling() {
//         // Arrange
//         when(adminUserRepository.findByUsername("test_manager")).thenReturn(Optional.of(testManager));
//         when(adminUserRepository.countTotalLeadsForManager(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
//             .thenThrow(new RuntimeException("Database error"));

//         // Act
//         ResponseEntity<ResponseDto<ManagerDashboardResponseDto>> response = 
//             managerService.getManagerDashboard("test_manager", "this_month");

//         // Assert
//         assertNotNull(response);
//         assertNotNull(response.getBody());
//         assertFalse(response.getBody().isSuccess());
//         assertTrue(response.getBody().getMessage().contains("Error fetching manager dashboard"));
//     }

//     @Test
//     void testPeriodFilterUtil_ValidPeriods() {
//         // Test all valid periods
//         String[] validPeriods = {"this_month", "last_month", "last_3_months", "last_6_months", "this_year"};
        
//         for (String period : validPeriods) {
//             assertDoesNotThrow(() -> {
//                 PeriodFilterUtil.PeriodRange range = PeriodFilterUtil.getPeriodRange(period);
//                 assertNotNull(range);
//                 assertNotNull(range.getStartDate());
//                 assertNotNull(range.getEndDate());
//                 assertNotNull(range.getPreviousStartDate());
//                 assertNotNull(range.getPreviousEndDate());
//             });
//         }
//     }

//     @Test
//     void testPeriodFilterUtil_InvalidPeriod() {
//         // Test invalid period
//         assertThrows(IllegalArgumentException.class, () -> {
//             PeriodFilterUtil.getPeriodRange("invalid_period");
//         });
//     }

//     @Test
//     void testPeriodFilterUtil_GrowthCalculations() {
//         // Test growth calculation with zero previous value
//         double growth = PeriodFilterUtil.calculateGrowth(100.0, 0.0);
//         assertEquals(100.0, growth, 0.1);
        
//         // Test growth calculation with normal values
//         growth = PeriodFilterUtil.calculateGrowth(120.0, 100.0);
//         assertEquals(20.0, growth, 0.1);
        
//         // Test negative growth
//         growth = PeriodFilterUtil.calculateGrowth(80.0, 100.0);
//         assertEquals(-20.0, growth, 0.1);
//     }

//     @Test
//     void testPeriodFilterUtil_ConversionRates() {
//         // Test conversion rate calculation
//         double conversionRate = PeriodFilterUtil.calculateConversionRate(80, 100);
//         assertEquals(80.0, conversionRate, 0.1);
        
//         // Test with zero leads
//         conversionRate = PeriodFilterUtil.calculateConversionRate(10, 0);
//         assertEquals(0.0, conversionRate, 0.1);
        
//         // Test closing rate calculation
//         double closingRate = PeriodFilterUtil.calculateClosingRate(40, 80);
//         assertEquals(50.0, closingRate, 0.1);
        
//         // Test with zero quotes
//         closingRate = PeriodFilterUtil.calculateClosingRate(10, 0);
//         assertEquals(0.0, closingRate, 0.1);
//     }
// }
