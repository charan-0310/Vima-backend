package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.IncentiveEvaluationRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveEvaluationResponseDto;
import com.vimainsurance.vimaadmin.dto.IncentivePackageDto;
import com.vimainsurance.vimaadmin.dto.IncentivePackageFullRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleSlabDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleSlabRequestDto;
import com.vimainsurance.vimaadmin.dto.PackageNameDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IIncentiveService {
    // Package operations
    ResponseEntity<ResponseDto<List<IncentivePackageDto>>> getAllPackages();
    ResponseEntity<ResponseDto<IncentivePackageDto>> getPackageWithRules(Long packageId);
    ResponseEntity<ResponseDto<IncentivePackageDto>> createFullPackage(IncentivePackageFullRequestDto requestDto);
    ResponseEntity<ResponseDto<IncentivePackageDto>> updateFullPackage(Long packageId, IncentivePackageFullRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> deletePackage(Long packageId);
    
    // Rule operations
    ResponseEntity<ResponseDto<IncentiveRuleDto>> createRule(IncentiveRuleRequestDto ruleDto);
    ResponseEntity<ResponseDto<IncentiveRuleDto>> updateRule(Long ruleId, IncentiveRuleRequestDto ruleDto);
    ResponseEntity<ResponseDto<String>> deleteRule(Long ruleId);
    
    // Slab operations
    ResponseEntity<ResponseDto<IncentiveRuleSlabDto>> createSlab(Long ruleId, IncentiveRuleSlabRequestDto slabDto);
    ResponseEntity<ResponseDto<IncentiveRuleSlabDto>> updateSlab(Long slabId, IncentiveRuleSlabRequestDto slabDto);
    ResponseEntity<ResponseDto<String>> deleteSlab(Long slabId);
    
    // Evaluation
    ResponseEntity<ResponseDto<IncentiveEvaluationResponseDto>> evaluateIncentive(IncentiveEvaluationRequestDto requestDto);
    ResponseEntity<ResponseDto<List<PackageNameDto>>> getAllPackageNames();
} 