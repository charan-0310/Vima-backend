package com.vimainsurance.vimaadmin.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as auditable. AOP will capture context and write asynchronously after successful return.
 * Loosely coupled: business code only declares the annotation; no direct dependency on audit implementation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditedOperation {

    /** Database schema of the audited entity (e.g. cpc, claims). Optional. */
    String schemaName() default "";

    /** Table name of the audited entity (e.g. customers, endorsements). Optional. */
    String tableName() default "";

    /** Logical entity type (e.g. EMPLOYEE_UPLOAD, ENDORSEMENT, POLICY). */
    String entityType();

    /** Action (e.g. BULK_UPLOAD, CREATE, UPDATE, DELETE). */
    String action();
}
