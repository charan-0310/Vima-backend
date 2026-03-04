package com.vimainsurance.vimaadmin.util;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;

/**
 * Use when organizationId comes from a DTO (request body) rather than path/header.
 * Validates access and sets audit context so the rest of the request runs with the same
 * guarantees as {@link com.vimainsurance.vimaadmin.annotation.CurrentOrganization}.
 */
@Component
public class OrganizationAccessHelper {

    @Autowired(required = false)
    private JwtUserExtractor jwtUserExtractor;

    /**
     * Validates that the current user can access the organization and sets
     * {@link AuditContextSupplier#setOrganizationId(UUID)} for the request thread.
     * Call at the start of a service method when organizationId is from a DTO.
     *
     * @param organizationId from e.g. requestDto.getOrganizationId()
     * @throws com.vimainsurance.vimaadmin.exception.OrganizationAccessDeniedException if access is denied
     */
    public void validateAndSetContext(UUID organizationId) {
        if (organizationId == null) {
            return;
        }
        if (jwtUserExtractor != null) {
            jwtUserExtractor.validateOrganizationAccess(organizationId);
        }
        AuditContextSupplier.setOrganizationId(organizationId);
    }
}
