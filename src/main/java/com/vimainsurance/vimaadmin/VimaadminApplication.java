package com.vimainsurance.vimaadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.vimainsurance.vimaadmin.config.AuditReplayConfig;
import com.vimainsurance.vimaadmin.config.CsvDealsHeaderProperties;
import com.vimainsurance.vimaadmin.config.EndorsementSchedulerConfig;
import com.vimainsurance.vimaadmin.config.MantraCareProperties;
import com.vimainsurance.vimaadmin.config.VendorMasterDataConfig;
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({
		VendorMasterDataConfig.class,
		CsvDealsHeaderProperties.class,
		EndorsementSchedulerConfig.class,
		AuditReplayConfig.class,
		NotificationsProperties.class,
		MantraCareProperties.class
})
public class VimaadminApplication {

	public static void main(String[] args) {
		SpringApplication.run(VimaadminApplication.class, args);
	}

}
