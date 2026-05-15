package com.vimainsurance.vimaadmin.notification.slack;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Single source of truth for "given an environment and a logical Slack channel
 * label, what URL do we POST to?". Every Slack send in this codebase must go
 * through this bean — no caller is permitted to read a Slack URL directly from
 * properties or env vars.
 *
 * <h2>Safety invariants</h2>
 * <ul>
 *   <li><b>Non-prod always wins.</b> In {@link VimaEnvironment#LOCAL},
 *       {@link VimaEnvironment#DEV} and {@link VimaEnvironment#STAGING} the
 *       router resolves any requested channel to
 *       {@link SlackChannel#TEST_NOTIFICATIONS}. There is no opt-out and no
 *       host-env override.</li>
 *   <li><b>Missing URL → skip, not silent fall-through.</b> If the URL for a
 *       requested label is blank we return {@link Optional#empty()} and the
 *       caller logs+skips. We never fall back to another channel.</li>
 *   <li><b>{@link SlackChannel#TEST_NOTIFICATIONS} is rejected in prod.</b>
 *       Catches a stray non-prod-style call from accidentally posting to
 *       production.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackChannelRouter {

    private final VimaEnvironment env;
    private final SlackChannelProperties props;

    @PostConstruct
    void logRoutingTable() {
        log.info("slack_routing_init env={} testNotifications={} reminders={} supportClaims={} policyWins={}",
                env,
                isConfigured(props.getTestNotificationsUrl()),
                isConfigured(props.getRemindersUrl()),
                isConfigured(props.getSupportClaimsUrl()),
                isConfigured(props.getPolicyWinsUrl()));
    }

    /**
     * Map a {@link NotificationEventType} to the channel it belongs in.
     * Claim lifecycle events go to {@link SlackChannel#SUPPORT_CLAIMS};
     * everything else goes to {@link SlackChannel#REMINDERS}.
     */
    public SlackChannel channelForEvent(NotificationEventType eventType) {
        if (eventType == null) {
            return SlackChannel.REMINDERS;
        }
        return switch (eventType) {
            case EMPLOYEE_CLAIM_SUBMITTED,
                    EMPLOYEE_CLAIM_QUERY_RAISED,
                    EMPLOYEE_CLAIM_QUERY_RESPONDED,
                    EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED,
                    EMPLOYEE_CLAIM_APPROVED,
                    EMPLOYEE_CLAIM_REJECTED,
                    EMPLOYEE_CLAIM_SETTLED -> SlackChannel.SUPPORT_CLAIMS;
            default -> SlackChannel.REMINDERS;
        };
    }

    /**
     * Resolve the physical webhook URL for {@code requested} in the current
     * environment. Returns {@link Optional#empty()} when nothing should be
     * sent (with a {@code WARN} log explaining why).
     */
    public Optional<String> resolveUrl(SlackChannel requested) {
        if (requested == null) {
            log.warn("slack_routing_skip reason=null_channel env={}", env);
            return Optional.empty();
        }
        if (env.isNonProd()) {
            return resolveNonProd(requested);
        }
        return resolveProd(requested);
    }

    private Optional<String> resolveNonProd(SlackChannel requested) {
        String url = blankToNull(props.getTestNotificationsUrl());
        if (url == null) {
            log.warn("slack_routing_skip reason=test_notifications_url_blank env={} requested={}",
                    env, requested);
            return Optional.empty();
        }
        if (requested != SlackChannel.TEST_NOTIFICATIONS) {
            log.debug("slack_routing_rerouted_to_test env={} requested={} actual=TEST_NOTIFICATIONS",
                    env, requested);
        }
        return Optional.of(url);
    }

    private Optional<String> resolveProd(SlackChannel requested) {
        if (requested == SlackChannel.TEST_NOTIFICATIONS) {
            log.warn("slack_routing_skip reason=test_channel_in_prod env=PROD");
            return Optional.empty();
        }
        String url = switch (requested) {
            case REMINDERS -> blankToNull(props.getRemindersUrl());
            case SUPPORT_CLAIMS -> blankToNull(props.getSupportClaimsUrl());
            case POLICY_WINS -> blankToNull(props.getPolicyWinsUrl());
            case TEST_NOTIFICATIONS -> null; // unreachable; handled above
        };
        if (url == null) {
            log.warn("slack_routing_skip reason=url_not_configured env=PROD requested={}", requested);
            return Optional.empty();
        }
        return Optional.of(url);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String isConfigured(String url) {
        return (url == null || url.isBlank()) ? "<unset>" : "<set>";
    }
}
