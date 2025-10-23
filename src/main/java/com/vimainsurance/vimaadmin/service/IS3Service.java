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
     * Download a file from S3
     * 
     * @param key The S3 key (path) of the file
     * @return InputStream of the file
     */
    InputStream downloadFile(String key);

    /**
     * Delete a file from S3
     * 
     * @param key The S3 key (path) of the file to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteFile(String key);

    /**
     * Generate a pre-signed URL for downloading a file
     * 
     * @param key The S3 key (path) of the file
     * @param expirationInSeconds The expiration time in seconds
     * @return Pre-signed URL
     */
    String generatePresignedUrl(String key, long expirationInSeconds);

    /**
     * Generate a pre-signed URL for downloading a file with default expiration
     * 
     * @param key The S3 key (path) of the file
     * @return Pre-signed URL
     */
    String generatePresignedUrl(String key);

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
