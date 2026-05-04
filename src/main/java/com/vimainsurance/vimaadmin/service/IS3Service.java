package com.vimainsurance.vimaadmin.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.File;

/**
 * Service interface for AWS S3 operations
 * 
 * Provides methods for file upload, download, deletion, and URL generation
 */
public interface IS3Service {

    /**
     * Upload a file to S3
     * 
     * @param file The file to upload
     * @param key The S3 key (path) for the file
     * @return The S3 URL of the uploaded file
     */
    String uploadFile(MultipartFile file, String key);

    /**
     * Upload a file to S3
     * 
     * @param file The file to upload
     * @param key The S3 key (path) for the file
     * @return The S3 URL of the uploaded file
     */
    String uploadFile(File file, String key, String contentType);
    /**
     * Upload a file to S3 from InputStream
     * 
     * @param inputStream The input stream of the file
     * @param key The S3 key (path) for the file
     * @param contentType The content type of the file
     * @param contentLength The content length of the file
     * @return The S3 URL of the uploaded file
     */
    String uploadFile(InputStream inputStream, String key, String contentType, long contentLength);

    /**
     * Download a file from S3 (default configured bucket).
     */
    InputStream downloadFile(String key);

    /**
     * Download from an explicit bucket (e.g. {@code document.s3_bucket}).
     */
    InputStream downloadFile(String bucket, String key);

    /**
     * Delete a file from S3
     * 
     * @param key The S3 key (path) of the file to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteFile(String key);

    /**
     * Generate a pre-signed URL for downloading a file (configured default bucket).
     */
    String generatePresignedUrl(String key, long expirationInSeconds);

    String generatePresignedUrl(String key);

    /**
     * Pre-signed GET for an explicit bucket (use when {@code document.s3_bucket} differs from app default).
     */
    String generatePresignedUrl(String bucket, String key, long expirationInSeconds);

    String generatePresignedUrl(String bucket, String key);

    /**
     * Check if a file exists in S3
     * 
     * @param key The S3 key (path) of the file
     * @return true if file exists, false otherwise
     */
    boolean fileExists(String key);

    /**
     * Get the size of a file in S3
     * 
     * @param key The S3 key (path) of the file
     * @return File size in bytes, or -1 if file doesn't exist
     */
    long getFileSize(String key);

    /**
     * Copy a file within S3
     * 
     * @param sourceKey The source S3 key
     * @param destinationKey The destination S3 key
     * @return true if copy was successful, false otherwise
     */
    boolean copyFile(String sourceKey, String destinationKey);
}
