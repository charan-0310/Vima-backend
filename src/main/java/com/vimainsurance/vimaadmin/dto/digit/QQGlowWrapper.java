package com.vimainsurance.vimaadmin.dto.digit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class QQGlowWrapper {

    @JsonProperty("QQ_Glow")
    private QQGlow qqGlow;

    // @JsonProperty("QQ_Glow")
    // public QQGlow getQqGlow() {
    //     return qqGlow;
    // }

    // @JsonProperty("QQ_Glow")
    // public void setQqGlow(QQGlow qqGlow) {
    //     this.qqGlow = qqGlow;
    // }

    @Data
    public static class QQGlow {
        private BasicDetails basicDetails;
        private PlanDetails planDetails;
    }

    @Data
    public static class BasicDetails {
        private String imdCode;
        @JsonProperty("isSamePolicyHolder")
        private boolean isSamePolicyHolder;
        private String productCode;
        private String policyType;
        private String typeOfProposer;
        private String premiumPaymentFrequency;
        private List<Person> persons;
        private String productCategory;
        private String relationshipWithProposer;
    }

    @Data
    public static class Person {
        private String firstName;
        private String middleName;
        private String lastName;
        private String gender;
        private String dob;
        private String smokerStatus;
        private String highestEducation;
        private String mobileNumber;
        private boolean isNRI;
        private String personRole;
        private String incomeRange;
        private String occupation;
        private String state;
        private String city;
        private String pincode;
        private String incomeProofFlag;
        private String alcoholStatus;
        private String residentType;
    }

    @Data
    public static class PlanDetails {
        private String sumAssured;
        private List<ContractCoverage> contractCoverages;
        private String premiumPaymentTerm;
        private int policyPeriod;
        private String premiumType;
    }

    @Data
    public static class ContractCoverage {
        private String coverType;
        private String coverageName;
        private String coverAvailability;
        private boolean selection;
    }
}
