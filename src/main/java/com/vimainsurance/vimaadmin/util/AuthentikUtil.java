package com.vimainsurance.vimaadmin.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.AuthentikGroupCreationDto;
import com.vimainsurance.vimaadmin.dto.AuthentikPaginatedResponse;
import com.vimainsurance.vimaadmin.dto.OrganizationDto;
import com.vimainsurance.vimaadmin.dto.RoleDto;

@Service
public class AuthentikUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestTemplate restTemplate;

    @Value("${authentik.url}")
    private String authentikUrl;

    @Value("${authentik.token}")
    private String authentikToken;

    public void createGroup(AuthentikGroupCreationDto groupCreationDto) {
        String url = authentikUrl + "/core/groups/";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authentikToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<AuthentikGroupCreationDto> request = new HttpEntity<>(groupCreationDto, headers);
        restTemplate.postForEntity(url, request, Void.class);
    }

    /**
     * Get users from Authentik with pagination
     * @param page Page number (1-based, default: 1)
     * @param pageSize Number of records per page (default: 25)
     * @return Paginated response with users and pagination info
     */
    public AuthentikPaginatedResponse<AdminUserResponseDto> getUsers(Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 25;
        }

        String url = UriComponentsBuilder.fromUriString(authentikUrl + "/core/users/")
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authentikToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Object.class);
        
        if(response.getStatusCode().is2xxSuccessful()){
            return mapAuthentikResponseToPaginatedDto(response.getBody());
        } else {
            throw new RuntimeException("Failed to get users from Authentik");
        }
    }

    /**
     * Get users from Authentik with search, filtering, sorting, and pagination
     * @param search Search term (searches in username, email, name)
     * @param isActive Filter by active status (true/false)
     * @param ordering Sort field (e.g., "username", "-username" for desc, "email", "-email")
     * @param groupsByName List of group names for filtering (e.g., ["ROLE_VIMA_ADMIN", "ORG_MAIN"])
     *                     Each group will be added as a separate groups_by_name query parameter
     * @param page Page number (1-based, default: 1)
     * @param pageSize Number of records per page (default: 25)
     * @return Paginated response with users and pagination info
     */
    public AuthentikPaginatedResponse<AdminUserResponseDto> getUsersWithFilters(
            String search, Boolean isActive, String ordering, List<String> groupsByName, Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 25;
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(authentikUrl + "/core/users/")
                .queryParam("page", page)
                .queryParam("page_size", pageSize);

        // Add search parameter if provided
        if (search != null && !search.trim().isEmpty()) {
            uriBuilder.queryParam("search", search.trim());
        }

        // Add is_active filter if provided
        if (isActive != null) {
            uriBuilder.queryParam("is_active", isActive);
        }

        // Add ordering if provided
        if (ordering != null && !ordering.trim().isEmpty()) {
            uriBuilder.queryParam("ordering", ordering.trim());
        }

        // Add groups_by_name filters if provided (supports multiple groups)
        // Each group will be added as a separate groups_by_name query parameter
        if (groupsByName != null && !groupsByName.isEmpty()) {
            for (String groupName : groupsByName) {
                if (groupName != null && !groupName.trim().isEmpty()) {
                    uriBuilder.queryParam("groups_by_name", groupName.trim());
                }
            }
        }

        String url = uriBuilder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authentikToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Object.class);
        
        if(response.getStatusCode().is2xxSuccessful()){
            return mapAuthentikResponseToPaginatedDto(response.getBody());
        } else {
            throw new RuntimeException("Failed to get users from Authentik");
        }
    }

    /**
     * Get all users from Authentik by fetching all pages
     * @return List of all users
     */
    public List<AdminUserResponseDto> getAllUsers() {
        List<AdminUserResponseDto> allUsers = new ArrayList<>();
        int currentPage = 1;
        int pageSize = 100; // Use a reasonable page size for fetching all
        
        while (true) {
            AuthentikPaginatedResponse<AdminUserResponseDto> response = getUsers(currentPage, pageSize);
            if (response.getResults() == null || response.getResults().isEmpty()) {
                break;
            }
            allUsers.addAll(response.getResults());
            
            // Check if there's a next page
            if (response.getPagination() != null && response.getPagination().getNext() != null && response.getPagination().getNext() > 0) {
                currentPage = response.getPagination().getNext();
            } else {
                break;
            }
        }
        
        return allUsers;
    }

    @SuppressWarnings("unchecked")
    private AuthentikPaginatedResponse<AdminUserResponseDto> mapAuthentikResponseToPaginatedDto(Object responseBody) {
        AuthentikPaginatedResponse<AdminUserResponseDto> paginatedResponse = new AuthentikPaginatedResponse<>();
        List<AdminUserResponseDto> result = new ArrayList<>();
        
        try {
            Map<String, Object> responseMap = objectMapper.convertValue(responseBody, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
            
            if (results != null) {
                for (Map<String, Object> userMap : results) {
                    AdminUserResponseDto dto = mapAuthentikUserToDto(userMap);
                    result.add(dto);
                }
            }
            
            // Extract pagination info
            Map<String, Object> paginationMap = (Map<String, Object>) responseMap.get("pagination");
            if (paginationMap != null) {
                AuthentikPaginatedResponse.PaginationInfo paginationInfo = new AuthentikPaginatedResponse.PaginationInfo();
                paginationInfo.setNext(getIntegerValue(paginationMap.get("next")));
                paginationInfo.setPrevious(getIntegerValue(paginationMap.get("previous")));
                paginationInfo.setCount(getIntegerValue(paginationMap.get("count")));
                paginationInfo.setCurrent(getIntegerValue(paginationMap.get("current")));
                paginationInfo.setTotalPages(getIntegerValue(paginationMap.get("total_pages")));
                paginationInfo.setStartIndex(getIntegerValue(paginationMap.get("start_index")));
                paginationInfo.setEndIndex(getIntegerValue(paginationMap.get("end_index")));
                paginatedResponse.setPagination(paginationInfo);
            }
            
            paginatedResponse.setResults(result);
        } catch (Exception e) {
            throw new RuntimeException("Error mapping Authentik response to DTO", e);
        }
        
        return paginatedResponse;
    }

    private Integer getIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private AdminUserResponseDto mapAuthentikUserToDto(Map<String, Object> userMap) {
        AdminUserResponseDto dto = new AdminUserResponseDto();
        
        dto.setUsername((String) userMap.get("username"));
        dto.setEmail((String) userMap.get("email"));
        dto.setFullName((String) userMap.get("name"));
        dto.setIsActive((Boolean) userMap.get("is_active"));
        
        // Map dates
        String lastLoginStr = (String) userMap.get("last_login");
        if (lastLoginStr != null) {
            dto.setLastLogin(parseDateTime(lastLoginStr));
        }
        
        String dateJoinedStr = (String) userMap.get("date_joined");
        if (dateJoinedStr != null) {
            dto.setCreatedAt(parseDateTime(dateJoinedStr));
        }
        
        // Extract roles and organizations from groups_obj
        List<Map<String, Object>> groupsObj = (List<Map<String, Object>>) userMap.get("groups_obj");
        List<String> roles = new ArrayList<>();
        List<String> organizations = new ArrayList<>();
        
        if (groupsObj != null) {
            for (Map<String, Object> group : groupsObj) {
                String groupName = (String) group.get("name");
                if (groupName != null) {
                    if (groupName.startsWith("ROLE_")) {
                        roles.add(groupName);
                    } else if (groupName.startsWith("ORG_")) {
                        organizations.add(groupName);
                    }
                }
            }
        }
        
        dto.setRoles(roles);
        dto.setOrganizations(organizations);
        
        // Set oauthProviderId (uid field in Authentik)
        dto.setOauthProviderId((String) userMap.get("uid"));
        
        return dto;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr == null || dateTimeStr.isEmpty()) {
                return null;
            }
            // Parse ISO 8601 format (e.g., "2026-01-09T05:13:15.081071Z")
            Instant instant = Instant.parse(dateTimeStr);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get all groups from Authentik (helper method)
     * @return List of all groups as maps containing name and pk
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAllGroups() {
        List<Map<String, Object>> allGroups = new ArrayList<>();
        int currentPage = 1;
        int pageSize = 100; // Use a reasonable page size for fetching all
        
        while (true) {
            String url = UriComponentsBuilder.fromUriString(authentikUrl + "/core/groups/")
                    .queryParam("page", currentPage)
                    .queryParam("page_size", pageSize)
                    .toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authentikToken);
            headers.set("Content-Type", "application/json");
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Object.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to get groups from Authentik");
            }
            
            try {
                Map<String, Object> responseMap = objectMapper.convertValue(response.getBody(), Map.class);
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
                
                if (results != null && !results.isEmpty()) {
                    allGroups.addAll(results);
                }
                
                // Check if there's a next page
                Map<String, Object> paginationMap = (Map<String, Object>) responseMap.get("pagination");
                if (paginationMap != null) {
                    Integer nextPage = getIntegerValue(paginationMap.get("next"));
                    if (nextPage != null && nextPage > 0) {
                        currentPage = nextPage;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                throw new RuntimeException("Error parsing groups from Authentik response", e);
            }
        }
        
        return allGroups;
    }

    /**
     * Get all roles from Authentik (groups starting with ROLE_)
     * @return List of roles with their IDs and names
     */
    public List<RoleDto> getRoles() {
        List<RoleDto> roles = new ArrayList<>();
        List<Map<String, Object>> allGroups = getAllGroups();
        
        for (Map<String, Object> groupMap : allGroups) {
            String groupName = (String) groupMap.get("name");
            String groupId = (String) groupMap.get("pk");
            
            if (groupName != null && groupId != null && groupName.startsWith("ROLE_")) {
                RoleDto role = new RoleDto();
                role.setId(groupId);
                role.setName(groupName);
                roles.add(role);
            }
        }
        
        return roles;
    }

    /**
     * Get all organizations from Authentik (groups starting with ORG_)
     * @return List of organizations with their IDs and names
     */
    public List<OrganizationDto> getOrganizations() {
        List<OrganizationDto> organizations = new ArrayList<>();
        List<Map<String, Object>> allGroups = getAllGroups();
        
        for (Map<String, Object> groupMap : allGroups) {
            String groupName = (String) groupMap.get("name");
            String groupId = (String) groupMap.get("pk");
            
            if (groupName != null && groupId != null && groupName.startsWith("ORG_")) {
                OrganizationDto org = new OrganizationDto();
                org.setId(groupId);
                org.setName(groupName);
                organizations.add(org);
            }
        }
        
        return organizations;
    }
}
