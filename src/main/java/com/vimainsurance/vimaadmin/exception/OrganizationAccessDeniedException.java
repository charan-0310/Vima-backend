package com.vimainsurance.vimaadmin.exception;

public class OrganizationAccessDeniedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OrganizationAccessDeniedException(String message) {
        super(message);
    }
}

