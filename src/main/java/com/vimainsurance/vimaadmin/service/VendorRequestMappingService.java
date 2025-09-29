package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.QuickQuoteRequestDto;
import com.vimainsurance.vimaadmin.enums.Vendor;
import com.vimainsurance.vimaadmin.mapper.VendorRequestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VendorRequestMappingService {
    
    @Autowired
    private List<VendorRequestMapper> vendorRequestMappers;
    
    private Map<Vendor, VendorRequestMapper> mapperRegistry;
    
    @PostConstruct
    public void initializeMapperRegistry() {
        mapperRegistry = new HashMap<>();
        for (VendorRequestMapper mapper : vendorRequestMappers) {
            mapperRegistry.put(mapper.getVendor(), mapper);
        }
    }
    
    public Object mapToVendorDto(QuickQuoteRequestDto dto, Vendor vendor) {
        VendorRequestMapper mapper = mapperRegistry.get(vendor);
        if (mapper == null) {
            throw new IllegalArgumentException("No mapper found for vendor: " + vendor);
        }
        return mapper.map(dto);
    }
    
    public Map<Vendor, VendorRequestMapper> getAvailableMappers() {
        return new HashMap<>(mapperRegistry);
    }
} 