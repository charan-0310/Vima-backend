package com.vimainsurance.vimaadmin.service.serviceimpl;

import com.vimainsurance.vimaadmin.dto.EmailAttachment;
import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EmailResponse;
import com.vimainsurance.vimaadmin.service.IEmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private static final String UTF_8 = "UTF-8";
    private static final String COMPANY_NAME = "Vima Insurance";
    private static final String COMPANY_NAME_KEY = "companyName";

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.from-name:Vima Insurance}")
    private String fromName;

    @Value("${app.base-url:http://localhost:7219}")
    private String baseUrl;

    @Override
    public EmailResponse sendSimpleEmail(EmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getBody(), false);
            
            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                helper.setCc(emailRequest.getCc());
            }
            
            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                helper.setBcc(emailRequest.getBcc());
            }
            
            mailSender.send(message);
            
            log.info("Simple email sent successfully to: {}", emailRequest.getTo());
            return EmailResponse.builder()
                    .success(true)
                    .message("Email sent successfully")
                    .build();
                    
        } catch (jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send simple email to: {}", emailRequest.getTo(), e);
            return EmailResponse.builder()
                    .success(false)
                    .message("Failed to send email")
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public EmailResponse sendHtmlEmail(EmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getBody(), true);
            
            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                helper.setCc(emailRequest.getCc());
            }
            
            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                helper.setBcc(emailRequest.getBcc());
            }
            
            mailSender.send(message);
            
            log.info("HTML email sent successfully to: {}", emailRequest.getTo());
            return EmailResponse.builder()
                    .success(true)
                    .message("HTML email sent successfully")
                    .build();
                    
        } catch (jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send HTML email to: {}", emailRequest.getTo(), e);
            return EmailResponse.builder()
                    .success(false)
                    .message("Failed to send HTML email")
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public EmailResponse sendTemplateEmail(EmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());
            
            // Process template
            Context context = new Context();
            if (emailRequest.getTemplateVariables() != null) {
                context.setVariables(emailRequest.getTemplateVariables());
            }
            
            String htmlContent = templateEngine.process(emailRequest.getTemplateName(), context);
            helper.setText(htmlContent, true);
            
            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                helper.setCc(emailRequest.getCc());
            }
            
            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                helper.setBcc(emailRequest.getBcc());
            }
            
            mailSender.send(message);
            
            log.info("Template email sent successfully to: {} using template: {}", 
                    emailRequest.getTo(), emailRequest.getTemplateName());
            return EmailResponse.builder()
                    .success(true)
                    .message("Template email sent successfully")
                    .build();
                    
        } catch (jakarta.mail.MessagingException | org.thymeleaf.exceptions.TemplateEngineException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send template email to: {} using template: {}", 
                    emailRequest.getTo(), emailRequest.getTemplateName(), e);
            return EmailResponse.builder()
                    .success(false)
                    .message("Failed to send template email")
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public EmailResponse sendEmailWithAttachments(EmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getBody(), emailRequest.isHtml());
            
            // Add attachments
            if (emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty()) {
                for (EmailAttachment attachment : emailRequest.getAttachments()) {
                    helper.addAttachment(attachment.getFileName(), 
                            new jakarta.mail.util.ByteArrayDataSource(
                                    attachment.getContent(), 
                                    attachment.getContentType()));
                }
            }
            
            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                helper.setCc(emailRequest.getCc());
            }
            
            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                helper.setBcc(emailRequest.getBcc());
            }
            
            mailSender.send(message);
            
            log.info("Email with attachments sent successfully to: {}", emailRequest.getTo());
            return EmailResponse.builder()
                    .success(true)
                    .message("Email with attachments sent successfully")
                    .build();
                    
        } catch (jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email with attachments to: {}", emailRequest.getTo(), e);
            return EmailResponse.builder()
                    .success(false)
                    .message("Failed to send email with attachments")
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public EmailResponse sendBulkEmail(EmailRequest emailRequest) {
        try {
            List<String> recipients = emailRequest.getToList();
            if (recipients == null || recipients.isEmpty()) {
                return EmailResponse.builder()
                        .success(false)
                        .message("No recipients specified for bulk email")
                        .build();
            }
            
            int successCount = 0;
            int failureCount = 0;
            
            for (String recipient : recipients) {
                if (sendEmailToRecipient(emailRequest, recipient)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
            
            log.info("Bulk email completed. Success: {}, Failures: {}", successCount, failureCount);
            return EmailResponse.builder()
                    .success(failureCount == 0)
                    .message(String.format("Bulk email completed. Success: %d, Failures: %d", successCount, failureCount))
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to send bulk email", e);
            return EmailResponse.builder()
                    .success(false)
                    .message("Failed to send bulk email")
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public EmailResponse sendWelcomeEmail(String recipientEmail, String userFullName, String username, String password) {
        EmailRequest emailRequest = EmailRequest.builder()
                .to(recipientEmail)
                .subject("Welcome to Vima Insurance")
                .templateName("welcome")
                .templateVariables(java.util.Map.of(
                        "userFullName", userFullName,
                        "username", username,
                        "temporaryPassword", password,
                        COMPANY_NAME_KEY, COMPANY_NAME
                ))
                .build();
        
        return sendTemplateEmail(emailRequest);
    }

    @Override
    public EmailResponse sendPasswordResetEmail(String recipientEmail, String newPassword, String userName) {
        EmailRequest emailRequest = EmailRequest.builder()
                .to(recipientEmail)
                .subject("New Password Generated - Vima Insurance")
                .templateName("password-reset")
                .templateVariables(java.util.Map.of(
                        "userName", userName,
                        "newPassword", newPassword,
                        "baseUrl", baseUrl,
                        COMPANY_NAME_KEY, COMPANY_NAME
                ))
                .build();
        
        return sendTemplateEmail(emailRequest);
    }

    @Override
    public EmailResponse sendQuoteNotificationEmail(String recipientEmail, String quoteId, String customerName) {
        EmailRequest emailRequest = EmailRequest.builder()
                .to(recipientEmail)
                .subject("New Quote Generated - Vima Insurance")
                .templateName("quote-notification")
                .templateVariables(java.util.Map.of(
                        "customerName", customerName,
                        "quoteId", quoteId,
                        COMPANY_NAME_KEY, COMPANY_NAME
                ))
                .build();
        
        return sendTemplateEmail(emailRequest);
    }

    /**
     * Helper method to send email to a single recipient
     * @param emailRequest Original email request
     * @param recipient Recipient email address
     * @return true if email was sent successfully, false otherwise
     */
    private boolean sendEmailToRecipient(EmailRequest emailRequest, String recipient) {
        try {
            EmailRequest singleEmailRequest = EmailRequest.builder()
                    .to(recipient)
                    .subject(emailRequest.getSubject())
                    .body(emailRequest.getBody())
                    .isHtml(emailRequest.isHtml())
                    .templateName(emailRequest.getTemplateName())
                    .templateVariables(emailRequest.getTemplateVariables())
                    .attachments(emailRequest.getAttachments())
                    .build();
            
            EmailResponse response;
            if (emailRequest.getTemplateName() != null && !emailRequest.getTemplateName().isEmpty()) {
                response = sendTemplateEmail(singleEmailRequest);
            } else if (emailRequest.isHtml()) {
                response = sendHtmlEmail(singleEmailRequest);
            } else {
                response = sendSimpleEmail(singleEmailRequest);
            }
            
            return response.isSuccess();
            
        } catch (Exception e) {
            log.error("Failed to send email to: {}", recipient, e);
            return false;
        }
    }
}
