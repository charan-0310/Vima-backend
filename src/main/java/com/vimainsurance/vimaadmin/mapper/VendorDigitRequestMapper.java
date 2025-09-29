package com.vimainsurance.vimaadmin.mapper;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.config.VendorMasterDataConfig;
import com.vimainsurance.vimaadmin.dto.QuickQuoteRequestDto;
import com.vimainsurance.vimaadmin.dto.digit.QQGlowWrapper;
import com.vimainsurance.vimaadmin.enums.Vendor;

@Component
public class VendorDigitRequestMapper implements VendorRequestMapper {

    private static final Logger logger = LoggerFactory.getLogger(VendorDigitRequestMapper.class);

    @Autowired
    private VendorMasterDataConfig vendorMasterDataConfig;

    @Override
    public Object map(QuickQuoteRequestDto dto) {
        String vendorName = "DIGIT";
        
        // Debug logging
        logger.info("Attempting to get vendor data for: {}", vendorName);
        logger.info("Available vendors: {}", vendorMasterDataConfig.getSupportedVendors());
        
        VendorMasterDataConfig.VendorMasterData vendorData = vendorMasterDataConfig.getVendorData(vendorName);
        
        if (vendorData == null) {
            logger.warn("VendorData is null for vendor: {}. Using default values.", vendorName);
        } else {
            logger.info("VendorData found for vendor: {}", vendorName);
        }
        
        QQGlowWrapper wrapper = new QQGlowWrapper();
        QQGlowWrapper.QQGlow qqGlow = new QQGlowWrapper.QQGlow();
        
        // Map Basic Details using vendor-specific master data
        QQGlowWrapper.BasicDetails basicDetails = new QQGlowWrapper.BasicDetails();
        basicDetails.setImdCode(vendorMasterDataConfig.getVendorDefault(vendorName, "imdCode"));
        basicDetails.setSamePolicyHolder(true);
        basicDetails.setProductCode(getFirstOrDefault(vendorData != null ? vendorData.getProductCodes() : null, "10406"));
        basicDetails.setPolicyType(getFirstOrDefault(vendorData != null ? vendorData.getPolicyType() : null, "singlelife"));
        basicDetails.setTypeOfProposer(vendorMasterDataConfig.getVendorDefault(vendorName, "relationshipWithProposer"));
        basicDetails.setPremiumPaymentFrequency(vendorMasterDataConfig.mapPaymentFrequency(vendorName, dto.getPaymentFrequency()));
        basicDetails.setProductCategory(vendorMasterDataConfig.getVendorDefault(vendorName, "productCategory"));
        basicDetails.setRelationshipWithProposer(vendorMasterDataConfig.getVendorDefault(vendorName, "relationshipWithProposer"));
        
        // Map Person details using vendor-specific mappings
        List<QQGlowWrapper.Person> persons = new ArrayList<>();
        QQGlowWrapper.Person person = new QQGlowWrapper.Person();
        person.setFirstName(dto.getName());
        person.setMiddleName("");
        person.setLastName("");
        person.setGender(vendorMasterDataConfig.mapGender(vendorName, dto.getGender()));
        person.setDob(formatDateOfBirth(dto));
        person.setSmokerStatus(vendorMasterDataConfig.mapSmokerStatus(vendorName, dto.isSmoker()));
        person.setHighestEducation(dto.getHighestEducation());
        person.setMobileNumber(dto.getMobileNumber());
        person.setNRI(false);
        person.setPersonRole("lifeAssured");
        person.setIncomeRange(dto.getIncomeRange() != null ? dto.getIncomeRange() : "300000");
        person.setOccupation(vendorMasterDataConfig.mapOccupation(vendorName, dto.getOccupation()));
        person.setState(vendorMasterDataConfig.mapState(vendorName, dto.getState()));
        person.setCity(dto.getCity());
        person.setPincode(dto.getPincode());
        person.setIncomeProofFlag(dto.getIncomeProofFlag() != null ? dto.getIncomeProofFlag() : "N");
        person.setAlcoholStatus(vendorMasterDataConfig.mapAlcoholStatus(vendorName, dto.isAlcoholic()));
        person.setResidentType(vendorMasterDataConfig.getVendorDefault(vendorName, "residentType"));
        
        QQGlowWrapper.Person person2 = new QQGlowWrapper.Person();
        person2.setFirstName(dto.getName());
        person2.setMiddleName("");
        person2.setLastName("");
        person2.setGender(vendorMasterDataConfig.mapGender(vendorName, dto.getGender()));
        person2.setDob(formatDateOfBirth(dto));
        person2.setSmokerStatus(vendorMasterDataConfig.mapSmokerStatus(vendorName, dto.isSmoker()));
        person2.setHighestEducation(dto.getHighestEducation());
        person2.setMobileNumber(dto.getMobileNumber());
        person2.setNRI(false);
        person2.setPersonRole("policyHolder");
        person2.setIncomeRange(dto.getIncomeRange() != null ? dto.getIncomeRange() : "300000");
        person2.setOccupation(vendorMasterDataConfig.mapOccupation(vendorName, dto.getOccupation()));
        person2.setState(vendorMasterDataConfig.mapState(vendorName, dto.getState()));
        person2.setCity(dto.getCity());
        person2.setPincode(dto.getPincode());
        person2.setIncomeProofFlag(dto.getIncomeProofFlag() != null ? dto.getIncomeProofFlag() : "N");
        person2.setAlcoholStatus(vendorMasterDataConfig.mapAlcoholStatus(vendorName, dto.isAlcoholic()));
        person2.setResidentType(vendorMasterDataConfig.getVendorDefault(vendorName, "residentType"));
        persons.add(person);
        persons.add(person2);
        basicDetails.setPersons(persons);
        
        // Map Plan Details using vendor-specific master data
        QQGlowWrapper.PlanDetails planDetails = new QQGlowWrapper.PlanDetails();
        planDetails.setSumAssured(dto.getSumAssured().get(0).toString());
        planDetails.setPremiumPaymentTerm(dto.getCoverageEndAge());
        planDetails.setPolicyPeriod(Integer.parseInt(dto.getCoverageEndAge()));
        planDetails.setPremiumType(vendorMasterDataConfig.getVendorDefault(vendorName, "premiumType"));
        
        // Map Contract Coverages using vendor-specific master data
        List<QQGlowWrapper.ContractCoverage> contractCoverages = new ArrayList<>();
        QQGlowWrapper.ContractCoverage coverage = new QQGlowWrapper.ContractCoverage();
        coverage.setCoverType(getFirstOrDefault(vendorData != null ? vendorData.getCoverageTypes() : null, "DGTPD"));
        coverage.setCoverageName(getFirstOrDefault(vendorData != null ? vendorData.getCoverageNames() : null, "Additional Accidental Total and Permanent Disability Benefit"));
        coverage.setCoverAvailability("");
        coverage.setSelection(false);
        contractCoverages.add(coverage);

        QQGlowWrapper.ContractCoverage coverage2 = new QQGlowWrapper.ContractCoverage();
        coverage2.setCoverType(getFirstOrDefault(vendorData != null ? vendorData.getCoverageTypes() : null, "DRTLI"));
        coverage2.setCoverageName(getFirstOrDefault(vendorData != null ? vendorData.getCoverageNames() : null, "Death Benefit"));
        coverage2.setCoverAvailability("MANDATORY");
        coverage2.setSelection(true);
        contractCoverages.add(coverage2);

        QQGlowWrapper.ContractCoverage coverage3 = new QQGlowWrapper.ContractCoverage();
        coverage3.setCoverType(getFirstOrDefault(vendorData != null ? vendorData.getCoverageTypes() : null, "DGADB"));
        coverage3.setCoverageName(getFirstOrDefault(vendorData != null ? vendorData.getCoverageNames() : null, "Accidental Death Benefit"));
        coverage3.setCoverAvailability("");
        coverage3.setSelection(false);
        contractCoverages.add(coverage3);
        
        planDetails.setContractCoverages(contractCoverages);
        
        qqGlow.setBasicDetails(basicDetails);
        qqGlow.setPlanDetails(planDetails);
        wrapper.setQqGlow(qqGlow);
        
        return wrapper;
    }

    @Override
    public Vendor getVendor() {
        return Vendor.DIGIT;
    }
    
    private String formatDateOfBirth(QuickQuoteRequestDto dto) {
        if (dto.getDateOfBirth() != null && !dto.getDateOfBirth().isEmpty()) {
            return dto.getDateOfBirth();
        }
        
        // Construct from individual fields if dateOfBirth is not provided
        if (dto.getDobDay() != null && !dto.getDobDay().isEmpty() && 
            dto.getDobMonth() != null && !dto.getDobMonth().isEmpty() && 
            dto.getDobYear() != null && !dto.getDobYear().isEmpty()) {
            String day = dto.getDobDay().length() == 1 ? "0" + dto.getDobDay() : dto.getDobDay();
            String month = dto.getDobMonth().length() == 1 ? "0" + dto.getDobMonth() : dto.getDobMonth();
            return dto.getDobYear() + "-" + month + "-" + day;
        }
        
        return "1990-01-01"; // Default date
    }
    
    private String getFirstOrDefault(List<String> list, String defaultValue) {
        return list != null && !list.isEmpty() ? list.get(0) : defaultValue;
    }
} 