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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.RawMessage;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private static final String UTF_8 = "UTF-8";
    private static final String COMPANY_NAME = "Vima Insurance";
    private static final String COMPANY_NAME_KEY = "companyName";
    private static final String MAIL_PROVIDER_SES = "ses";
    private static final Pattern INLINE_DATA_IMAGE_PATTERN = Pattern.compile(
            "<img\\b([^>]*?)\\bsrc\\s*=\\s*['\"](data:image/([a-zA-Z0-9.+-]+);base64,([^'\"]+))['\"]([^>]*)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final SesV2Client sesV2Client;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${aws.ses.from-email:}")
    private String sesFromEmail;

    @Value("${aws.ses.configuration-set:}")
    private String sesConfigurationSet;

    @Value("${app.email.from-name:Vima Insurance}")
    private String fromName;

    @Value("${app.base-url:http://localhost:7219}")
    private String baseUrl;

    @Value("${mail.provider:gmail}")
    private String mailProvider;

    @Override
    public EmailResponse sendSimpleEmail(EmailRequest emailRequest) {
        try {
            sendEmail(emailRequest, false, false);
            
            log.info("Simple email sent successfully to: {}", emailRequest.getTo());
            return EmailResponse.builder()
                    .success(true)
                    .message("Email sent successfully")
                    .build();
                    
        } catch (Exception e) {
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
            boolean hasInlineDataImage = containsInlineDataImages(emailRequest.getBody());
            sendEmail(emailRequest, true, hasInlineDataImage);
            
            log.info("HTML email sent successfully to: {}", emailRequest.getTo());
            return EmailResponse.builder()
                    .success(true)
                    .message("HTML email sent successfully")
                    .build();
                    
        } catch (Exception e) {
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
            Context context = new Context();
            if (emailRequest.getTemplateVariables() != null) {
                context.setVariables(emailRequest.getTemplateVariables());
            }
            
            String htmlContent = templateEngine.process(emailRequest.getTemplateName(), context);
            EmailRequest templateEmailRequest = EmailRequest.builder()
                    .to(emailRequest.getTo())
                    .toList(emailRequest.getToList())
                    .cc(emailRequest.getCc())
                    .ccList(emailRequest.getCcList())
                    .bcc(emailRequest.getBcc())
                    .bccList(emailRequest.getBccList())
                    .subject(emailRequest.getSubject())
                    .body(htmlContent)
                    .isHtml(true)
                    .build();
            sendEmail(templateEmailRequest, true, false);
            
            log.info("Template email sent successfully to: {} using template: {}", 
                    emailRequest.getTo(), emailRequest.getTemplateName());
            return EmailResponse.builder()
                    .success(true)
                    .message("Template email sent successfully")
                    .build();
                    
        } catch (Exception e) {
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
            sendEmail(emailRequest, emailRequest.isHtml(), true);
            
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

    private void sendEmail(EmailRequest emailRequest, boolean html, boolean withAttachments)
            throws jakarta.mail.MessagingException, java.io.UnsupportedEncodingException {
        if (useSesProvider()) {
            if (withAttachments) {
                sendRawEmailViaSes(emailRequest, html);
            } else {
                sendSimpleEmailViaSes(emailRequest, html);
            }
            return;
        }

        sendViaSmtp(emailRequest, html, withAttachments);
    }

    private boolean useSesProvider() {
        return MAIL_PROVIDER_SES.equalsIgnoreCase(mailProvider);
    }

    private void applySesConfigurationSet(SendEmailRequest.Builder builder) {
        if (sesConfigurationSet != null && !sesConfigurationSet.isBlank()) {
            builder.configurationSetName(sesConfigurationSet.trim());
        }
    }

    private void sendSimpleEmailViaSes(EmailRequest emailRequest, boolean html) {
        String body = resolveBody(emailRequest);
        String senderEmail = resolveFromEmail();
        List<String> toAddresses = sanitizeAddresses(resolveToAddresses(emailRequest));
        List<String> ccAddresses = resolveOptionalAddresses(emailRequest.getCc(), emailRequest.getCcList());
        List<String> bccAddresses = resolveOptionalAddresses(emailRequest.getBcc(), emailRequest.getBccList());

        if (toAddresses.isEmpty()) {
            throw new IllegalArgumentException("At least one valid recipient email is required.");
        }

        Destination destination = Destination.builder()
                .toAddresses(toAddresses)
                .ccAddresses(ccAddresses)
                .bccAddresses(bccAddresses)
                .build();

        Content subject = Content.builder().data(emailRequest.getSubject()).charset(UTF_8).build();
        Body messageBody = Body.builder()
                .text(!html ? Content.builder().data(body).charset(UTF_8).build() : null)
                .html(html ? Content.builder().data(body).charset(UTF_8).build() : null)
                .build();

        Message message = Message.builder()
                .subject(subject)
                .body(messageBody)
                .build();

        SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                .fromEmailAddress(senderEmail)
                .destination(destination)
                .content(EmailContent.builder().simple(message).build());
        applySesConfigurationSet(requestBuilder);

        sesV2Client.sendEmail(requestBuilder.build());
    }

    private void sendRawEmailViaSes(EmailRequest emailRequest, boolean html)
            throws jakarta.mail.MessagingException, java.io.UnsupportedEncodingException {
        String senderEmail = resolveFromEmail();
        List<String> toAddresses = sanitizeAddresses(resolveToAddresses(emailRequest));
        if (toAddresses.isEmpty()) {
            throw new IllegalArgumentException("At least one valid recipient email is required.");
        }
        MimeMessage mimeMessage = buildMimeMessage(emailRequest, html, true);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);

            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .fromEmailAddress(senderEmail)
                    .destination(Destination.builder().toAddresses(toAddresses).build())
                    .content(EmailContent.builder()
                            .raw(RawMessage.builder()
                                    .data(SdkBytes.fromByteArray(outputStream.toByteArray()))
                                    .build())
                            .build());
            applySesConfigurationSet(requestBuilder);

            sesV2Client.sendEmail(requestBuilder.build());
        } catch (java.io.IOException | jakarta.mail.MessagingException e) {
            throw new RuntimeException("Failed to build raw SES email payload", e);
        }
    }

    private void sendViaSmtp(EmailRequest emailRequest, boolean html, boolean withAttachments)
            throws jakarta.mail.MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = buildMimeMessage(emailRequest, html, withAttachments);
        mailSender.send(message);
    }

    private MimeMessage buildMimeMessage(EmailRequest emailRequest, boolean html, boolean withAttachments)
            throws jakarta.mail.MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);

        String senderEmail = resolveFromEmail();
        String senderName = Objects.requireNonNullElse(fromName, "");
        String[] toAddresses = resolveToAddresses(emailRequest);
        String subject = Objects.requireNonNullElse(emailRequest.getSubject(), "");

        helper.setFrom(senderEmail, senderName);
        helper.setTo(toAddresses);
        helper.setSubject(subject);

        String body = resolveBody(emailRequest);
        if (html && body != null && containsInlineDataImages(body)) {
            body = attachInlineDataImages(helper, body);
        }
        helper.setText(body != null ? body : "", html || emailRequest.getTemplateName() != null);

        if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
            helper.setCc(emailRequest.getCc());
        }
        if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
            helper.setBcc(emailRequest.getBcc());
        }

        if (withAttachments && emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty()) {
            for (EmailAttachment attachment : emailRequest.getAttachments()) {
                helper.addAttachment(attachment.getFileName(),
                        new jakarta.mail.util.ByteArrayDataSource(
                                attachment.getContent(),
                                attachment.getContentType()));
            }
        }
        return message;
    }

    private String[] resolveToAddresses(EmailRequest emailRequest) {
        if (emailRequest.getToList() != null && !emailRequest.getToList().isEmpty()) {
            return sanitizeAddresses(emailRequest.getToList()).toArray(new String[0]);
        }
        if (emailRequest.getTo() != null && !emailRequest.getTo().isBlank()) {
            return new String[]{emailRequest.getTo()};
        }
        return new String[0];
    }

    private String resolveBody(EmailRequest emailRequest) {
        String body = emailRequest.getBody();
        if (emailRequest.getTemplateName() != null && !emailRequest.getTemplateName().isBlank()) {
            Context context = new Context();
            if (emailRequest.getTemplateVariables() != null) {
                context.setVariables(emailRequest.getTemplateVariables());
            }
            body = templateEngine.process(emailRequest.getTemplateName(), context);
        }
        return body != null ? body : "";
    }

    private String resolveFromEmail() {
        if (sesFromEmail != null && !sesFromEmail.isBlank()) {
            return sesFromEmail;
        }
        if (fromEmail != null && !fromEmail.isBlank()) {
            return fromEmail;
        }
        throw new IllegalStateException("Sender email is not configured. Set spring.mail.username or aws.ses.from-email.");
    }

    private List<String> sanitizeAddresses(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }
        return addresses.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean containsInlineDataImages(String body) {
        return body != null && INLINE_DATA_IMAGE_PATTERN.matcher(body).find();
    }

    private String attachInlineDataImages(MimeMessageHelper helper, String htmlBody) throws jakarta.mail.MessagingException {
        Matcher matcher = INLINE_DATA_IMAGE_PATTERN.matcher(htmlBody);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String mimeSubtype = matcher.group(3);
            String base64Data = matcher.group(4);
            String contentType = "image/" + (mimeSubtype != null ? mimeSubtype.toLowerCase() : "png");
            String cid = "inline-" + UUID.randomUUID();
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(base64Data.replaceAll("\\s", ""));
            } catch (IllegalArgumentException ex) {
                // If decode fails, keep original image tag as-is.
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            helper.addInline(
                    cid,
                    new jakarta.mail.util.ByteArrayDataSource(decoded, contentType));

            String replacement = "<img" + matcher.group(1) + "src=\"cid:" + cid + "\"" + matcher.group(5) + ">";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<String> sanitizeAddresses(String[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return List.of();
        }
        return Arrays.stream(addresses)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> resolveOptionalAddresses(String singleAddress, List<String> addressList) {
        List<String> fromList = sanitizeAddresses(addressList);
        if (!fromList.isEmpty()) {
            return fromList;
        }
        if (singleAddress == null || singleAddress.isBlank()) {
            return List.of();
        }
        // Supports single address and comma-separated addresses.
        return sanitizeAddresses(singleAddress.split(","));
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
                        "baseUrl", baseUrl,
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
