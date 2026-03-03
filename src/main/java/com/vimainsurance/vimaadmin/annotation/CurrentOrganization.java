package com.vimainsurance.vimaadmin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the current request's organization ID (validated for access) to a controller method parameter.
 * Resolved from path variable "organizationId" or header "X-Organization-ID".
 * Validates access via JwtUserExtractor and sets AuditContextSupplier for the request thread.
 * Use on {@code UUID} parameters only.
 * <p>
 * When organizationId is only in the request body (DTO), use
 * {@link com.vimainsurance.vimaadmin.util.OrganizationAccessHelper#validateAndSetContext(UUID)}
 * at the start of the service method with {@code dto.getOrganizationId()}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentOrganization {

    /**
     * When true (default), missing or invalid organization ID results in 400.
     * When false, allows null for endpoints that work across organizations.
     */
    boolean required() default true;
}
