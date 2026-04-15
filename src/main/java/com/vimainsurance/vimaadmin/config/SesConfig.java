package com.vimainsurance.vimaadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
public class SesConfig {

    @Value("${aws.ses.region:ap-south-1}")
    private String sesRegion;

    @Value("${aws.ses.access-key:}")
    private String sesAccessKey;

    @Value("${aws.ses.secret-key:}")
    private String sesSecretKey;

    @Bean
    public SesV2Client sesV2Client() {
        if (sesAccessKey != null && !sesAccessKey.isBlank() && sesSecretKey != null && !sesSecretKey.isBlank()) {
            AwsBasicCredentials creds = AwsBasicCredentials.create(sesAccessKey, sesSecretKey);
            return SesV2Client.builder()
                    .region(Region.of(sesRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                    .build();
        }

        return SesV2Client.builder()
                .region(Region.of(sesRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
