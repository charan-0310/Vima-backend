package com.vimainsurance.vimaadmin.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.vimainsurance.vimaadmin.config.RedirectProperties;

import jakarta.annotation.PostConstruct;

/**
 * Validates outbound redirect URIs against {@code redirect.allowed-targets} (F-22).
 */
@Component
public class RedirectTargetValidator {

    private final RedirectProperties redirectProperties;
    private List<URI> allowedUris = List.of();

    public RedirectTargetValidator(RedirectProperties redirectProperties) {
        this.redirectProperties = redirectProperties;
    }

    @PostConstruct
    void init() {
        List<URI> parsed = new ArrayList<>();
        if (StringUtils.hasText(redirectProperties.getUrl())) {
            addIfValid(parsed, redirectProperties.getUrl().trim());
        }
        if (redirectProperties.getAllowedTargets() != null) {
            for (String target : redirectProperties.getAllowedTargets()) {
                if (StringUtils.hasText(target)) {
                    addIfValid(parsed, target.trim());
                }
            }
        }
        allowedUris = List.copyOf(parsed);
    }

    /**
     * @return true when {@code url} is null/blank or matches the configured allowlist
     */
    public boolean isAllowed(String url) {
        if (!StringUtils.hasText(url)) {
            return true;
        }
        URI target = parseHttpUri(url.trim());
        if (target == null) {
            return false;
        }
        for (URI allowed : allowedUris) {
            if (matchesAllowed(target, allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @throws IllegalArgumentException when the URL is present but not allowlisted
     */
    public void requireAllowed(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        if (!isAllowed(url)) {
            throw new IllegalArgumentException("Redirect target is not allowlisted");
        }
    }

    private static void addIfValid(List<URI> parsed, String value) {
        URI uri = parseHttpUri(value);
        if (uri != null) {
            parsed.add(uri);
        }
    }

    private static URI parseHttpUri(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return null;
            }
            String normalized = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(normalized) && !"https".equals(normalized)) {
                return null;
            }
            if (!StringUtils.hasText(uri.getHost())) {
                return null;
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean matchesAllowed(URI target, URI allowed) {
        if (!sameOrigin(target, allowed)) {
            return false;
        }
        String allowedPath = normalizePath(allowed.getPath());
        if (allowedPath.isEmpty() || "/".equals(allowedPath)) {
            return true;
        }
        String targetPath = normalizePath(target.getPath());
        return targetPath.equals(allowedPath) || targetPath.startsWith(allowedPath.endsWith("/")
                ? allowedPath
                : allowedPath + "/");
    }

    private static boolean sameOrigin(URI a, URI b) {
        return a.getScheme().equalsIgnoreCase(b.getScheme())
                && a.getHost().equalsIgnoreCase(b.getHost())
                && a.getPort() == b.getPort();
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
    }
}
