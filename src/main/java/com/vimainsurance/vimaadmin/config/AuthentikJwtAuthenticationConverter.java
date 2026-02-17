package com.vimainsurance.vimaadmin.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * JWT Authentication Converter for Authentik and Keycloak.
 *
 * Extracts roles/authorities from JWT tokens and converts them to Spring Security authorities.
 * Used for both Authentik (dev/prod) and Keycloak (UAT) - issuer-uri is profile-specific in application-*.properties.
 *
 * Looks for roles in:
 * - 'groups' claim (Authentik default)
 * - 'roles' claim
 * - 'realm_access.roles' (Keycloak)
 * - 'resource_access' client roles (Keycloak)
 *
 * Roles are mapped to authorities with the 'ROLE_' prefix for Spring Security compatibility.
 */
@Component
public class AuthentikJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * Extract authorities from JWT claims
     * 
     * @param jwt JWT token from Authentik
     * @return Collection of granted authorities
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Get default authorities (scope, etc.)
        authorities.addAll(defaultAuthoritiesConverter.convert(jwt));

        // Extract roles from JWT claims
        List<String> roles = extractRoles(jwt);
        
        // Convert roles to authorities with ROLE_ prefix for Spring Security
        authorities.addAll(roles.stream()
                .map(role -> {
                    // Ensure role starts with ROLE_ prefix
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    return new SimpleGrantedAuthority(authority);
                })
                .collect(Collectors.toList()));

        // Also add roles without ROLE_ prefix for flexibility (some methods check without prefix)
        authorities.addAll(roles.stream()
                .filter(role -> !role.startsWith("ROLE_"))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));

        return authorities;
    }

    /**
     * Extract roles from various JWT claim locations
     * 
     * Authentik typically stores roles/groups in the 'groups' claim.
     * This method also checks 'roles' and 'realm_access.roles' for compatibility.
     * 
     * @param jwt JWT token
     * @return List of role names
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        List<String> roles = new ArrayList<>();

        // Try 'groups' claim (Authentik default)
        Object groupsClaim = jwt.getClaim("groups");
        if (groupsClaim != null) {
            if (groupsClaim instanceof ArrayList) {
                ((ArrayList<?>) groupsClaim).forEach(item -> {
                    if (item instanceof String) {
                        String itemString = (String) item;
                        roles.add(itemString.replace(" Group", ""));
                    }
                });
            } else if (groupsClaim instanceof String) {
                roles.add((String) groupsClaim);
            }
        }

        // Try 'roles' claim
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim != null) {
            if (rolesClaim instanceof List) {
                ((List<?>) rolesClaim).forEach(item -> {
                    if (item instanceof String && !roles.contains(item)) {
                        roles.add((String) item);
                    }
                });
            } else if (rolesClaim instanceof String && !roles.contains(rolesClaim)) {
                roles.add((String) rolesClaim);
            }
        }

        // Try 'realm_access.roles' (Keycloak-style)
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            Object realmRoles = realmAccess.get("roles");
            if (realmRoles instanceof List) {
                ((List<?>) realmRoles).forEach(item -> {
                    if (item instanceof String && !roles.contains(item)) {
                        roles.add((String) item);
                    }
                });
            }
        }

        // Try 'resource_access' claim (for client-specific roles)
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            for (Object value : resourceAccess.values()) {
                if (value instanceof Map) {
                    Map<String, Object> clientAccess = (Map<String, Object>) value;
                    Object clientRoles = clientAccess.get("roles");
                    if (clientRoles instanceof List) {
                        ((List<?>) clientRoles).forEach(item -> {
                            if (item instanceof String && !roles.contains(item)) {
                                roles.add((String) item);
                            }
                        });
                    }
                }
            }
        }

        return roles;
    }
}

