package com.vimainsurance.vimaadmin.util;

import java.net.URI;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Normalizes {@code document.documents.s3_key} for S3 API calls.
 * Prefer storing the object key; legacy rows may store a full S3/HTTPS URL or a CloudFront URL — the bucket name
 * lives in {@code s3_bucket}. This utility extracts only the object key for {@code GetObject}.
 */
public final class S3DocumentKeyUtil {

    private static final Logger log = LoggerFactory.getLogger(S3DocumentKeyUtil.class);

    private S3DocumentKeyUtil() {}

    /**
     * @param raw value from {@link com.vimainsurance.vimaadmin.entity.Document#getS3Key()} / DB column {@code s3_key}
     * @param documentId optional for logs when normalization runs
     * @return trimmed object key, or null if blank / unusable
     */
    public static String resolveObjectKeyForPresign(String raw, UUID documentId) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }

        if (t.startsWith("s3://")) {
            String rest = t.substring("s3://".length());
            int slash = rest.indexOf('/');
            if (slash >= 0 && slash < rest.length() - 1) {
                log.info("Document {}: using S3 object key parsed from s3:// URL in s3_key column", documentId);
                return rest.substring(slash + 1);
            }
            log.warn("Document {}: s3:// URL in s3_key could not be parsed. Value prefix: {}", documentId,
                    t.length() > 100 ? t.substring(0, 100) + "…" : t);
            return t;
        }

        if (t.startsWith("http://") || t.startsWith("https://")) {
            try {
                URI uri = URI.create(t);
                String host = uri.getHost();
                if (host != null && host.contains("cloudfront.net")) {
                    String path = uri.getPath();
                    if (path != null && !path.isEmpty() && !"/".equals(path)) {
                        String extracted = path.startsWith("/") ? path.substring(1) : path;
                        log.info("Document {}: object key parsed from CloudFront URL in s3_key (bucket from s3_bucket column)",
                                documentId);
                        return extracted;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // fall through to S3 host parsing
            }
            int marker = t.indexOf(".amazonaws.com/");
            if (marker > 0) {
                String extracted = t.substring(marker + ".amazonaws.com/".length());
                if (!extracted.isEmpty()) {
                    int q = extracted.indexOf('?');
                    if (q >= 0) {
                        extracted = extracted.substring(0, q);
                    }
                    log.info("Document {}: using S3 object key parsed from HTTPS URL stored in s3_key column", documentId);
                    return extracted;
                }
            }
            log.warn("Document {}: s3_key looks like a URL but object key could not be parsed; presign may fail. Prefix: {}",
                    documentId, t.length() > 100 ? t.substring(0, 100) + "…" : t);
        }

        return t;
    }
}
