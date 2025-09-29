package com.vimainsurance.vimaadmin.mapper;

import com.vimainsurance.vimaadmin.dto.QuickQuoteRequestDto;
import com.vimainsurance.vimaadmin.enums.Vendor;

public interface VendorRequestMapper {
    Object map(QuickQuoteRequestDto dto);
    Vendor getVendor();
} 