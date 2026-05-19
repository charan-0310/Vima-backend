package com.vimainsurance.vimaadmin.ratelimit;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * F-04 — Matches client IPs against a comma-separated allowlist (exact IPv4/IPv6 or IPv4 CIDR).
 */
public final class RateLimitExemptIpMatcher {

    private final List<Rule> rules;

    public RateLimitExemptIpMatcher(String exemptIpsConfig) {
        this.rules = parse(exemptIpsConfig);
    }

    public boolean isExempt(String ip) {
        if (ip == null || ip.isBlank() || rules.isEmpty()) {
            return false;
        }
        String trimmed = ip.trim();
        for (Rule rule : rules) {
            if (rule.matches(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private static List<Rule> parse(String config) {
        List<Rule> parsed = new ArrayList<>();
        if (config == null || config.isBlank()) {
            return parsed;
        }
        for (String part : config.split(",")) {
            String entry = part.trim();
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.contains("/")) {
                CidrRule cidr = CidrRule.tryParse(entry);
                if (cidr != null) {
                    parsed.add(cidr);
                }
            } else {
                parsed.add(new ExactRule(entry));
            }
        }
        return parsed;
    }

    private interface Rule {
        boolean matches(String ip);
    }

    private record ExactRule(String value) implements Rule {
        @Override
        public boolean matches(String ip) {
            return value.equalsIgnoreCase(ip);
        }
    }

    /**
     * IPv4 CIDR only ({@code a.b.c.d/n}). Sufficient for private ranges documented in F-04.
     */
    private static final class CidrRule implements Rule {
        private final int network;
        private final int mask;

        private CidrRule(int network, int mask) {
            this.network = network;
            this.mask = mask;
        }

        static CidrRule tryParse(String cidr) {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return null;
            }
            try {
                int prefix = Integer.parseInt(parts[1].trim());
                if (prefix < 0 || prefix > 32) {
                    return null;
                }
                byte[] addr = InetAddress.getByName(parts[0].trim()).getAddress();
                if (addr.length != 4) {
                    return null;
                }
                int ip = toInt(addr);
                int mask = prefix == 0 ? 0 : -1 << (32 - prefix);
                return new CidrRule(ip & mask, mask);
            } catch (UnknownHostException | NumberFormatException e) {
                return null;
            }
        }

        @Override
        public boolean matches(String ip) {
            try {
                byte[] addr = InetAddress.getByName(ip).getAddress();
                if (addr.length != 4) {
                    return false;
                }
                int candidate = toInt(addr);
                return (candidate & mask) == network;
            } catch (UnknownHostException e) {
                return false;
            }
        }

        private static int toInt(byte[] octets) {
            return ((octets[0] & 0xff) << 24)
                    | ((octets[1] & 0xff) << 16)
                    | ((octets[2] & 0xff) << 8)
                    | (octets[3] & 0xff);
        }
    }
}
