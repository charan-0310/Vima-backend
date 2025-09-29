package com.vimainsurance.vimaadmin.config;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "master-data")
public class VendorMasterDataConfig {
    
    private CommonMasterData common;
    private Map<String, VendorMasterData> vendors;
    
    @Data
    public static class CommonMasterData {
        private List<String> gender;
        private List<String> smokerStatus;
        private List<String> alcoholStatus;
        private List<String> occupation;
        private List<String> incomeRange;
    }
    
    @Data
    public static class VendorMasterData {
        private List<String> premiumPaymentFrequency;
        private Map<String, String> paymentFrequencyMapping;
        private Map<String, String> genderMapping;
        private Map<String, String> booleanMapping;
        private List<String> smokerStatus;
        private Map<String, String> smokerStatusMapping;
        private List<String> alcoholStatus;
        private Map<String, String> alcoholStatusMapping;
        private Map<String, String> occupationMapping;
        private Map<String, String> stateMappingConfig;
        private List<String> typeOfProposer;
        private List<String> policyType;
        private List<String> productCodes;
        private List<String> coverageTypes;
        private List<String> coverageNames;
        private VendorDefaults defaults;
    }
    
    @Data
    public static class VendorDefaults {
        private String imdCode;
        private String agentCode;
        private String branchCode;
        private String productCategory;
        private String relationshipWithProposer;
        private String highestEducation;
        private String personRole;
        private String residentType;
        private String premiumType;
    }
    
    // Helper methods
    public VendorMasterData getVendorData(String vendorName) {
        if (vendors == null || vendorName == null) {
            return null;
        }
        return vendors.get(vendorName.toUpperCase());
    }
    
    public CommonMasterData getCommonData() {
        return common;
    }
    
    // Vendor-specific helper methods
    public String mapPaymentFrequency(String vendorName, String frequency) {
        if (frequency == null) {
            return "";
        }
        VendorMasterData vendorData = getVendorData(vendorName);
        if (vendorData == null || vendorData.getPaymentFrequencyMapping() == null) {
            return frequency;
        }
        return vendorData.getPaymentFrequencyMapping()
                .getOrDefault(frequency.toLowerCase(), frequency);
    }
    
    public String mapGender(String vendorName, String gender) {
        VendorMasterData vendorData = getVendorData(vendorName);
        if (vendorData == null || vendorData.getGenderMapping() == null) {
            return gender;
        }
        return vendorData.getGenderMapping()
                .getOrDefault(gender.toLowerCase(), gender);
    }
    
    public String mapBooleanValue(String vendorName, boolean value) {
        VendorMasterData vendorData = getVendorData(vendorName);
        if (vendorData == null || vendorData.getBooleanMapping() == null) {
            return String.valueOf(value);
        }
        return vendorData.getBooleanMapping()
                .getOrDefault(String.valueOf(value), String.valueOf(value));
    }
    
    public String mapSmokerStatus(String vendorName, boolean isSmoker) {
        VendorMasterData vendorData = getVendorData(vendorName);
        if (vendorData == null || vendorData.getSmokerStatusMapping() == null) {
            return isSmoker ? "Yes" : "No";
        }
        return vendorData.getSmokerStatusMapping()
                .getOrDefault(String.valueOf(isSmoker), isSmoker ? "Yes" : "No");
    }
    
    public String mapAlcoholStatus(String vendorName, boolean isAlcoholic) {
        VendorMasterData vendorData = getVendorData(vendorName);
        if (vendorData == null || vendorData.getAlcoholStatusMapping() == null) {
            return isAlcoholic ? "Yes" : "No";
        }
        return vendorData.getAlcoholStatusMapping()
                .getOrDefault(String.valueOf(isAlcoholic), isAlcoholic ? "Yes" : "No");
    }
    

    
    // Debug method to check state mapping
    public void debugStateMapping(String vendorName) {
        VendorMasterData vendorData = getVendorData(vendorName);
        if (vendorData != null && vendorData.getStateMappingConfig() != null) {
            System.out.println("DEBUG: State mapping for vendor " + vendorName + ": " + vendorData.getStateMappingConfig());
        } else {
            System.out.println("DEBUG: No state mapping found for vendor " + vendorName);
        }
    }
    
    public String getVendorDefault(String vendorName, String fieldName) {
        VendorMasterData vendorData = getVendorData(vendorName);
        if (vendorData == null || vendorData.getDefaults() == null) {
            return null;
        }
        
        VendorDefaults defaults = vendorData.getDefaults();
        switch (fieldName.toLowerCase()) {
            case "imdcode": return defaults.getImdCode();
            case "agentcode": return defaults.getAgentCode();
            case "branchcode": return defaults.getBranchCode();
            case "productcategory": return defaults.getProductCategory();
            case "relationshipwithproposer": return defaults.getRelationshipWithProposer();
            case "highesteducation": return defaults.getHighestEducation();
            case "personrole": return defaults.getPersonRole();
            case "residenttype": return defaults.getResidentType();
            case "premiumtype": return defaults.getPremiumType();
            default: return null;
        }
    }
    
    public boolean isVendorSupported(String vendorName) {
        return vendors != null && vendors.containsKey(vendorName.toUpperCase());
    }
    
    public List<String> getSupportedVendors() {
        return vendors != null ? List.copyOf(vendors.keySet()) : List.of();
    }
    
    public String mapOccupation(String vendorName, String occupation) {
        if (occupation == null) {
            return "";
        }
        VendorMasterData vendorData = getVendorData(vendorName);
        if (vendorData == null || vendorData.getOccupationMapping() == null) {
            return occupation;
        }
        return vendorData.getOccupationMapping()
                .getOrDefault(occupation, occupation);
    }
    
    public String mapState(String vendorName, String state) {
        if (state == null) {
            return "XX";
        }
        VendorMasterData vendorData = getVendorData(vendorName);
        if (vendorData == null || vendorData.getStateMappingConfig() == null) {
            return state;
        }
        System.out.println(vendorData.getStateMappingConfig().get(state));
        return vendorData.getStateMappingConfig()
                .getOrDefault(state, "XX");
    }
} 