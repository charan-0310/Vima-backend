package com.vimainsurance.vimaadmin.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.AuthentikPaginatedResponse;
import com.vimainsurance.vimaadmin.dto.OrganizationDto;
import com.vimainsurance.vimaadmin.dto.RoleDto;

import jakarta.ws.rs.core.Response;

/**
 * Keycloak Admin API utility. Provides getRoles() and getOrganizations() compatible
 * with the same DTOs used by AuthentikUtil, so AdminUserServiceImpl can switch
 * between Authentik and Keycloak via configuration.
 * <p>
 * Requires: keycloak.auth-server-url, keycloak.realm, keycloak.client-id, keycloak.client-secret
 * (e.g. in application-uat.properties). The client must be confidential with
 * "Service accounts roles" enabled.
 * <p>
 * To avoid 401 on getOrganizations(): In Keycloak Admin Console, go to Clients → vima-backend
 * → Service account roles tab → assign realm-management roles: <b>view-realm</b>, <b>view-groups</b>
 * (and optionally query-groups, manage-groups). For getRoles() the service account needs
 * <b>view-realm</b> (realm roles are visible with view-realm).
 */
@Component
public class KeyCloakUtil {

    private static final Logger logger = LoggerFactory.getLogger(KeyCloakUtil.class);

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ORG_PREFIX = "ORG_";
    private static final int MAX_GROUPS = 500;

    @Value("${keycloak.auth-server-url:}")
    private String authServerUrl;

    @Value("${keycloak.realm:}")
    private String realm;

    @Value("${keycloak.service.client-id:}")
    private String serviceClientId;

    @Value("${keycloak.service.client-secret:}")
    private String serviceClientSecret;

