package com.vimainsurance.vimaadmin.controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.vimainsurance.vimaadmin.dto.HRDashBoardResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IHRService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@RestController
@RequestMapping("/api/v1/hr")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
public class HRController {

    @Autowired
    private IHRService hrService;

    @GetMapping("/dashboard")
    public ResponseEntity<ResponseDto<HRDashBoardResponseDto>> getHrDashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // Parse date strings to LocalDateTime, default to null (which means "all")
        LocalDateTime start = null;
        LocalDateTime end = null;
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                start = LocalDateTime.parse(startDate, formatter);
            } catch (Exception e) {
                // If parsing fails, try ISO_DATE format (YYYY-MM-DD) and set to start of day
                try {
                    start = java.time.LocalDate.parse(startDate).atStartOfDay();
                } catch (Exception ex) {
                    // If both fail, ignore and use null (all dates)
                }
            }
        }
        
        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                end = LocalDateTime.parse(endDate, formatter);
            } catch (Exception e) {
                // If parsing fails, try ISO_DATE format and set to end of day
                try {
                    end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
                } catch (Exception ex) {
                    // If both fail, ignore and use null (all dates)
                }
            }
        }
        
        return hrService.getHrDashboard(start, end);
    }

}
