package com.vimainsurance.vimaadmin.service.serviceimpl;

import com.vimainsurance.vimaadmin.config.S3Config;
import com.vimainsurance.vimaadmin.service.IS3Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.io.File;
import java.io.FileInputStream;

import org.springframework.beans.factory.annotation.Value;

/**
 * Service implementation for AWS S3 operations
 * 
 * Handles file upload, download, deletion, and URL generation for S3
 */
@Service
public class S3ServiceImpl implements IS3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3ServiceImpl.class);
    private static final String CORRELATION_ID = "correlationId";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;

    @Value("${aws.s3.server-side-encryption.kms-key-id}")
    private String serverSideEncryption;

    @Autowired
    public S3ServiceImpl(S3Client s3Client, S3Presigner s3Presigner, S3Config s3Config) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.s3Config = s3Config;
    }

    @Override
    public String uploadFile(MultipartFile file, String key) {
        logger.info("[correlationId:{}] Uploading file to S3: {}", MDC.get(CORRELATION_ID), key);
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .ssekmsKeyId(serverSideEncryption)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                s3Config.getBucketName(), s3Config.getRegion(), key);
            
            logger.info("[correlationId:{}] File uploaded successfully to S3: {}", MDC.get(CORRELATION_ID), key);
            return s3Url;
            
        } catch (IOException e) {
            logger.error("[correlationId:{}] Error uploading file to S3: {}", MDC.get(CORRELATION_ID), key, e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        } catch (S3Exception e) {
            logger.error("[correlationId:{}] S3 error uploading file: {}", MDC.get(CORRELATION_ID), key, e);
            throw new RuntimeException("S3 error: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
    @Override
    public String uploadFile(File file, String key, String contentType) {
        logger.info("[correlationId:{}] Uploading file to S3: {}", MDC.get(CORRELATION_ID), key);
        
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .ssekmsKeyId(serverSideEncryption)
                    .contentType(contentType)
                    .contentLength(file.length())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(fileInputStream, file.length()));
            
            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                s3Config.getBucketName(), s3Config.getRegion(), key);
            
            logger.info("[correlationId:{}] File uploaded successfully to S3: {}", MDC.get(CORRELATION_ID), key);
            return s3Url;
            
        } catch (IOException e) {
            logger.error("[correlationId:{}] Error uploading file to S3: {}", MDC.get(CORRELATION_ID), key, e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        } catch (S3Exception e) {
            logger.error("[correlationId:{}] S3 error uploading file: {}", MDC.get(CORRELATION_ID), key, e);
            throw new RuntimeException("S3 error: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public String uploadFile(InputStream inputStream, String key, String contentType, long contentLength) {
        logger.info("[correlationId:{}] Uploading file to S3 from InputStream: {}", MDC.get(CORRELATION_ID), key);
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
            
            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                s3Config.getBucketName(), s3Config.getRegion(), key);
            
            logger.info("[correlationId:{}] File uploaded successfully to S3: {}", MDC.get(CORRELATION_ID), key);
            return s3Url;
            
        } catch (S3Exception e) {
            logger.error("[correlationId:{}] S3 error uploading file: {}", MDC.get(CORRELATION_ID), key, e);
            throw new RuntimeException("S3 error: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String key) {
        return downloadFile(s3Config.getBucketName(), key);
    }

    @Override
    public InputStream downloadFile(String bucket, String key) {
        String b = (bucket != null && !bucket.isBlank()) ? bucket.trim() : s3Config.getBucketName();
        logger.info("[correlationId:{}] Downloading file from S3: s3://{}/{}", MDC.get(CORRELATION_ID), b, key);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(b)
                    .key(key)
                    .build();

            return s3Client.getObject(getObjectRequest);

        } catch (S3Exception e) {
            logger.error("[correlationId:{}] S3 error downloading file: {}", MDC.get(CORRELATION_ID), key, e);
            throw new RuntimeException("S3 error: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String key) {
        logger.info("[correlationId:{}] Deleting file from S3: {}", MDC.get(CORRELATION_ID), key);
        
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            
            logger.info("[correlationId:{}] File deleted successfully from S3: {}", MDC.get(CORRELATION_ID), key);
            return true;
            
        } catch (S3Exception e) {
            logger.error("[correlationId:{}] S3 error deleting file: {}", MDC.get(CORRELATION_ID), key, e);
            return false;
        }
    }

    @Override
    public String generatePresignedUrl(String key, long expirationInSeconds) {
        return generatePresignedUrl(s3Config.getBucketName(), key, expirationInSeconds);
    }

    @Override
    public String generatePresignedUrl(String key) {
        return generatePresignedUrl(s3Config.getBucketName(), key, s3Config.getPresignedUrlExpiration());
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, long expirationInSeconds) {
        String b = (bucket != null && !bucket.isBlank()) ? bucket.trim() : s3Config.getBucketName();
        logger.info("[correlationId:{}] Generating presigned URL for s3://{}/{}", MDC.get(CORRELATION_ID), b, key);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(b)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirationInSeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            String presignedUrl = presignedRequest.url().toString();
            logger.info("[correlationId:{}] Presigned URL generated successfully for: {}", MDC.get(CORRELATION_ID), key);

            return presignedUrl;

        } catch (S3Exception e) {
            logger.error("[correlationId:{}] S3 error generating presigned URL: {}", MDC.get(CORRELATION_ID), key, e);
            throw new RuntimeException("S3 error: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public String generatePresignedUrl(String bucket, String key) {
        return generatePresignedUrl(bucket, key, s3Config.getPresignedUrlExpiration());
    }

    @Override
    public boolean fileExists(String key) {
        logger.info("[correlationId:{}] Checking if file exists in S3: {}", MDC.get(CORRELATION_ID), key);
        
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            logger.debug("[correlationId:{}] File does not exist in S3: {}", MDC.get(CORRELATION_ID), key);
            return false;
        } catch (S3Exception e) {
            logger.error("[correlationId:{}] S3 error checking file existence: {}", MDC.get(CORRELATION_ID), key, e);
            return false;
        }
    }

    @Override
    public long getFileSize(String key) {
        logger.info("[correlationId:{}] Getting file size from S3: {}", MDC.get(CORRELATION_ID), key);
        
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();

            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            return headObjectResponse.contentLength();
            
        } catch (NoSuchKeyException e) {
            logger.debug("[correlationId:{}] File does not exist in S3: {}", MDC.get(CORRELATION_ID), key);
            return -1;
        } catch (S3Exception e) {
            logger.error("[correlationId:{}] S3 error getting file size: {}", MDC.get(CORRELATION_ID), key, e);
            return -1;
        }
    }

    @Override
    public boolean copyFile(String sourceKey, String destinationKey) {
        logger.info("[correlationId:{}] Copying file in S3 from {} to {}", 
                   MDC.get(CORRELATION_ID), sourceKey, destinationKey);
        
        try {
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(s3Config.getBucketName())
                    .sourceKey(sourceKey)
                    .destinationBucket(s3Config.getBucketName())
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(copyObjectRequest);
            
            logger.info("[correlationId:{}] File copied successfully in S3", MDC.get(CORRELATION_ID));
            return true;
            
        } catch (S3Exception e) {
            logger.error("[correlationId:{}] S3 error copying file: {}", MDC.get(CORRELATION_ID), e);
            return false;
        }
    }


}
