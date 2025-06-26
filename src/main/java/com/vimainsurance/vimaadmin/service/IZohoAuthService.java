package com.vimainsurance.vimaadmin.service;

public interface IZohoAuthService {
    void initializeZohoCRM() throws Exception;
    void refreshZohoToken() throws Exception;
    public void initializeZohoManually() throws Exception;
} 