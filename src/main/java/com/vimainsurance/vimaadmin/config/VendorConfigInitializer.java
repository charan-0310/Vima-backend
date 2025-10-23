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
                
               
                if (config.getVendors() != null && config.getVendors().containsKey("DIGIT")) {
                    VendorMasterDataConfig.VendorMasterData digitData = config.getVendors().get("DIGIT");
     
                    // Print all available fields in digitData
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