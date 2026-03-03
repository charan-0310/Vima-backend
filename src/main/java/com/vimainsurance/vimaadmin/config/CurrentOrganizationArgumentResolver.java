package com.vimainsurance.vimaadmin.config;

import java.util.Map;
import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import com.vimainsurance.vimaadmin.annotation.CurrentOrganization;
import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

/**
 * Resolves {@link CurrentOrganization} parameters: reads organizationId from path variable or
 * X-Organization-ID header, validates access via {@link JwtUserExtractor}, sets
 * {@link AuditContextSupplier#setOrganizationId(UUID)} for the request thread, and returns the UUID.
 * <p>
 * When organizationId is only in the request body (e.g. {@link com.vimainsurance.vimaadmin.dto.OrganizationRequestDto}),
 * use {@link com.vimainsurance.vimaadmin.util.OrganizationAccessHelper#validateAndSetContext(UUID)} instead.
 */
@Slf4j
@Component
public class CurrentOrganizationArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String HEADER_ORGANIZATION_ID = "X-Organization-ID";
    private static final String PATH_VAR_ORGANIZATION_ID = "organizationId";

    @Autowired(required = false)
    private JwtUserExtractor jwtUserExtractor;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(CurrentOrganization.class) != null
                && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    @Nullable
    public Object resolveArgument(MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) throws Exception {
        CurrentOrganization ann = parameter.getParameterAnnotation(CurrentOrganization.class);
        if (ann == null) {
            return null;
        }

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            if (ann.required()) {
                throw new IllegalArgumentException("Organization ID is required but request is not available");
            }
            return null;
        }

        String raw = getOrganizationIdFromRequest(request);
        if (raw == null || raw.isBlank()) {
            if (ann.required()) {
                throw new IllegalArgumentException("Organization ID is required (path variable 'organizationId' or header 'X-Organization-ID')");
            }
            return null;
        }

        UUID organizationId;
        try {
            organizationId = UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            if (ann.required()) {
                throw new IllegalArgumentException("Invalid organization ID format: " + raw, e);
            }
            return null;
        }

        if (jwtUserExtractor != null) {
            jwtUserExtractor.validateOrganizationAccess(organizationId);
        }

        AuditContextSupplier.setOrganizationId(organizationId);
        return organizationId;
    }

    @SuppressWarnings("unchecked")
    private String getOrganizationIdFromRequest(HttpServletRequest request) {
        Map<String, String> pathVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVars != null) {
            String fromPath = pathVars.get(PATH_VAR_ORGANIZATION_ID);
            if (fromPath != null && !fromPath.isBlank()) {
                return fromPath;
            }
        }
        String fromHeader = request.getHeader(HEADER_ORGANIZATION_ID);
        return fromHeader != null ? fromHeader : null;
    }
}
