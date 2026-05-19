package com.vimainsurance.vimaadmin.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-28: Redacts PII and secrets from log lines before they reach appenders (console / CloudWatch).
 */
public final class LogSensitiveDataMasker {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern BEARER = Pattern.compile(
            "(?i)(Bearer\\s+)[A-Za-z0-9\\-._~+/]+=*");
    private static final Pattern JWT = Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*");
    private static final Pattern AUTHORIZATION_HEADER = Pattern.compile(
            "(?i)(Authorization\\s*[:=]\\s*)([^\\s,;]+)");
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(password|secret|token|api[_-]?key|client[_-]?secret|refresh[_-]?token|access[_-]?key)\\s*[:=]\\s*([^\\s,;\"']+)");
    private static final Pattern AADHAAR = Pattern.compile(
            "(?<!\\d)(\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4})(?!\\d)");
    private static final Pattern PAN = Pattern.compile(
            "\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");
    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern MOBILE_IN = Pattern.compile(
            "(?<!\\d)(?:\\+91[\\s-]?)?[6-9]\\d{9}(?!\\d)");

    private LogSensitiveDataMasker() {
    }

    public static String maskMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        String out = message;
        out = replaceAll(out, BEARER, m -> m.group(1) + REDACTED);
        out = replaceAll(out, JWT, m -> REDACTED + "_JWT");
        out = replaceAll(out, AUTHORIZATION_HEADER, m -> m.group(1) + REDACTED);
        out = replaceAll(out, KEY_VALUE_SECRET, m -> {
            String value = m.group(2);
            if (value.contains(REDACTED)) {
                return m.group(0);
            }
            return m.group(1) + "=" + REDACTED;
        });
        out = replaceAll(out, AADHAAR, LogSensitiveDataMasker::maskAadhaarDigits);
        out = replaceAll(out, PAN, LogSensitiveDataMasker::maskPanValue);
        out = replaceAll(out, EMAIL, LogSensitiveDataMasker::maskEmailValue);
        out = replaceAll(out, MOBILE_IN, LogSensitiveDataMasker::maskPhoneValue);
        return out;
    }

    private static String replaceAll(String input, Pattern pattern, java.util.function.Function<Matcher, String> replacer) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacer.apply(matcher)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String maskAadhaarDigits(Matcher m) {
        String digits = m.group(1).replaceAll("\\D", "");
        if (digits.length() != 12) {
            return "XXXX XXXX XXXX";
        }
        return "XXXX XXXX " + digits.substring(8);
    }

    private static String maskPanValue(Matcher m) {
        String pan = m.group();
        if (pan.length() < 5) {
            return "XXXXX";
        }
        return pan.substring(0, 3) + "XXXXX" + pan.substring(pan.length() - 2);
    }

    private static String maskEmailValue(Matcher m) {
        String email = m.group();
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***@***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return "*".repeat(local.length()) + domain;
        }
        return local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + domain;
    }

    private static String maskPhoneValue(Matcher m) {
        String digits = m.group().replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "**********";
        }
        return "******" + digits.substring(digits.length() - 4);
    }
}
