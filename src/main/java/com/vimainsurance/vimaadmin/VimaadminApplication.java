package com.vimainsurance.vimaadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.vimainsurance.vimaadmin.config.CsvDealsHeaderProperties;
import com.vimainsurance.vimaadmin.config.VendorMasterDataConfig;

@SpringBootApplication
@EnableConfigurationProperties({
		VendorMasterDataConfig.class,
		CsvDealsHeaderProperties.class
})
public class VimaadminApplication {

	public static void main(String[] args) {
		SpringApplication.run(VimaadminApplication.class, args);
	}

}
