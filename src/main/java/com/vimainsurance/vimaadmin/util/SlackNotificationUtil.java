package com.vimainsurance.vimaadmin.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.notification.slack.SlackChannel;
import com.vimainsurance.vimaadmin.notification.slack.SlackChannelRouter;

/**
 * Legacy Slack sender for the "New Policy Issued!" callers
 * ({@code CustomerServiceImpl}, {@code DealsServiceImpl}). New code should
 * prefer the unified {@code NotificationDispatcher} path.
 *
 * <p>URL resolution is delegated entirely to {@link SlackChannelRouter}.
 * Callers pass a {@link SlackChannel} label; this class never touches a
 * webhook URL directly.
 */
@Component
public class SlackNotificationUtil {
    private static final Logger logger = LoggerFactory.getLogger(SlackNotificationUtil.class);

    private final RestTemplate restTemplate;
    private final SlackChannelRouter slackChannelRouter;

    public SlackNotificationUtil(RestTemplate restTemplate, SlackChannelRouter slackChannelRouter) {
        this.restTemplate = restTemplate;
        this.slackChannelRouter = slackChannelRouter;
    }

    /** Plain-text message body posted to {@code channel}. */
    public void sendSlackMessage(String message, SlackChannel channel) {
        Optional<String> url = slackChannelRouter.resolveUrl(channel);
        if (url.isEmpty()) {
            return; // router already logged the reason
        }
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("text", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url.get(), request, String.class);
            logger.info("slack_legacy_sent channel={}", channel);
        } catch (Exception e) {
            logger.error("slack_legacy_failed channel={} error={}", channel, e.getMessage(), e);
        }
    }

    /** Slack attachment with title + body posted to {@code channel}. */
    public void sendSlackMessage(String title, String message, SlackChannel channel) {
        Optional<String> url = slackChannelRouter.resolveUrl(channel);
        if (url.isEmpty()) {
            return; // router already logged the reason
        }
        try {
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", "good");
            attachment.put("title", title);
            attachment.put("text", message);
            attachment.put("ts", System.currentTimeMillis() / 1000);

            Map<String, Object> payload = new HashMap<>();
            payload.put("attachments", new Object[] { attachment });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url.get(), request, String.class);
            logger.info("slack_legacy_sent channel={} title={}", channel, title);
        } catch (Exception e) {
            logger.error("slack_legacy_failed channel={} title={} error={}", channel, title, e.getMessage(), e);
        }
    }

    /**
     * Message body for Slack reminder channel when an employee submits a
     * claim via the portal.
     */
    public String buildEmployeeClaimSubmittedMessage(Claim claim, Organization org, Deals employee) {
        if (claim == null) {
            return "";
        }
        String orgName = org != null && org.getOrganizationName() != null && !org.getOrganizationName().isBlank()
            ? org.getOrganizationName() : "N/A";
        String submitter = "N/A";
        if (employee != null) {
            if (employee.getFullName() != null && !employee.getFullName().isBlank()) {
                submitter = employee.getFullName();
            } else if (employee.getEmail() != null && !employee.getEmail().isBlank()) {
                submitter = employee.getEmail();
            }
        }
        String claimNo = claim.getClaimNumber() != null ? claim.getClaimNumber() : "—";
        String member = claim.getMemberName() != null && !claim.getMemberName().isBlank() ? claim.getMemberName() : "—";
        String amount = claim.getClaimAmount() != null ? claim.getClaimAmount().toPlainString() : "—";
        String type = claim.getClaimType() != null ? claim.getClaimType().name() : "—";
        String hospital = claim.getHospitalName() != null && !claim.getHospitalName().isBlank() ? claim.getHospitalName() : "—";

        StringBuilder message = new StringBuilder();
        message.append(":inbox_tray: *Employee claim submitted*\n");
        message.append("• *Claim #:* ").append(claimNo).append("\n");
        message.append("• *Organization:* ").append(orgName).append("\n");
        message.append("• *Submitted by:* ").append(submitter).append("\n");
        message.append("• *Member / patient:* ").append(member).append("\n");
        message.append("• *Type:* ").append(type).append(" | *Amount:* ").append(amount).append("\n");
        message.append("• *Hospital:* ").append(hospital);
        return message.toString();
    }

    public String buildEndorsementNotificationMessage(Endorsement endorsement) {
        if (endorsement == null) {
            return "";
        }

        StringBuilder message = new StringBuilder();

        String endorsementTypeDisplay = formatEndorsementType(
                endorsement.getEndorsementType() != null ? endorsement.getEndorsementType().name() : "UNKNOWN");

        String organizationName = endorsement.getOrganization() != null
            ? endorsement.getOrganization().getOrganizationName()
            : "N/A";

        String uploadedByName = "N/A";
        if (endorsement.getUploadedBy() != null) {
            uploadedByName = endorsement.getUploadedBy().getFullName() != null
                ? endorsement.getUploadedBy().getFullName()
                : (endorsement.getUploadedBy().getUsername() != null
                    ? endorsement.getUploadedBy().getUsername()
                    : "N/A");
        }

        message.append(":clipboard: Type: ").append(endorsementTypeDisplay).append("\n");
        message.append(":busts_in_silhouette: Total Employees: ")
            .append(endorsement.getTotalEmployees() != null ? endorsement.getTotalEmployees() : 0).append("\n");
        message.append(":family: Total Dependents: ")
            .append(endorsement.getTotalDependents() != null ? endorsement.getTotalDependents() : 0).append("\n");
        message.append(":bust_in_silhouette: Uploaded By: ").append(uploadedByName).append("\n");
        message.append(":office: Organization: ").append(organizationName);

        return message.toString();
    }

    private String formatEndorsementType(String endorsementType) {
        if (endorsementType == null || endorsementType.isEmpty()) {
            return "Unknown";
        }
        String formatted = endorsementType.replace("_", " ");
        String[] words = formatted.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
        }
        return result.toString();
    }
}
