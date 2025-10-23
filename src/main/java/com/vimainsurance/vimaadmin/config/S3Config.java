package com.vimainsurance.vimaadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS S3 Configuration
 * 
 * Configures AWS S3 client and presigner for document storage operations.
 * Properties are loaded from application.properties/application-{profile}.properties
 */
@Configuration
public class S3Config {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    @Value("${aws.s3.presigned-url-expiration:3600}")
    private long presignedUrlExpiration;

    /**
     * Creates and configures AWS S3 Client
     * 
     * @return Configured S3Client instance
     */
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        software.amazon.awssdk.services.s3.S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials));
        
        // If custom endpoint is provided (for LocalStack, MinIO, etc.)
        if (endpoint != null && !endpoint.isEmpty()) {
            s3ClientBuilder.endpointOverride(URI.create(endpoint));
        }
        
        return s3ClientBuilder.build();
    }

    /**
     * Creates and configures S3 Presigner for generating pre-signed URLs
     * 
     * @return Configured S3Presigner instance
     */
    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials));
        
        // If custom endpoint is provided
        if (endpoint != null && !endpoint.isEmpty()) {
            presignerBuilder.endpointOverride(URI.create(endpoint));
        }
        
        return presignerBuilder.build();
    }

    /**
     * Get the configured S3 bucket name
     * 
     * @return S3 bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Get the configured presigned URL expiration time in seconds
     * 
     * @return Presigned URL expiration time
     */
    public long getPresignedUrlExpiration() {
        return presignedUrlExpiration;
    }

    /**
     * Get the configured AWS region
     * 
     * @return AWS region
     */
    public String getRegion() {
        return region;
    }
}