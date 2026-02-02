package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EmailResponse;

public interface IEmailService {
    
    /**
     * Send a simple text email
     * @param emailRequest Email request containing recipient, subject, and body
     * @return EmailResponse with success status and message
     */
    EmailResponse sendSimpleEmail(EmailRequest emailRequest);
    
    /**
     * Send an HTML email
     * @param emailRequest Email request containing recipient, subject, and HTML body
     * @return EmailResponse with success status and message
     */
    EmailResponse sendHtmlEmail(EmailRequest emailRequest);
    
    /**
     * Send an email using a Thymeleaf template
     * @param emailRequest Email request containing recipient, subject, template name, and variables
     * @return EmailResponse with success status and message
     */
    EmailResponse sendTemplateEmail(EmailRequest emailRequest);
    
    /**
     * Send an email with attachments
     * @param emailRequest Email request containing recipient, subject, body, and attachments
     * @return EmailResponse with success status and message
     */
    EmailResponse sendEmailWithAttachments(EmailRequest emailRequest);
    
    /**
     * Send a bulk email to multiple recipients
     * @param emailRequest Email request containing multiple recipients
     * @return EmailResponse with success status and message
     */
    EmailResponse sendBulkEmail(EmailRequest emailRequest);
    
    /**
     * Send a welcome email to new users
     * @param recipientEmail Recipient email address
     * @param userFullName User's full name (for "Welcome, X" greeting)
     * @param username Username for login (typically email)
     * @param password Temporary password
     * @return EmailResponse with success status and message
     */
    EmailResponse sendWelcomeEmail(String recipientEmail, String userFullName, String username, String password);
    
    /**
     * Send a password reset email with new password
     * @param recipientEmail Recipient email address
     * @param newPassword New password to send to user
     * @param userName User's name
     * @return EmailResponse with success status and message
     */
    EmailResponse sendPasswordResetEmail(String recipientEmail, String newPassword, String userName);
    
    /**
     * Send a quote notification email
     * @param recipientEmail Recipient email address
     * @param quoteId Quote ID
     * @param customerName Customer name
     * @return EmailResponse with success status and message
     */
    EmailResponse sendQuoteNotificationEmail(String recipientEmail, String quoteId, String customerName);
}
