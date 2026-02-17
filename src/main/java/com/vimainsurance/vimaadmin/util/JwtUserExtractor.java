package com.vimainsurance.vimaadmin.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.exception.OrganizationAccessDeniedException;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;

/**
 * Helper component to extract user details from JWT tokens issued by Authentik.
 * 
 * This backend only validates JWT tokens issued by Authentik.
 * React performs login and token exchange - no login endpoints or callback endpoints are required here.
 * 
 * The JWT token is validated by Spring Security OAuth2 Resource Server against the configured issuer-uri.
 */
@Component
public class JwtUserExtractor {

    @Autowired
    private IAdminUserRepository adminUserRepository;

    /**
     * Get the current JWT token from the security context
     * 
     * @return Optional JWT token if authenticated, empty otherwise
     */
    public Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth.getToken());
        }
        
        return Optional.empty();
    }

    /**
     * Extract email from JWT token
     * 
     * @param jwt JWT token
     * @return email if present, null otherwise
     */
    public String getEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    /**
     * Extract email from current authenticated JWT token
     * 
     * @return email if present, null otherwise
     */
    public String getCurrentEmail() {
        return getCurrentJwt()
                .map(this::getEmail)
                .orElse(null);
    }

    /**
     * Extract preferred_username from JWT token
     * 
     * @param jwt JWT token
     * @return preferred_username if present, null otherwise
     */
    public String getPreferredUsername(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }

    /**
     * Extract preferred_username from current authenticated JWT token
     * 
     * @return preferred_username if present, null otherwise
     */
    public String getCurrentPreferredUsername() {
        return getCurrentJwt()
                .map(this::getPreferredUsername)
                .orElse(null);
    }

    /**
     * Extract subject (sub) from JWT token
     * 
     * @param jwt JWT token
     * @return subject if present, null otherwise
     */
    public String getSubject(Jwt jwt) {
        return jwt.getSubject();
    }

    /**
     * Extract subject (sub) from current authenticated JWT token
     * 
     * @return subject if present, null otherwise
     */
    public String getCurrentSubject() {
        return getCurrentJwt()
                .map(this::getSubject)
                .orElse(null);
    }

    /**
     * Extract groups from JWT token.
     * Authentik typically includes groups in the 'groups' claim or as a list.
     * 
     * @param jwt JWT token
     * @return List of groups if present, empty list otherwise
     */
    @SuppressWarnings("unchecked")
    public List<String> getGroups(Jwt jwt) {
        List<String> groups = new ArrayList<>();
        
        // Try 'groups' claim
        Object groupsClaim = jwt.getClaim("groups");
        if (groupsClaim instanceof List) {
            ((List<?>) groupsClaim).forEach(item -> {
                if (item instanceof String) {
                    groups.add((String) item);
                }
            });
        } else if (groupsClaim instanceof String) {
            groups.add((String) groupsClaim);
        }
        
        // If no groups found, try 'realm_access.roles' (for Keycloak-style tokens)
        if (groups.isEmpty()) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                Object roles = realmAccess.get("roles");
                if (roles instanceof List) {
                    ((List<?>) roles).forEach(item -> {
                        if (item instanceof String) {
                            groups.add((String) item);
                        }
                    });
                }
            }
        }
        
        return groups;
    }

    /**
     * Extract groups from current authenticated JWT token
     * 
     * @return List of groups if present, empty list otherwise
     */
    public List<String> getCurrentGroups() {
        return getCurrentJwt()
                .map(this::getGroups)
                .orElse(new ArrayList<>());
    }

    /**
     * Extract roles from JWT token.
     * Roles might be in 'roles' claim or as part of 'realm_access' or 'groups'
     * 
     * @param jwt JWT token
     * @return List of roles if present, empty list otherwise
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(Jwt jwt) {
        List<String> roles = new ArrayList<>();
        
        // Try 'roles' claim
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof List) {
            ((List<?>) rolesClaim).forEach(item -> {
                if (item instanceof String) {
                    roles.add((String) item);
                }
            });
        } else if (rolesClaim instanceof String) {
            roles.add((String) rolesClaim);
        }
        
        // If no roles found, try 'groups' (sometimes roles are in groups)
        if (roles.isEmpty()) {
            roles.addAll(getGroups(jwt));
        }
        
        return roles;
    }

    /**
     * Extract roles from current authenticated JWT token
     * 
     * @return List of roles if present, empty list otherwise
     */
    public List<String> getCurrentRoles() {
        return getCurrentJwt()
                .map(this::getRoles)
                .orElse(new ArrayList<>());
    }

    /**
     * Get all claims from the JWT token
     * 
     * @param jwt JWT token
     * @return Map of all claims
     */
    public Map<String, Object> getAllClaims(Jwt jwt) {
        return jwt.getClaims();
    }

    /**
     * Get all claims from current authenticated JWT token
     * 
     * @return Map of all claims, empty map if not authenticated
     */
    public Map<String, Object> getCurrentAllClaims() {
        return getCurrentJwt()
                .map(this::getAllClaims)
                .orElse(Map.of());
    }

    /**
     * Get a specific claim from the JWT token
     * 
     * @param jwt JWT token
     * @param claimName Name of the claim to retrieve
     * @return Claim value if present, null otherwise
     */
    public Object getClaim(Jwt jwt, String claimName) {
        return jwt.getClaim(claimName);
    }

    /**
     * Get a specific claim from current authenticated JWT token
     * 
     * @param claimName Name of the claim to retrieve
     * @return Claim value if present, null otherwise
     */
    public Object getCurrentClaim(String claimName) {
        return getCurrentJwt()
                .map(jwt -> getClaim(jwt, claimName))
                .orElse(null);
    }

    /**
     * Extract username from JWT token with fallback logic.
     * Tries to extract username in the following order:
     * 1. preferred_username claim
     * 2. email claim
     * 3. subject (sub) claim
     * 
     * @param jwt JWT token
     * @return username if found, null otherwise
     */
    public String extractUsername(Jwt jwt) {
        // Try preferred_username first
        String username = getPreferredUsername(jwt);
        if (username != null && !username.isEmpty()) {
            return username;
        }
        
        // Fallback to email
        username = getEmail(jwt);
        if (username != null && !username.isEmpty()) {
            return username;
        }
        
        // Fallback to subject
        username = getSubject(jwt);
        if (username != null && !username.isEmpty()) {
            return username;
        }
        
        return null;
    }

    /**
     * Extract username from current authentication context with fallback logic.
     * For JWT authentication, tries: preferred_username -> email -> subject
     * For non-JWT authentication, falls back to authentication name.
     * 
     * @return username if found, null otherwise
     */
    public String extractCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return extractUsername(jwt);
        } else {
            // Fallback to authentication name for non-JWT authentication
            return authentication != null ? authentication.getName() : null;
        }
    }

    /**
     * Get current user's UUID from the database using username from token
     * 
     * @return UUID of current user if found, null otherwise
     */
    public UUID getCurrentUserId() {
        String username = extractCurrentUsername();
        if (username == null) {
            return null;
        }
        
        Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
        return adminUser.map(AdminUser::getId).orElse(null);
    }

    /**
     * Get current user's role from the database using username from token
     * 
     * @return UserRole of current user if found, null otherwise
     */
    public UserRole getCurrentUserRole() {
        String username = extractCurrentUsername();
        if (username == null) {
            return null;
        }
        
        Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
        if (adminUser.isEmpty()) {
            return null;
        }
        
        try {
            return UserRole.fromValue(adminUser.get().getRole());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get current user's username from token
     * 
     * @return username if found, null otherwise
     */
    public String getCurrentUsername() {
        return extractCurrentUsername();
    }

    public UUID getCompanyId(Jwt jwt) {
        String companyIdStr = jwt.getClaimAsString("company_id");
        return companyIdStr != null ? UUID.fromString(companyIdStr) : null;
    }

    public UUID getCurrentCompanyId() {
        return getCurrentJwt().map(this::getCompanyId).orElse(null);
    }

    /**
     * Extract employee/individual ID from JWT (for employee portal claims).
     * Reads "employee_id" or "individual_id" claim.
     *
     * @return UUID of current employee if present in token, null otherwise
     */
    public UUID getCurrentEmployeeId() {
        return getCurrentJwt().map(this::getEmployeeId).orElse(null);
    }

    private UUID getEmployeeId(Jwt jwt) {
        String idStr = jwt.getClaimAsString("user_id");
        if (idStr == null) {
            idStr = jwt.getClaimAsString("individual_id");
        }
        if (idStr == null || idStr.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public List<String> getOrganizations(Jwt jwt) {
        List<String> orgs = new ArrayList<>();
        Object claim = jwt.getClaim("organization_ids");
        if (claim == null) {
            claim = jwt.getClaim("organizations");
        }
        if (claim instanceof List) {
            ((List<?>) claim).forEach(item -> {
                if (item instanceof String) {
                    orgs.add((String) item);
                } else if (item instanceof Map) {
                    Object id = ((Map<?, ?>) item).get("id");
                    if (id instanceof String) {
                        orgs.add((String) id);
                    }
                }
            });
        } else if (claim instanceof String) {
            orgs.add((String) claim);
        }
        return orgs;
    }

    public List<String> getCurrentOrganizations() {
        return getCurrentJwt()
                .map(this::getOrganizations)
                .orElse(new ArrayList<>());
    }

    /**
     * Validates that the current user has access to the specified organization.
     * If the user has organization restrictions and the organization is not in their allowed list,
     * throws OrganizationAccessDeniedException.
     * 
     * @param organizationId The organization ID to validate access for
     * @throws OrganizationAccessDeniedException if the user is not authorized to access the organization
     */
    public void validateOrganizationAccess(UUID organizationId) {
        List<String> allowedOrganizations = getCurrentOrganizations();
        if (!allowedOrganizations.isEmpty() && !allowedOrganizations.contains(organizationId.toString())) {
            throw new OrganizationAccessDeniedException("You are not authorized to access this organization");
        }
    }
}

