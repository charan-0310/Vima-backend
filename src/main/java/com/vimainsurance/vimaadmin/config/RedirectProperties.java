package com.vimainsurance.vimaadmin.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * F-22: Allowlisted redirect targets for Keycloak action emails and other server-side redirects.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "redirect")
public class RedirectProperties {

    /** Default redirect base when a single canonical URL is required. */
    private String url = "";

    /** Comma-separated list in properties; bound as list by Spring Boot. */
    private List<String> allowedTargets = new ArrayList<>();
}
