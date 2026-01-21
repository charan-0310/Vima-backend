package com.vimainsurance.vimaadmin.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.HRDashBoardResponseDto;

public interface IHRService {

    ResponseEntity<ResponseDto<HRDashBoardResponseDto>> getHrDashboard(LocalDateTime startDate, LocalDateTime endDate, String companyId);

}
