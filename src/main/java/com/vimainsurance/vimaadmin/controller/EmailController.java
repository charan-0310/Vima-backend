package com.vimainsurance.vimaadmin.controller;

import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EmailResponse;
import com.vimainsurance.vimaadmin.service.IEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Tag(name = "Email Service", description = "Email service endpoints for sending various types of emails")
public class EmailController {

    @Autowired
    private final IEmailService emailService;

    @PostMapping("/send-simple")
    @Operation(summary = "Send a simple text email", description = "Send a plain text email to a recipient")
    public ResponseEntity<EmailResponse> sendSimpleEmail(@Valid @RequestBody EmailRequest emailRequest) {
        EmailResponse response = emailService.sendSimpleEmail(emailRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-html")
    @Operation(summary = "Send an HTML email", description = "Send an HTML formatted email to a recipient")
    public ResponseEntity<EmailResponse> sendHtmlEmail(@Valid @RequestBody EmailRequest emailRequest) {
        EmailResponse response = emailService.sendHtmlEmail(emailRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-template")
    @Operation(summary = "Send a template email", description = "Send an email using a Thymeleaf template")
    public ResponseEntity<EmailResponse> sendTemplateEmail(@Valid @RequestBody EmailRequest emailRequest) {
        EmailResponse response = emailService.sendTemplateEmail(emailRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-with-attachments")
    @Operation(summary = "Send email with attachments", description = "Send an email with file attachments")
    public ResponseEntity<EmailResponse> sendEmailWithAttachments(@Valid @RequestBody EmailRequest emailRequest) {
        EmailResponse response = emailService.sendEmailWithAttachments(emailRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-bulk")
    @Operation(summary = "Send bulk email", description = "Send the same email to multiple recipients")
    public ResponseEntity<EmailResponse> sendBulkEmail(@Valid @RequestBody EmailRequest emailRequest) {
        EmailResponse response = emailService.sendBulkEmail(emailRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/welcome")
    @Operation(summary = "Send welcome email", description = "Send a welcome email to a new user")
    public ResponseEntity<EmailResponse> sendWelcomeEmail(
            @RequestParam String email,
            @RequestParam String userFullName,
            @RequestParam String username,
            @RequestParam(required = false) String password) {
        String generatedPassword = password != null ? password : "TempPass123!";
        EmailResponse response = emailService.sendWelcomeEmail(email, userFullName, username, generatedPassword);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset")
    @Operation(summary = "Send password reset email", description = "Send a password reset email with new password to a user")
    public ResponseEntity<EmailResponse> sendPasswordResetEmail(
            @RequestParam String email,
            @RequestParam String newPassword,
            @RequestParam String userName) {
        EmailResponse response = emailService.sendPasswordResetEmail(email, newPassword, userName);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/quote-notification")
    @Operation(summary = "Send quote notification email", description = "Send a quote notification email to a customer")
    public ResponseEntity<EmailResponse> sendQuoteNotificationEmail(
            @RequestParam String email,
            @RequestParam String quoteId,
            @RequestParam String customerName) {
        EmailResponse response = emailService.sendQuoteNotificationEmail(email, quoteId, customerName);
        return ResponseEntity.ok(response);
    }
}
