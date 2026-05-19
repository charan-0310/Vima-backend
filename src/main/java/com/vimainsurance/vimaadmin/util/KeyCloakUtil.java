package com.vimainsurance.vimaadmin.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.security.RedirectTargetValidator;
import com.vimainsurance.vimaadmin.dto.AuthentikPaginatedResponse;
import com.vimainsurance.vimaadmin.dto.OrganizationDto;
import com.vimainsurance.vimaadmin.dto.RoleDto;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.NotFoundException;

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
 * For employee login preview (events + sessions): add <b>query-users</b>, <b>view-users</b>,
 * and <b>query-events</b> (or view-events) so {@link #getLastLoginForEmails} can read LOGIN events and user sessions.
 */
@Component
public class KeyCloakUtil {

    @Autowired
    private RedirectTargetValidator redirectTargetValidator;

    private static final Logger logger = LoggerFactory.getLogger(KeyCloakUtil.class);

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ORG_PREFIX = "ORG_";

    /** Realm roles that are Keycloak internals — never show in Vima user-management UI. */
    private static final List<String> APPLICATION_ROLE_PRIORITY = List.of(
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_VIMA_ADMIN", "ROLE_SALES_MANAGER", "ROLE_SALES_ADMIN",
            "ROLE_HR_ADMIN", "ROLE_SALES_AGENT", "ROLE_CLAIMS_PROCESSOR", "ROLE_SUPPORT_AGENT",
            "ROLE_SALES_POSP", "ROLE_EMPLOYEE");

    /**
     * Keycloak assigns every user {@code default-roles-<realm>}, plus default client roles.
     * These are not application roles and should not appear in the admin portal.
     */
    public static boolean isKeycloakInternalRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return true;
        }
        String lower = roleName.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("default-roles")) {
            return true;
        }
        return "uma_authorization".equals(lower) || "offline_access".equals(lower);
    }

    public static String normalizeApplicationRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return roleName;
        }
        String trimmed = roleName.trim();
        return trimmed.startsWith(ROLE_PREFIX) ? trimmed : ROLE_PREFIX + trimmed;
    }

    public static List<String> filterApplicationRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(KeyCloakUtil::normalizeApplicationRoleName)
                .filter(r -> !isKeycloakInternalRole(r))
                .distinct()
                .collect(Collectors.toList());
    }

    public static String resolvePrimaryApplicationRole(List<String> applicationRoles) {
        if (applicationRoles == null || applicationRoles.isEmpty()) {
            return null;
        }
        for (String priority : APPLICATION_ROLE_PRIORITY) {
            if (applicationRoles.contains(priority)) {
                return priority;
            }
        }
        return applicationRoles.get(0);
    }
    private static final int MAX_GROUPS = 500;
    /** Max distinct emails per preview call (guardrail for Keycloak Admin API load). */
    private static final int EMAIL_PREVIEW_MAX_UNIQUE = 25_000;
    /** Parallel workers for batched email existence checks (each uses its own short-lived Keycloak client). */
    private static final int EMAIL_PREVIEW_PARALLEL_WORKERS = 12;

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
                    String normalized = normalizeApplicationRoleName(name);
                    if (isKeycloakInternalRole(normalized)) continue;
                    RoleDto dto = new RoleDto();
                    dto.setId(rr.getId() != null ? rr.getId() : name);
                    dto.setName(normalized);
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
                    List<String> rawRoles = new ArrayList<>();
                    for (RoleRepresentation rr : realmRoles) {
                        if (rr != null && rr.getName() != null) {
                            rawRoles.add(normalizeApplicationRoleName(rr.getName()));
                        }
                    }
                    List<String> applicationRoles = filterApplicationRoles(rawRoles);
                    dto.getRoles().addAll(applicationRoles);
                    dto.setRole(resolvePrimaryApplicationRole(applicationRoles));
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
        String u = username != null ? username.trim() : "";
        String e = email != null ? email.trim() : "";
        if (u.isBlank() && e.isBlank()) {
            throw new RuntimeException("Keycloak user creation requires username or email");
        }
        if (u.isBlank()) u = e;
        if (e.isBlank()) e = u;

        try (Keycloak keycloak = getKeycloakClient()) {
            Map<String, String> groupNameToId = loadTopLevelGroupNameToIdMap(keycloak);
            return createUserInternal(keycloak, groupNameToId, null, name, u, e, role, organizations, isActive, temporaryPassword, individualId);
        } catch (jakarta.ws.rs.BadRequestException ex) {
            String hint = " (Common causes: duplicate username or email, or realm requires email as username.)";
            try {
                String body = ex.getResponse().readEntity(String.class);
                logger.error("Keycloak create user 400 Bad Request: {}", body);
                throw new RuntimeException("Keycloak user creation failed: " + body + hint, ex);
            } catch (Exception readEx) {
                throw new RuntimeException("Keycloak user creation failed: 400 Bad Request." + hint + " " + ex.getMessage(), ex);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Failed to create user in Keycloak", ex);
            throw new RuntimeException("Failed to create user in Keycloak: " + ex.getMessage(), ex);
        }
    }

    /**
     * Same as {@link #createUser(String, String, String, String, List, Boolean, String, String)} but reuses an open
     * Admin client and preloaded top-level group map / realm role (from {@link #loadTopLevelGroupNameToIdMap} /
     * {@link #getRealmRoleOrNull}) for bulk onboarding.
     */
    public String createUser(Keycloak keycloak, Map<String, String> groupNameToId, RoleRepresentation prefetchedRealmRole,
            String name, String username, String email, String role, List<String> organizations, Boolean isActive,
            String temporaryPassword, String individualId) {
        if (!isConfigPresent()) {
            throw new RuntimeException("Keycloak config missing; cannot create user");
        }
        String u = username != null ? username.trim() : "";
        String e = email != null ? email.trim() : "";
        if (u.isBlank() && e.isBlank()) {
            throw new RuntimeException("Keycloak user creation requires username or email");
        }
        if (u.isBlank()) u = e;
        if (e.isBlank()) e = u;
        try {
            return createUserInternal(keycloak, groupNameToId, prefetchedRealmRole, name, u, e, role, organizations, isActive, temporaryPassword, individualId);
        } catch (jakarta.ws.rs.BadRequestException ex) {
            String hint = " (Common causes: duplicate username or email, or realm requires email as username.)";
            try {
                String body = ex.getResponse().readEntity(String.class);
                logger.error("Keycloak create user 400 Bad Request: {}", body);
                throw new RuntimeException("Keycloak user creation failed: " + body + hint, ex);
            } catch (Exception readEx) {
                throw new RuntimeException("Keycloak user creation failed: 400 Bad Request." + hint + " " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * One fetch of top-level groups (up to {@link #MAX_GROUPS}) for the realm — reuse across many user creates/joins in a batch.
     */
    public Map<String, String> loadTopLevelGroupNameToIdMap(Keycloak keycloak) {
        Map<String, String> map = new HashMap<>();
        if (keycloak == null) {
            return map;
        }
        try {
            List<GroupRepresentation> groups = keycloak.realm(realm).groups().groups(0, MAX_GROUPS);
            if (groups != null) {
                for (GroupRepresentation g : groups) {
                    if (g != null && g.getName() != null && g.getId() != null) {
                        map.put(g.getName(), g.getId());
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Failed to list Keycloak groups for batch map: {}", ex.getMessage());
        }
        return map;
    }

    /**
     * Realm role by exact name, or null if missing.
     */
    public RoleRepresentation getRealmRoleOrNull(Keycloak keycloak, String roleName) {
        if (keycloak == null || roleName == null || roleName.isBlank()) {
            return null;
        }
        try {
            return keycloak.realm(realm).roles().get(roleName.trim()).toRepresentation();
        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            logger.warn("Could not load realm role {}: {}", roleName, e.getMessage());
            return null;
        }
    }

    /**
     * Keycloak realm roles are stored with a {@code ROLE_} prefix (e.g. {@code ROLE_HR_ADMIN}).
     */
    public static String normalizeRealmRoleName(String role) {
        if (role == null || role.isBlank()) {
            return role;
        }
        String trimmed = role.trim();
        return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
    }

    private String createUserInternal(Keycloak keycloak, Map<String, String> groupNameToId,
            RoleRepresentation prefetchedRealmRole,
            String name, String u, String e, String role, List<String> organizations, Boolean isActive,
            String temporaryPassword, String individualId) {
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
        user.setRequiredActions(List.of("UPDATE_PASSWORD"));

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

            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation createdUser = userResource.toRepresentation();
            createdUser.setRequiredActions(List.of("UPDATE_PASSWORD"));
            userResource.update(createdUser);

            if (role != null && !role.trim().isEmpty()) {
                String roleName = normalizeRealmRoleName(role);
                RoleRepresentation realmRoleToAssign = prefetchedRealmRole;
                if (realmRoleToAssign == null || !roleName.equals(realmRoleToAssign.getName())) {
                    try {
                        realmRoleToAssign = realmResource.roles().get(roleName).toRepresentation();
                    } catch (Exception ex) {
                        realmRoleToAssign = null;
                    }
                }
                if (realmRoleToAssign != null) {
                    try {
                        realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(realmRoleToAssign));
                    } catch (Exception ex) {
                        logger.warn("Could not assign realm role {} to user {}: {}", roleName, u, ex.getMessage());
                    }
                }
            }

            if (organizations != null && !organizations.isEmpty() && groupNameToId != null) {
                for (String org : organizations) {
                    if (org == null || org.isBlank()) continue;
                    String orgName = org.trim();
                    if (!orgName.startsWith(ORG_PREFIX)) orgName = ORG_PREFIX + orgName;
                    String groupId = groupNameToId.get(orgName);
                    if (groupId != null) {
                        try {
                            realmResource.users().get(userId).joinGroup(groupId);
                        } catch (Exception ex) {
                            logger.warn("Could not add user {} to group {}: {}", u, orgName, ex.getMessage());
                        }
                    }
                }
            }

            return passwordToSet;
        }
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

    /**
     * Returns whether a Keycloak user exists with this exact email (case-insensitive match on email field).
     * Uses the Admin API search with exact=true. Intended for login preview and idempotency checks.
     */
    public boolean emailExistsInRealm(String email) {
        if (email == null || email.isBlank() || !isConfigPresent()) {
            return false;
        }
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            return emailExistsInRealm(keycloak, email.trim());
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
    }

    /**
     * Batch lookup for many distinct normalized emails. Splits work across parallel workers
     * (each worker holds one Keycloak client and processes a slice) to handle large lists
     * without serializing thousands of Admin API calls on one connection.
     *
     * @param normalizedUniqueEmails lowercased/trimmed emails, no duplicates
     * @return map email -> exists in realm
     */
    public Map<String, Boolean> checkEmailsExistInRealmBatched(List<String> normalizedUniqueEmails) {
        if (!isConfigPresent()) {
            throw new IllegalStateException("Keycloak config missing; cannot check emails");
        }
        if (normalizedUniqueEmails == null || normalizedUniqueEmails.isEmpty()) {
            return Map.of();
        }
        if (normalizedUniqueEmails.size() > EMAIL_PREVIEW_MAX_UNIQUE) {
            throw new IllegalArgumentException(
                    "Too many distinct emails (" + normalizedUniqueEmails.size() + "); max " + EMAIL_PREVIEW_MAX_UNIQUE);
        }
        Map<String, Boolean> result = new ConcurrentHashMap<>();
        int n = normalizedUniqueEmails.size();
        int workers = Math.min(EMAIL_PREVIEW_PARALLEL_WORKERS, Math.max(1, (n + 199) / 200));
        List<List<String>> slices = splitIntoSlices(normalizedUniqueEmails, workers);
        ExecutorService pool = Executors.newFixedThreadPool(slices.size());
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (List<String> slice : slices) {
                futures.add(pool.submit(() -> {
                    try (Keycloak kc = getKeycloakClient()) {
                        for (String email : slice) {
                            if (email == null || email.isBlank()) {
                                continue;
                            }
                            result.put(email.trim(), emailExistsInRealm(kc, email.trim()));
                        }
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email preview interrupted", e);
        } catch (ExecutionException e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("Keycloak email batch check failed: " + c.getMessage(), c);
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(30, TimeUnit.MINUTES)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        return result;
    }

    private static List<List<String>> splitIntoSlices(List<String> list, int numSlices) {
        if (list.isEmpty()) {
            return List.of();
        }
        int slices = Math.min(numSlices, list.size());
        List<List<String>> out = new ArrayList<>(slices);
        int chunk = (list.size() + slices - 1) / slices;
        for (int i = 0; i < list.size(); i += chunk) {
            out.add(list.subList(i, Math.min(i + chunk, list.size())));
        }
        return out;
    }

    private boolean emailExistsInRealm(Keycloak keycloak, String email) {
        if (email == null || email.isBlank() || keycloak == null) {
            return false;
        }
        try {
            List<UserRepresentation> users = keycloak.realm(realm).users().search(email.trim(), true, 0, 20);
            if (users == null || users.isEmpty()) {
                return false;
            }
            return users.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(u -> u.getEmail() != null
                            && email.trim().equalsIgnoreCase(u.getEmail().trim()));
        } catch (Exception e) {
            logger.warn("Keycloak user search failed for {}: {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Adds a realm role to an existing Keycloak user identified by email.
     * Also ensures user joins the provided organization groups when supplied.
     *
     * @return true when user exists and role assignment flow completes; false when user not found/config missing.
     */
    public boolean addRoleToExistingUserByEmail(String email, String role, List<String> organizations) {
        if (email == null || email.isBlank()) return false;
        if (!isConfigPresent()) {
            logger.warn("Keycloak config missing; cannot add role to existing user");
            return false;
        }
        try (Keycloak keycloak = getKeycloakClient()) {
            Map<String, String> map = loadTopLevelGroupNameToIdMap(keycloak);
            return addRoleToExistingUserInternal(keycloak, map, null, email, role, organizations);
        } catch (Exception e) {
            logger.error("Failed to add role {} to existing user {} in Keycloak", role, email, e);
            return false;
        }
    }

    /**
     * Bulk-friendly overload: reuses an open Admin client and preloaded group map / realm role.
     */
    public boolean addRoleToExistingUserByEmail(Keycloak keycloak, Map<String, String> groupNameToId,
            RoleRepresentation prefetchedRealmRole, String email, String role, List<String> organizations) {
        if (email == null || email.isBlank()) return false;
        if (!isConfigPresent()) {
            logger.warn("Keycloak config missing; cannot add role to existing user");
            return false;
        }
        try {
            return addRoleToExistingUserInternal(keycloak, groupNameToId, prefetchedRealmRole, email, role, organizations);
        } catch (Exception e) {
            logger.error("Failed to add role {} to existing user {} in Keycloak", role, email, e);
            return false;
        }
    }

    private boolean addRoleToExistingUserInternal(Keycloak keycloak, Map<String, String> groupNameToId,
            RoleRepresentation prefetchedRealmRole, String email, String role, List<String> organizations) {
        var realmResource = keycloak.realm(realm);
        List<UserRepresentation> users = realmResource.users().search(email.trim(), true, 0, 20);
        if (users == null || users.isEmpty()) return false;

        List<UserRepresentation> emailMatches = users.stream()
                .filter(u -> u != null && u.getEmail() != null && email.trim().equalsIgnoreCase(u.getEmail().trim()))
                .collect(Collectors.toList());
        if (emailMatches.size() > 1) {
            logger.warn(
                    "Keycloak: multiple users share email {}; using id={} for role {}. Clean up duplicates in realm.",
                    email,
                    emailMatches.get(0).getId(),
                    role);
        }
        UserRepresentation existing = emailMatches.isEmpty() ? null : emailMatches.get(0);
        if (existing == null || existing.getId() == null || existing.getId().isBlank()) return false;

        UserResource userResource = realmResource.users().get(existing.getId());
        boolean roleAssignmentOk = true;
        String roleName = role != null ? normalizeRealmRoleName(role) : "";
        if (!roleName.isEmpty()) {
            List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
            boolean alreadyHasRole = currentRoles != null && currentRoles.stream()
                    .filter(r -> r != null && r.getName() != null)
                    .anyMatch(r -> r.getName().equals(roleName));
            if (!alreadyHasRole) {
                RoleRepresentation realmRole = prefetchedRealmRole;
                if (realmRole == null || !roleName.equals(realmRole.getName())) {
                    try {
                        realmRole = realmResource.roles().get(roleName).toRepresentation();
                    } catch (Exception ex) {
                        realmRole = null;
                    }
                }
                if (realmRole != null) {
                    userResource.roles().realmLevel().add(Collections.singletonList(realmRole));
                } else {
                    logger.warn("Role {} not found in realm {} while onboarding {}", roleName, realm, email);
                    roleAssignmentOk = false;
                }
            }
        }

        boolean orgAssignmentOk = true;
        if (organizations != null && !organizations.isEmpty() && groupNameToId != null) {
            for (String org : organizations) {
                if (org == null || org.isBlank()) continue;
                String orgName = org.trim();
                if (!orgName.startsWith(ORG_PREFIX)) orgName = ORG_PREFIX + orgName;
                String groupId = groupNameToId.get(orgName);
                if (groupId == null || groupId.isBlank()) {
                    logger.warn("Organization group {} not found in realm {} for {}", orgName, realm, email);
                    orgAssignmentOk = false;
                    continue;
                }
                try {
                    userResource.joinGroup(groupId);
                } catch (NotFoundException ignored) {
                    logger.warn("Organization group {} disappeared before join for {}", orgName, email);
                    orgAssignmentOk = false;
                }
            }
        }
        return roleAssignmentOk && orgAssignmentOk;
    }

    /**
     * Removes a realm role from an existing Keycloak user identified by email.
     *
     * @return true when user exists and role removal completes (or role was not assigned); false when user not found.
     */
    public boolean removeRoleFromExistingUserByEmail(String email, String role) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if (!isConfigPresent()) {
            logger.warn("Keycloak config missing; cannot remove role from existing user");
            return false;
        }
        try (Keycloak keycloak = getKeycloakClient()) {
            return removeRoleFromExistingUserInternal(keycloak, email, role);
        } catch (Exception e) {
            logger.error("Failed to remove role {} from existing user {} in Keycloak", role, email, e);
            return false;
        }
    }

    private boolean removeRoleFromExistingUserInternal(Keycloak keycloak, String email, String role) {
        var realmResource = keycloak.realm(realm);
        List<UserRepresentation> users = realmResource.users().search(email.trim(), true, 0, 20);
        if (users == null || users.isEmpty()) {
            return false;
        }
        List<UserRepresentation> emailMatches = users.stream()
                .filter(u -> u != null && u.getEmail() != null && email.trim().equalsIgnoreCase(u.getEmail().trim()))
                .collect(Collectors.toList());
        if (emailMatches.isEmpty() || emailMatches.get(0).getId() == null) {
            return false;
        }
        UserResource userResource = realmResource.users().get(emailMatches.get(0).getId());
        String roleName = role != null ? normalizeRealmRoleName(role) : "";
        if (roleName.isEmpty()) {
            return true;
        }
        List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
        if (currentRoles == null || currentRoles.stream()
                .noneMatch(r -> r != null && roleName.equals(r.getName()))) {
            return true;
        }
        try {
            RoleRepresentation realmRole = realmResource.roles().get(roleName).toRepresentation();
            userResource.roles().realmLevel().remove(Collections.singletonList(realmRole));
            return true;
        } catch (Exception ex) {
            logger.warn("Role {} not found or could not be removed for {} in realm {}", roleName, email, realm);
            return false;
        }
    }

    public void setUserAttribute(String email, String attributeName, String attributeValue) {
        if (!isConfigPresent() || email == null || email.isBlank() || attributeName == null || attributeName.isBlank()) {
            return;
        }
        try (Keycloak keycloak = getKeycloakClient()) {
            UserRepresentation user = findUserByEmail(keycloak, email);
            if (user == null || user.getId() == null) {
                logger.warn("setUserAttribute: user not found for email {}", email);
                return;
            }
            UserResource userResource = keycloak.realm(realm).users().get(user.getId());
            UserRepresentation fullUser = userResource.toRepresentation();
            Map<String, List<String>> attrs = fullUser.getAttributes() != null
                    ? new HashMap<>(fullUser.getAttributes())
                    : new HashMap<>();
            attrs.put(attributeName, List.of(attributeValue != null ? attributeValue : ""));
            fullUser.setAttributes(attrs);
            userResource.update(fullUser);
        } catch (Exception e) {
            logger.error("Failed to set attribute {} for user {}: {}", attributeName, email, e.getMessage(), e);
        }
    }

    public void disableUserByEmail(String email) {
        updateUserEnabledState(email, false);
    }

    public void enableUserByEmail(String email) {
        updateUserEnabledState(email, true);
    }

    public boolean isDemoAccountUserByEmail(String email) {
        if (!isConfigPresent() || email == null || email.isBlank()) {
            return false;
        }
        try (Keycloak keycloak = getKeycloakClient()) {
            UserRepresentation user = findUserByEmail(keycloak, email);
            if (user == null || user.getId() == null) {
                return false;
            }
            UserRepresentation fullUser = keycloak.realm(realm).users().get(user.getId()).toRepresentation();
            Map<String, List<String>> attributes = fullUser.getAttributes();
            if (attributes == null || attributes.isEmpty()) {
                return false;
            }
            List<String> accountTypes = attributes.get("account_type");
            if (accountTypes == null || accountTypes.isEmpty()) {
                return false;
            }
            return accountTypes.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .anyMatch(v -> "demo".equalsIgnoreCase(v));
        } catch (Exception e) {
            logger.warn("Failed to evaluate demo account flag for {}: {}", email, e.getMessage());
            return false;
        }
    }

    private void updateUserEnabledState(String email, boolean enabled) {
        if (!isConfigPresent() || email == null || email.isBlank()) {
            return;
        }
        try (Keycloak keycloak = getKeycloakClient()) {
            UserRepresentation user = findUserByEmail(keycloak, email);
            if (user == null || user.getId() == null) {
                logger.warn("updateUserEnabledState: user not found for email {}", email);
                return;
            }
            UserResource userResource = keycloak.realm(realm).users().get(user.getId());
            UserRepresentation fullUser = userResource.toRepresentation();
            fullUser.setEnabled(enabled);
            userResource.update(fullUser);
        } catch (Exception e) {
            logger.error("Failed to update enabled={} for user {}: {}", enabled, email, e.getMessage(), e);
        }
    }

    private UserRepresentation findUserByEmail(Keycloak keycloak, String email) {
        if (keycloak == null || email == null || email.isBlank()) {
            return null;
        }
        List<UserRepresentation> users = keycloak.realm(realm).users().search(email.trim(), true, 0, 20);
        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.stream()
                .filter(u -> u != null && u.getEmail() != null && email.trim().equalsIgnoreCase(u.getEmail().trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Whether realm is configured to record LOGIN events (used for UI hint only).
     * <p>Keycloak 26+ may leave {@link RealmRepresentation#isEventsEnabled()} null; we also read
     * {@link RealmResource#getRealmEventsConfig()} and treat LOGIN type case-insensitively.
     */
    public boolean isRealmLoginEventsEnabled() {
        if (!isConfigPresent()) {
            return false;
        }
        try (Keycloak kc = getKeycloakClient()) {
            RealmResource realmRes = kc.realm(realm);
            try {
                RealmEventsConfigRepresentation cfg = realmRes.getRealmEventsConfig();
                if (cfg != null && realmEventsConfigAllowsLoginEvents(cfg)) {
                    return true;
                }
            } catch (Exception e) {
                logger.debug("getRealmEventsConfig: {}", e.getMessage());
            }
            RealmRepresentation r = realmRes.toRepresentation();
            return realmRepresentationAllowsLoginEvents(r);
        } catch (Exception e) {
            logger.warn("Could not read Keycloak realm events config: {}", e.getMessage());
            return false;
        }
    }

    private static boolean realmEventsConfigAllowsLoginEvents(RealmEventsConfigRepresentation cfg) {
        if (!cfg.isEventsEnabled()) {
            return false;
        }
        return enabledEventTypesAllowLogin(cfg.getEnabledEventTypes());
    }

    private static boolean realmRepresentationAllowsLoginEvents(RealmRepresentation r) {
        Boolean enabled = r.isEventsEnabled();
        if (Boolean.FALSE.equals(enabled)) {
            return false;
        }
        if (Boolean.TRUE.equals(enabled)) {
            return enabledEventTypesAllowLogin(r.getEnabledEventTypes());
        }
        // enabled == null (common on newer Keycloak exports): infer from saved types only
        return enabledEventTypesAllowLogin(r.getEnabledEventTypes());
    }

    private static boolean enabledEventTypesAllowLogin(List<String> types) {
        if (types == null || types.isEmpty()) {
            return true;
        }
        for (String t : types) {
            if (t != null && "LOGIN".equalsIgnoreCase(t.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLoginEventType(String type) {
        return type != null && "LOGIN".equalsIgnoreCase(type.trim());
    }

    private static Instant later(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    /**
     * Best known last activity from LOGIN events (several query shapes) and active user sessions.
     */
    private Instant resolveLastLoginOrSessionActivity(RealmResource realmResource, String userId) {
        UserResource userResource = realmResource.users().get(userId);
        Instant fromEvents = latestLoginEventInstant(realmResource, userId);
        Instant fromSessions = latestUserSessionInstant(userResource, userId);
        return later(fromEvents, fromSessions);
    }

    private Instant latestLoginEventInstant(RealmResource realmResource, String userId) {
        long maxMillis = 0L;
        for (List<String> types : List.of(List.of("LOGIN"), List.of("login"))) {
            try {
                List<EventRepresentation> ev = realmResource.getEvents(
                        types, null, userId, null, null, null, 0, 50);
                maxMillis = Math.max(maxMillis, maxEventTimeMillis(ev, false));
            } catch (Exception ex) {
                logger.debug("Keycloak getEvents typed {} for user {}: {}", types, userId, ex.getMessage());
            }
        }
        if (maxMillis == 0L) {
            try {
                List<EventRepresentation> ev = realmResource.getEvents(
                        null, null, userId, null, null, null, 0, 100);
                maxMillis = maxEventTimeMillis(ev, true);
            } catch (Exception ex) {
                logger.debug("Keycloak getEvents (untyped) for user {}: {}", userId, ex.getMessage());
            }
        }
        return maxMillis > 0L ? Instant.ofEpochMilli(maxMillis) : null;
    }

    private static long maxEventTimeMillis(List<EventRepresentation> ev, boolean filterLoginTypeOnly) {
        if (ev == null) {
            return 0L;
        }
        long max = 0L;
        for (EventRepresentation er : ev) {
            if (er == null) {
                continue;
            }
            if (filterLoginTypeOnly && !isLoginEventType(er.getType())) {
                continue;
            }
            long t = er.getTime();
            if (t > max) {
                max = t;
            }
        }
        return max;
    }

    private Instant latestUserSessionInstant(UserResource userResource, String userId) {
        try {
            List<UserSessionRepresentation> sessions = userResource.getUserSessions();
            if (sessions == null || sessions.isEmpty()) {
                return null;
            }
            long max = 0L;
            for (UserSessionRepresentation s : sessions) {
                if (s == null) {
                    continue;
                }
                long row = Math.max(s.getLastAccess(), s.getStart());
                if (row > max) {
                    max = row;
                }
            }
            return max > 0L ? Instant.ofEpochMilli(max) : null;
        } catch (Exception e) {
            logger.debug("Keycloak getUserSessions failed for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Latest LOGIN event or user-session activity per normalized email (realm users only).
     * Uses LOGIN events when returned by Keycloak, and falls back to {@link UserResource#getUserSessions()}
     * so the admin UI can show recent logins even when the events API is empty or permission-limited.
     * Parallel batching matches {@link #checkEmailsExistInRealmBatched}.
     */
    public Map<String, Instant> getLastLoginForEmails(List<String> normalizedEmails) {
        if (!isConfigPresent() || normalizedEmails == null || normalizedEmails.isEmpty()) {
            return Map.of();
        }
        List<String> distinct = normalizedEmails.stream()
                .filter(e -> e != null && !e.isBlank())
                .map(e -> e.trim().toLowerCase())
                .distinct()
                .toList();
        if (distinct.size() > EMAIL_PREVIEW_MAX_UNIQUE) {
            throw new IllegalArgumentException(
                    "Too many distinct emails (" + distinct.size() + "); max " + EMAIL_PREVIEW_MAX_UNIQUE);
        }
        Map<String, Instant> result = new ConcurrentHashMap<>();
        int n = distinct.size();
        int workers = Math.min(EMAIL_PREVIEW_PARALLEL_WORKERS, Math.max(1, (n + 199) / 200));
        List<List<String>> slices = splitIntoSlices(distinct, workers);
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, slices.size()));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (List<String> slice : slices) {
                futures.add(pool.submit(() -> {
                    try (Keycloak kc = getKeycloakClient()) {
                        RealmResource realmResource = kc.realm(realm);
                        for (String email : slice) {
                            UserRepresentation user = findUserByEmail(kc, email);
                            if (user == null || user.getId() == null) {
                                continue;
                            }
                            Instant activity = resolveLastLoginOrSessionActivity(realmResource, user.getId());
                            if (activity != null) {
                                result.put(email, activity);
                            }
                        }
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Last-login lookup interrupted", e);
        } catch (ExecutionException e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("Keycloak last-login batch failed: " + c.getMessage(), c);
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(30, TimeUnit.MINUTES)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        return result;
    }

    /**
     * Sets a new temporary password and UPDATE_PASSWORD required action; returns plain password for welcome email.
     *
     * @throws IllegalArgumentException if user not found or Keycloak not configured
     */
    public String regenerateTemporaryPasswordForEmail(String email) {
        if (!isConfigPresent()) {
            throw new IllegalStateException("Keycloak config missing");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String emailTrim = email.trim();
        try (Keycloak kc = getKeycloakClient()) {
            UserRepresentation user = findUserByEmail(kc, emailTrim);
            if (user == null || user.getId() == null || user.getId().isBlank()) {
                throw new IllegalArgumentException("User not found in Keycloak for email");
            }
            String pwd = PasswordGenerator.generateRandomPassword(12);
            UserResource ur = kc.realm(realm).users().get(user.getId());
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(pwd);
            cred.setTemporary(true);
            ur.resetPassword(cred);
            UserRepresentation rep = ur.toRepresentation();
            rep.setRequiredActions(List.of("UPDATE_PASSWORD"));
            ur.update(rep);
            return pwd;
        }
    }

    /**
     * Sends Keycloak's execute-actions email for UPDATE_PASSWORD (password reset link flow).
     *
     * @param clientId     optional Keycloak client id (null = realm default)
     * @param redirectUri  optional redirect after actions (e.g. portal login URL)
     * @param lifespanSeconds optional link lifetime; defaults to 86400 when null
     */
    public void sendUpdatePasswordActionEmail(String email, String clientId, String redirectUri, Integer lifespanSeconds) {
        if (!isConfigPresent()) {
            throw new IllegalStateException("Keycloak config missing");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        int lifespan = lifespanSeconds != null && lifespanSeconds > 0 ? lifespanSeconds : 86400;
        String emailTrim = email.trim();
        try (Keycloak kc = getKeycloakClient()) {
            UserRepresentation user = findUserByEmail(kc, emailTrim);
            if (user == null || user.getId() == null || user.getId().isBlank()) {
                throw new IllegalArgumentException("User not found in Keycloak for email");
            }
            UserResource ur = kc.realm(realm).users().get(user.getId());
            List<String> actions = List.of("UPDATE_PASSWORD");
            String cid = clientId != null && !clientId.isBlank() ? clientId.trim() : null;
            String redir = redirectUri != null && !redirectUri.isBlank() ? redirectUri.trim() : null;
            redirectTargetValidator.requireAllowed(redir);
            if (cid != null || redir != null) {
                ur.executeActionsEmail(cid, redir, lifespan, actions);
            } else {
                ur.executeActionsEmail(actions, lifespan);
            }
        }
    }
}
