package com.vimainsurance.vimaadmin.config;

import java.io.IOException;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class VendorConfigInitializer {

    @Bean
    @Primary
    public VendorMasterDataConfig vendorMasterDataConfig() {
        try {
            // Load YAML file directly
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new ClassPathResource("vendor-master-data.yml"));
            
            if (yaml.getObject() != null) {
                // Bind YAML properties to VendorMasterDataConfig
                ConfigurationPropertySource propertySource = new MapConfigurationPropertySource(yaml.getObject());
                Binder binder = new Binder(propertySource);
                
                VendorMasterDataConfig config = binder.bind("master-data", VendorMasterDataConfig.class).get();
                
                System.out.println("✅ DIGIT vendor configuration loaded from YAML");
                System.out.println("Available vendors: " + (config.getVendors() != null ? config.getVendors().keySet() : "NONE"));
                
                if (config.getVendors() != null && config.getVendors().containsKey("DIGIT")) {
                    System.out.println("✅ DIGIT vendor found in YAML configuration");
                    VendorMasterDataConfig.VendorMasterData digitData = config.getVendors().get("DIGIT");
                    System.out.println("DIGIT smoker mapping: " + (digitData.getSmokerStatusMapping() != null));
                    System.out.println("DIGIT alcohol mapping: " + (digitData.getAlcoholStatusMapping() != null));
                    System.out.println("DIGIT state mapping: " + (digitData.getStateMappingConfig() != null));
                    if (digitData.getStateMappingConfig() != null) {
                        System.out.println("DIGIT state mapping size: " + digitData.getStateMappingConfig().size());
                        System.out.println("DIGIT state mapping sample: " + digitData.getStateMappingConfig().get("Tamil Nadu"));
                        System.out.println("DIGIT state mapping keys: " + digitData.getStateMappingConfig().keySet());
                    } else {
                        System.out.println("❌ DIGIT state mapping is NULL");
                    }
                    
                    // Print all available fields in digitData
                    System.out.println("DEBUG: All fields in DIGIT vendor data:");
                    System.out.println("  - premiumPaymentFrequency: " + (digitData.getPremiumPaymentFrequency() != null ? digitData.getPremiumPaymentFrequency().size() : "NULL"));
                    System.out.println("  - paymentFrequencyMapping: " + (digitData.getPaymentFrequencyMapping() != null ? digitData.getPaymentFrequencyMapping().size() : "NULL"));
                    System.out.println("  - genderMapping: " + (digitData.getGenderMapping() != null ? digitData.getGenderMapping().size() : "NULL"));
                    System.out.println("  - smokerStatusMapping: " + (digitData.getSmokerStatusMapping() != null ? digitData.getSmokerStatusMapping().size() : "NULL"));
                    System.out.println("  - alcoholStatusMapping: " + (digitData.getAlcoholStatusMapping() != null ? digitData.getAlcoholStatusMapping().size() : "NULL"));
                    System.out.println("  - stateMappingConfig: " + (digitData.getStateMappingConfig() != null ? digitData.getStateMappingConfig().size() : "NULL"));
                } else {
                    System.err.println("❌ DIGIT vendor not found in YAML");
                }
                
                return config;
            } else {
                System.err.println("❌ Failed to load vendor-master-data.yml");
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading YAML: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fallback to empty config if YAML loading fails
        VendorMasterDataConfig fallbackConfig = new VendorMasterDataConfig();
        System.out.println("⚠️ Using fallback configuration");
        return fallbackConfig;
    }
} 