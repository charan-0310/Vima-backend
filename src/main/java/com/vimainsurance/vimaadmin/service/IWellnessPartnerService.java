package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessOrgAssignmentResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerOrgRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerOrgResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerReorderRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerResponseDto;

public interface IWellnessPartnerService {

    ResponseEntity<ResponseDto<List<WellnessPartnerResponseDto>>> getAllPartners();

    ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> getPartnerById(UUID id);

    ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> createPartner(WellnessPartnerRequestDto requestDto);

    ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> updatePartner(UUID id, WellnessPartnerRequestDto requestDto);

    ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> activatePartner(UUID id);

    ResponseEntity<ResponseDto<WellnessPartnerResponseDto>> deactivatePartner(UUID id);

    ResponseEntity<ResponseDto<String>> deletePartner(UUID id);

    ResponseEntity<ResponseDto<WellnessOrgAssignmentResponseDto>> getOrgAssignment(UUID orgId);

    ResponseEntity<ResponseDto<WellnessPartnerOrgResponseDto>> assignPartnerToOrg(WellnessPartnerOrgRequestDto requestDto);

    ResponseEntity<ResponseDto<WellnessPartnerOrgResponseDto>> updateOrgMapping(UUID id, WellnessPartnerOrgRequestDto requestDto);

    ResponseEntity<ResponseDto<List<WellnessPartnerOrgResponseDto>>> reorderPartnersForOrg(WellnessPartnerReorderRequestDto requestDto);

    ResponseEntity<ResponseDto<String>> removePartnerFromOrg(UUID id);
}
