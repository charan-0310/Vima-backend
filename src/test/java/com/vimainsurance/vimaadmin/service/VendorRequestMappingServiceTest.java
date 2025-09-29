package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.QuickQuoteRequestDto;
import com.vimainsurance.vimaadmin.dto.digit.QQGlowWrapper;
import com.vimainsurance.vimaadmin.enums.Vendor;
import com.vimainsurance.vimaadmin.mapper.VendorDigitRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorRequestMappingServiceTest {

    @Mock
    private VendorDigitRequestMapper digitRequestMapper;

    @InjectMocks
    private VendorRequestMappingService vendorRequestMappingService;

    private QuickQuoteRequestDto testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new QuickQuoteRequestDto();
        testRequest.setName("John Doe");
        testRequest.setGender("male");
        testRequest.setDateOfBirth("1990-05-15");
        testRequest.setSmoker(false);
        testRequest.setAlcoholic(false);
        testRequest.setCoverageEndAge("60");
        testRequest.setSumAssured(List.of(2500000L));
        testRequest.setPaymentFrequency("annual");
        testRequest.setOccupation("Software Engineer");

        // Setup mock behavior
        when(digitRequestMapper.getVendor()).thenReturn(Vendor.DIGIT);
        
        // Use reflection to set the private field for testing
        try {
            java.lang.reflect.Field field = VendorRequestMappingService.class.getDeclaredField("vendorRequestMappers");
            field.setAccessible(true);
            field.set(vendorRequestMappingService, List.of(digitRequestMapper));
            vendorRequestMappingService.initializeMapperRegistry();
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup test", e);
        }
    }

    @Test
    void testMapToVendorDto_Success() {
        // Given
        QQGlowWrapper expectedResult = new QQGlowWrapper();
        when(digitRequestMapper.map(testRequest)).thenReturn(expectedResult);

        // When
        Object result = vendorRequestMappingService.mapToVendorDto(testRequest, Vendor.DIGIT);

        // Then
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    void testMapToVendorDto_VendorNotFound() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> vendorRequestMappingService.mapToVendorDto(testRequest, null)
        );
        
        assertTrue(exception.getMessage().contains("No mapper found for vendor"));
    }

    @Test
    void testGetAvailableMappers() {
        // When
        Map<Vendor, ?> availableMappers = vendorRequestMappingService.getAvailableMappers();

        // Then
        assertNotNull(availableMappers);
        assertTrue(availableMappers.containsKey(Vendor.DIGIT));
        assertEquals(1, availableMappers.size());
    }
} 