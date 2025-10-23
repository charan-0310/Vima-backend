// package com.vimainsurance.vimaadmin.controller;

// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.transaction.annotation.Transactional;

// @SpringBootTest
// @AutoConfigureWebMvc
// @ActiveProfiles("test")
// @Transactional
// class ManagerControllerIntegrationTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @Test
//     void testGetManagerDashboard_ValidRequest() throws Exception {
//         mockMvc.perform(get("/api/v1/manager/test_manager")
//                 .param("period", "this_month"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.success").value(true))
//                 .andExpect(jsonPath("$.payload.teamMetrics").exists())
//                 .andExpect(jsonPath("$.payload.agentMetrics").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.totalLeads").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.totalQuotes").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.totalPolicies").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.totalBusinessAmount").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.leadsGrowth").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.quotesGrowth").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.policiesGrowth").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.businessGrowth").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.conversionRate").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.closingRate").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.avgPolicyValue").exists())
//                 .andExpect(jsonPath("$.payload.teamMetrics.activeAgents").exists());
//     }

//     @Test
//     void testGetManagerDashboard_InvalidPeriod() throws Exception {
//         mockMvc.perform(get("/api/v1/manager/test_manager")
//                 .param("period", "invalid_period"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.success").value(false))
//                 .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid period")));
//     }

//     @Test
//     void testGetManagerDashboard_ManagerNotFound() throws Exception {
//         mockMvc.perform(get("/api/v1/manager/nonexistent_manager")
//                 .param("period", "this_month"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.success").value(false))
//                 .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Manager not found")));
//     }

//     @Test
//     void testGetManagerDashboard_DefaultPeriod() throws Exception {
//         mockMvc.perform(get("/api/v1/manager/test_manager"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.success").value(true))
//                 .andExpect(jsonPath("$.payload.teamMetrics").exists());
//     }

//     @Test
//     void testGetManagerCustomers_ValidRequest() throws Exception {
//         mockMvc.perform(get("/api/v1/manager/test_manager/customers")
//                 .param("page", "0")
//                 .param("rec", "10")
//                 .param("search", "test")
//                 .param("sortBy", "updatedAt")
//                 .param("sortDirection", "desc"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.success").value(true))
//                 .andExpect(jsonPath("$.payload").isArray());
//     }

//     @Test
//     void testGetManagerCustomers_WithOwnerFilter() throws Exception {
//         mockMvc.perform(get("/api/v1/manager/test_manager/customers")
//                 .param("page", "0")
//                 .param("rec", "10")
//                 .param("owner", "test_agent")
//                 .param("sortBy", "premium")
//                 .param("sortDirection", "desc"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.success").value(true))
//                 .andExpect(jsonPath("$.payload").isArray());
//     }
// }
