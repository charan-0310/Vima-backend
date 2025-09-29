package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EmailResponse;
import com.vimainsurance.vimaadmin.service.serviceimpl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Test Company");
    }

    @Test
    void testSendSimpleEmail_Success() {
        // Given
        EmailRequest emailRequest = EmailRequest.builder()
                .to("recipient@example.com")
                .subject("Test Subject")
                .body("Test Body")
                .build();

        // Mock the mail sender to not throw exceptions
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        EmailResponse response = emailService.sendSimpleEmail(emailRequest);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Email sent successfully", response.getMessage());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendWelcomeEmail() {
        // Given
        String recipientEmail = "user@example.com";
        String userName = "John Doe";

        // Mock template engine to return HTML content
        when(templateEngine.process(eq("welcome"), any())).thenReturn("<html>Welcome email</html>");
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        EmailResponse response = emailService.sendWelcomeEmail(recipientEmail, userName, "");

        // Then
        assertNotNull(response);
        verify(templateEngine, times(1)).process(eq("welcome"), any());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmail() {
        // Given
        String recipientEmail = "user@example.com";
        String resetToken = "reset-token-123";
        String userName = "John Doe";

        // Mock template engine to return HTML content
        when(templateEngine.process(eq("password-reset"), any())).thenReturn("<html>Password reset email</html>");
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        EmailResponse response = emailService.sendPasswordResetEmail(recipientEmail, resetToken, userName);

        // Then
        assertNotNull(response);
        verify(templateEngine, times(1)).process(eq("password-reset"), any());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendQuoteNotificationEmail() {
        // Given
        String recipientEmail = "customer@example.com";
        String quoteId = "QUOTE-123";
        String customerName = "Jane Doe";

        // Mock template engine to return HTML content
        when(templateEngine.process(eq("quote-notification"), any())).thenReturn("<html>Quote notification email</html>");
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        EmailResponse response = emailService.sendQuoteNotificationEmail(recipientEmail, quoteId, customerName);

        // Then
        assertNotNull(response);
        verify(templateEngine, times(1)).process(eq("quote-notification"), any());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
