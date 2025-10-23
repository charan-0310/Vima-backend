package com.vimainsurance.vimaadmin.service;

import java.util.List;

import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.ManagerDashboardResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import org.springframework.http.ResponseEntity;

public interface IManagerService {

    ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getCustomersByManagerAndAgents(
        String username, String search, int page, int rec, String owner, String sortBy, String sortDirection);

    ResponseEntity<ResponseDto<ManagerDashboardResponseDto>> getManagerDashboard(String username, String period);
    // List<AdminUserResponseDto> getSubordinatesByManager(String username);

    // List<CustomerResponseDto> getCustomersBySubordinate(String username, int page, int rec);
}