    /**
     * Builds a Keycloak admin client using client credentials (service account).
     * Caller should close the instance when done if not reusing.
     */
    public Keycloak getKeycloakClient() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(serviceClientId)
                .clientSecret(serviceClientSecret != null ? serviceClientSecret : "")
                .build();
    }

    /**
     * Get all realm roles from Keycloak, mapped to RoleDto (id, name).
     * Role names are returned with ROLE_ prefix when missing, to align with Authentik (ROLE_VIMA_ADMIN, etc.).
     */
    public List<RoleDto> getRoles() {
        List<RoleDto> roles = new ArrayList<>();
        if (!isConfigPresent()) {
            logger.warn("Keycloak config missing; returning empty roles");
            return roles;
        }
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            List<RoleRepresentation> realmRoles = keycloak.realm(realm).roles().list();
            if (realmRoles != null) {
                for (RoleRepresentation rr : realmRoles) {
                    if (rr == null) continue;
                    String name = rr.getName();
                    if (name == null || name.isBlank()) continue;
                    RoleDto dto = new RoleDto();
                    dto.setId(rr.getId() != null ? rr.getId() : name);
                    dto.setName(name.startsWith(ROLE_PREFIX) ? name : ROLE_PREFIX + name);
                    roles.add(dto);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get roles from Keycloak", e);
            throw new RuntimeException("Failed to get roles from Keycloak: " + e.getMessage(), e);
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
        return roles;
    }

    /**
     * Get all organization groups from Keycloak (groups whose name starts with ORG_),
     * mapped to OrganizationDto (id, name). Matches Authentik behavior where orgs are ORG_* groups.
     */
    public List<OrganizationDto> getOrganizations() {
        List<OrganizationDto> organizations = new ArrayList<>();
        if (!isConfigPresent()) {
            logger.warn("Keycloak config missing; returning empty organizations");
            return organizations;
        }
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            List<GroupRepresentation> groups = keycloak.realm(realm).groups().groups(0, MAX_GROUPS);
            if (groups != null) {
                List<OrganizationDto> fromTopLevel = groups.stream()
                        .filter(g -> g != null && g.getName() != null && g.getName().startsWith(ORG_PREFIX))
                        .map(this::toOrganizationDto)
                        .filter(dto -> dto != null)
                        .collect(Collectors.toList());
                organizations.addAll(fromTopLevel);
            }
        } catch (Exception e) {
            logger.error("Failed to get organizations from Keycloak. If 401 Unauthorized, assign realm-management roles (view-groups, view-realm) to the client's Service account.", e);
            String hint = (e.getMessage() != null && e.getMessage().contains("401"))
                    ? " Assign Service account roles: Clients → vima-backend → Service account roles → realm-management → view-groups, view-realm."
                    : "";
            throw new RuntimeException("Failed to get organizations from Keycloak: " + e.getMessage() + hint, e);
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
        return organizations;
    }

    /**
     * Create a top-level group in Keycloak with the given name.
     * Used when creating an organization so that HR_ADMIN etc. can be assigned to ORG_* groups.
     *
     * @param groupName Group name (e.g. "ORG_ACME_CORP")
     * @return Created group ID, or null if config missing or creation failed
     */
    public String createGroup(String groupName) {
        return createGroup(groupName, null);
    }

    /**
     * Create a top-level group in Keycloak with the given name and optional attributes.
     *
     * @param groupName  Group name (e.g. "ORG_ACME_CORP")
     * @param attributes Optional attributes (e.g. Map.of("organization_id", List.of(uuid)))
     * @return Created group ID, or null if config missing or creation failed
     */
    public String createGroup(String groupName, Map<String, List<String>> attributes) {
        if (groupName == null || groupName.isBlank()) {
            logger.warn("createGroup: groupName is blank");
            return null;
        }
        if (!isConfigPresent()) {
            logger.warn("Keycloak config missing; cannot create group");
            return null;
        }
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            GroupRepresentation group = new GroupRepresentation();
            group.setName(groupName.trim());
            if (attributes != null && !attributes.isEmpty()) {
                group.setAttributes(attributes);
            }
            try (Response response = keycloak.realm(realm).groups().add(group)) {
                if (response.getStatus() == 201) {
                    String createdId = CreatedResponseUtil.getCreatedId(response);
                    logger.info("Created Keycloak group {} with id {}", groupName, createdId);
                    return createdId;
                }
                if (response.getStatus() == 409) {
                    logger.debug("Keycloak group {} already exists", groupName);
                    return getGroupIdByName(groupName);
                }
                String body = response.readEntity(String.class);
                logger.warn("Keycloak createGroup failed: status={}, body={}", response.getStatus(), body);
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to create group in Keycloak: {}", e.getMessage(), e);
            return null;
        } finally {
            if (keycloak != null) keycloak.close();
        }
    }

    /**
     * Get group ID by group name (e.g. "ORG_MAIN", "ROLE_VIMA_ADMIN").
     * Used to resolve role/org names to Keycloak group IDs when creating users.
     * For realm roles we use realm.roles() by name; this is for groups only.
     */
    public String getGroupIdByName(String groupName) {
        if (groupName == null || groupName.isBlank()) return null;
        if (!isConfigPresent()) return null;
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            List<GroupRepresentation> groups = keycloak.realm(realm).groups().groups(0, MAX_GROUPS);
            if (groups != null) {
                for (GroupRepresentation g : groups) {
                    if (g != null && groupName.equals(g.getName())) return g.getId();
                }
            }
            return null;
        } catch (Exception e) {
            logger.warn("Failed to get group id by name from Keycloak: {}", e.getMessage());
            return null;
        } finally {
            if (keycloak != null) keycloak.close();
        }
    }

    /**
     * Get users from Keycloak with pagination (1-based page).
     * Returns same structure as AuthentikUtil.getUsers for drop-in use.
     */
    public AuthentikPaginatedResponse<AdminUserResponseDto> getUsers(Integer page, Integer pageSize) {
        return getUsersWithFilters(null, null, null, null, page, pageSize);
    }

    /**
     * Get users from Keycloak with search, filters, and pagination.
     * Keycloak supports: first (offset), max (page size), search (username/first/last/email), enabled.
     * ordering and groupsByName are not applied by Keycloak API (ordering could be done in-memory; group filter would require extra calls).
     *
     * @param search       Search term (Keycloak searches username, firstName, lastName, email)
     * @param isActive     Filter by enabled status
     * @param ordering     Ignored by Keycloak API
     * @param groupsByName Ignored in this implementation
     * @param page         1-based page number
     * @param pageSize     Page size
     */
    public AuthentikPaginatedResponse<AdminUserResponseDto> getUsersWithFilters(
            String search, Boolean isActive, String ordering, List<String> groupsByName,
            Integer page, Integer pageSize) {
        if (!isConfigPresent()) {
            throw new IllegalStateException("Keycloak config missing; cannot get users");
        }
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 25;
        int first = (page - 1) * pageSize;
        int max = pageSize;

        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            var usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> userList;
            int totalCount;
            if (search != null && !search.trim().isEmpty()) {
                userList = usersResource.search(search.trim(), isActive, first, max);
                totalCount = usersResource.count(search.trim(), null, null, null, null, null, isActive, null);
            } else {
                userList = usersResource.list(first, max);
                totalCount = usersResource.count();
            }
            if (userList == null) userList = List.of();

            List<AdminUserResponseDto> results = new ArrayList<>();
            for (UserRepresentation ur : userList) {
                AdminUserResponseDto dto = mapUserRepresentationToDto(keycloak, ur);
                if (dto != null) results.add(dto);
            }

            int totalPages = (totalCount + pageSize - 1) / pageSize;
            AuthentikPaginatedResponse<AdminUserResponseDto> response = new AuthentikPaginatedResponse<>();
            response.setResults(results);
            AuthentikPaginatedResponse.PaginationInfo pagination = new AuthentikPaginatedResponse.PaginationInfo();
            pagination.setCount(totalCount);
            pagination.setCurrent(page);
            pagination.setTotalPages(totalPages);
            pagination.setNext(page < totalPages ? page + 1 : null);
            pagination.setPrevious(page > 1 ? page - 1 : null);
            pagination.setStartIndex(first + 1);
            pagination.setEndIndex(Math.min(first + results.size(), totalCount));
            response.setPagination(pagination);
            return response;
        } catch (Exception e) {
            logger.error("Failed to get users from Keycloak", e);
            throw new RuntimeException("Failed to get users from Keycloak: " + e.getMessage(), e);
        } finally {
            if (keycloak != null) keycloak.close();
        }
    }

    /**
     * Get all users from Keycloak by fetching all pages (same signature as AuthentikUtil.getAllUsers).
     *
     * @return List of all users
     */
    public List<AdminUserResponseDto> getAllUsers() {
        List<AdminUserResponseDto> allUsers = new ArrayList<>();
        int currentPage = 1;
        int pageSize = 100;

        while (true) {
            AuthentikPaginatedResponse<AdminUserResponseDto> response = getUsers(currentPage, pageSize);
            if (response.getResults() == null || response.getResults().isEmpty()) {
                break;
            }
            allUsers.addAll(response.getResults());
            if (response.getPagination() != null && response.getPagination().getNext() != null && response.getPagination().getNext() > 0) {
                currentPage = response.getPagination().getNext();
            } else {
                break;
            }
        }
        return allUsers;
    }

    /**
     * Map Keycloak UserRepresentation to AdminUserResponseDto. Optionally enriches with realm roles and group names.
     */
    private AdminUserResponseDto mapUserRepresentationToDto(Keycloak keycloak, UserRepresentation ur) {
        if (ur == null) return null;
        AdminUserResponseDto dto = new AdminUserResponseDto();
        dto.setUsername(ur.getUsername());
        dto.setEmail(ur.getEmail());
        String first = ur.getFirstName() != null ? ur.getFirstName() : "";
        String last = ur.getLastName() != null ? ur.getLastName() : "";
        dto.setFullName((first + " " + last).trim().isEmpty() ? null : (first + " " + last).trim());
        dto.setIsActive(ur.isEnabled());
        dto.setOauthProvider("keycloak");
        dto.setOauthProviderId(ur.getId());
        if (ur.getCreatedTimestamp() != null) {
            dto.setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(ur.getCreatedTimestamp()), ZoneId.systemDefault()));
        }
        dto.setRoles(new ArrayList<>());
        dto.setOrganizations(new ArrayList<>());
        if (keycloak != null && ur.getId() != null) {
            try {
                UserResource userResource = keycloak.realm(realm).users().get(ur.getId());
                List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listEffective();
                if (realmRoles != null) {
                    for (RoleRepresentation rr : realmRoles) {
                        if (rr != null && rr.getName() != null) {
                            dto.getRoles().add(rr.getName().startsWith(ROLE_PREFIX) ? rr.getName() : ROLE_PREFIX + rr.getName());
                        }
                    }
                }
                List<GroupRepresentation> groups = userResource.groups();
                if (groups != null) {
                    for (GroupRepresentation g : groups) {
                        if (g != null && g.getName() != null && g.getName().startsWith(ORG_PREFIX)) {
                            dto.getOrganizations().add(g.getName());
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not enrich user {} with roles/groups: {}", ur.getUsername(), e.getMessage());
            }
        }
        return dto;
    }

    /**
     * Create user in Keycloak with optional temporary password (same signature as AuthentikUtil).
     * Assigns realm role by name and adds user to ORG_* groups. Sets password and optional UPDATE_PASSWORD required action.
     *
     * @return The temporary password that was set (either provided or auto-generated)
     */
    public String createUser(String name, String username, String email, String role, List<String> organizations, Boolean isActive) {
        return createUser(name, username, email, role, organizations, isActive, null, null);
    }

    /**
     * Create user in Keycloak with optional temporary password and optional user_id attribute.
     *
     * @param individualId Optional internal user id (e.g. admin_users.id) to set as user attribute "user_id"
     * @return The temporary password that was set (either provided or auto-generated)
     */
    public String createUser(String name, String username, String email, String role, List<String> organizations, Boolean isActive, String temporaryPassword, String individualId) {
        if (!isConfigPresent()) {
            throw new RuntimeException("Keycloak config missing; cannot create user");
        }
        // Keycloak requires a non-blank username; if "Email as username" is enabled, username often equals email
        String u = username != null ? username.trim() : "";
        String e = email != null ? email.trim() : "";
        if (u.isBlank() && e.isBlank()) {
            throw new RuntimeException("Keycloak user creation requires username or email");
        }
        if (u.isBlank()) u = e;
        if (e.isBlank()) e = u;

        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            var realmResource = keycloak.realm(realm);

            UserRepresentation user = new UserRepresentation();
            user.setUsername(u);
            user.setEmail(e);
            user.setEnabled(isActive != null ? isActive : true);
            if (name != null && !name.isBlank()) {
                user.setFirstName(name.trim());
                user.setLastName("");
            }
            if (individualId != null && !individualId.isBlank()) {
                user.setAttributes(Map.of("user_id", List.of(individualId.trim())));
            }
            // Require user to update password on first login only
            user.setRequiredActions(List.of("UPDATE_PASSWORD"));

            // Create password and attach to user so user is created with password in one request
            String passwordToSet = (temporaryPassword != null && !temporaryPassword.isBlank())
                    ? temporaryPassword
                    : PasswordGenerator.generateRandomPassword(12);
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(passwordToSet);
            cred.setTemporary(true);
            user.setCredentials(Collections.singletonList(cred));

            try (Response response = realmResource.users().create(user)) {
                int status = response.getStatus();
                String body = response.readEntity(String.class);
                if (status == 400) {
                    logger.error("Keycloak create user 400: {}", body);
                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request",
                            body != null ? body.getBytes(StandardCharsets.UTF_8) : null, StandardCharsets.UTF_8);
                }
                if (status == 409) {
                    logger.error("Keycloak create user 409: {}", body);
                    throw new HttpClientErrorException(HttpStatus.CONFLICT, "Conflict",
                            body != null ? body.getBytes(StandardCharsets.UTF_8) : null, StandardCharsets.UTF_8);
                }
                if (status != 201) {
                    throw new RuntimeException("Keycloak create user failed: status=" + status + ", body=" + body);
                }
                String userId = CreatedResponseUtil.getCreatedId(response);
                if (userId == null || userId.isBlank()) {
                    throw new RuntimeException("Failed to get created user id from Keycloak");
                }

                // Override required actions after create (realm default actions may have been applied)
                UserResource userResource = realmResource.users().get(userId);
                UserRepresentation createdUser = userResource.toRepresentation();
                createdUser.setRequiredActions(List.of("UPDATE_PASSWORD"));
                userResource.update(createdUser);

                // Assign realm role (Keycloak realm role name: VIMA_ADMIN; we accept ROLE_VIMA_ADMIN or VIMA_ADMIN)
                if (role != null && !role.trim().isEmpty()) {
                    String roleName = role.trim();
                    try {
                        RoleRepresentation realmRole = realmResource.roles().get(roleName).toRepresentation();
                        if (realmRole != null) {
                            realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(realmRole));
                        }
                    } catch (Exception ex) {
                        logger.warn("Could not assign realm role {} to user {}: {}", roleName, username, ex.getMessage());
                    }
                }

                // Assign organization groups (by name ORG_*)
                if (organizations != null && !organizations.isEmpty()) {
                    for (String org : organizations) {
                        if (org == null || org.isBlank()) continue;
                        String orgName = org.trim();
                        if (!orgName.startsWith(ORG_PREFIX)) orgName = ORG_PREFIX + orgName;
                        String groupId = getGroupIdByName(keycloak, orgName);
                        if (groupId != null) {
                            try {
                                realmResource.users().get(userId).joinGroup(groupId);
                            } catch (Exception ex) {
                                logger.warn("Could not add user {} to group {}: {}", username, orgName, ex.getMessage());
                            }
                        }
                    }
                }

                return passwordToSet;
            }
        } catch (jakarta.ws.rs.BadRequestException ex) {
            String hint = " (Common causes: duplicate username or email, or realm requires email as username.)";
            try {
                String body = ex.getResponse().readEntity(String.class);
                logger.error("Keycloak create user 400 Bad Request: {}", body);
                throw new RuntimeException("Keycloak user creation failed: " + body + hint, ex);
            } catch (Exception readEx) {
                throw new RuntimeException("Keycloak user creation failed: 400 Bad Request." + hint + " " + ex.getMessage(), ex);
            }
        } catch (Exception ex) {
            logger.error("Failed to create user in Keycloak", ex);
            throw new RuntimeException("Failed to create user in Keycloak: " + ex.getMessage(), ex);
        } finally {
            if (keycloak != null) keycloak.close();
        }
    }

    /** Resolve group id by name using an existing Keycloak instance (avoids creating a new client per call). */
    private String getGroupIdByName(Keycloak keycloak, String groupName) {
        if (groupName == null || groupName.isBlank() || keycloak == null) return null;
        try {
            List<GroupRepresentation> groups = keycloak.realm(realm).groups().groups(0, MAX_GROUPS);
            if (groups != null) {
                for (GroupRepresentation g : groups) {
                    if (g != null && groupName.equals(g.getName())) return g.getId();
                }
            }
        } catch (Exception ex) {
            logger.warn("Failed to get group id by name: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Set password for a Keycloak user (by user id).
     */
    public void setUserPassword(String userId, String password) {
        if (!isConfigPresent()) {
            throw new RuntimeException("Keycloak config missing; cannot set password");
        }
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(password);
            cred.setTemporary(false);
            keycloak.realm(realm).users().get(userId).resetPassword(cred);
        } catch (Exception ex) {
            logger.error("Failed to set password in Keycloak for user {}", userId, ex);
            throw new RuntimeException("Failed to set password in Keycloak: " + ex.getMessage(), ex);
        } finally {
            if (keycloak != null) keycloak.close();
        }
    }

    private OrganizationDto toOrganizationDto(GroupRepresentation g) {
        if (g == null) return null;
        OrganizationDto dto = new OrganizationDto();
        dto.setId(g.getId());
        dto.setName(g.getName() != null ? g.getName() : "");
        return dto;
    }

    private boolean isConfigPresent() {
        return authServerUrl != null && !authServerUrl.isBlank()
                && realm != null && !realm.isBlank()
                && serviceClientId != null && !serviceClientId.isBlank();
    }
}
