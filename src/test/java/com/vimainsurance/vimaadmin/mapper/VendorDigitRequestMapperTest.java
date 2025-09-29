package com.vimainsurance.vimaadmin.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.vimainsurance.vimaadmin.config.VendorMasterDataConfig;
import com.vimainsurance.vimaadmin.dto.QuickQuoteRequestDto;
import com.vimainsurance.vimaadmin.dto.digit.QQGlowWrapper;

class VendorDigitRequestMapperTest {

    @Mock
    private VendorMasterDataConfig vendorMasterDataConfig;
    
    @Mock
    private VendorMasterDataConfig.VendorMasterData vendorData;

    @InjectMocks
    private VendorDigitRequestMapper vendorDigitRequestMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock vendor data
        when(vendorMasterDataConfig.getVendorData("DIGIT")).thenReturn(vendorData);
        when(vendorData.getProductCodes()).thenReturn(Arrays.asList("10406"));
        when(vendorData.getPolicyType()).thenReturn(Arrays.asList("Singlelife"));
        when(vendorData.getCoverageTypes()).thenReturn(Arrays.asList("BASE"));
        when(vendorData.getCoverageNames()).thenReturn(Arrays.asList("TERM_LIFE"));
        
        // Mock mappings
        when(vendorMasterDataConfig.mapGender("DIGIT", "male")).thenReturn("Male");
        when(vendorMasterDataConfig.mapSmokerStatus("DIGIT", true)).thenReturn("SMOKER");
        when(vendorMasterDataConfig.mapSmokerStatus("DIGIT", false)).thenReturn("NON_SMOKER");
        when(vendorMasterDataConfig.mapAlcoholStatus("DIGIT", true)).thenReturn("ALCOHOLIC");
        when(vendorMasterDataConfig.mapAlcoholStatus("DIGIT", false)).thenReturn("NON_ALCOHOLIC");
        when(vendorMasterDataConfig.mapPaymentFrequency("DIGIT", "annual")).thenReturn("Yearly");
        
        // Mock defaults
        when(vendorMasterDataConfig.getVendorDefault("DIGIT", "imdCode")).thenReturn("2000273");
        when(vendorMasterDataConfig.getVendorDefault("DIGIT", "productCategory")).thenReturn("TR");
        when(vendorMasterDataConfig.getVendorDefault("DIGIT", "relationshipWithProposer")).thenReturn("Individual");
        when(vendorMasterDataConfig.getVendorDefault("DIGIT", "highestEducation")).thenReturn("GRADUATE");
        when(vendorMasterDataConfig.getVendorDefault("DIGIT", "residentType")).thenReturn("C");
        when(vendorMasterDataConfig.getVendorDefault("DIGIT", "premiumType")).thenReturn("RP");
    }

    @Test
    void testMappingSmokerAndAlcoholic() {
        // Given
        QuickQuoteRequestDto dto = createQuickQuoteRequest();
        dto.setSmoker(true);
        dto.setAlcoholic(true);

        // When
        QQGlowWrapper result = (QQGlowWrapper) vendorDigitRequestMapper.map(dto);

        // Then
        assertNotNull(result);
        assertNotNull(result.getQqGlow());
        assertNotNull(result.getQqGlow().getBasicDetails());
        
        List<QQGlowWrapper.Person> persons = result.getQqGlow().getBasicDetails().getPersons();
        assertEquals(2, persons.size());
        
        // Verify smoker and alcohol status mapping
        assertEquals("SMOKER", persons.get(0).getSmokerStatus());
        assertEquals("ALCOHOLIC", persons.get(0).getAlcoholStatus());
        assertEquals("SMOKER", persons.get(1).getSmokerStatus());
        assertEquals("ALCOHOLIC", persons.get(1).getAlcoholStatus());
    }

    @Test
    void testMappingNonSmokerNonAlcoholic() {
        // Given
        QuickQuoteRequestDto dto = createQuickQuoteRequest();
        dto.setSmoker(false);
        dto.setAlcoholic(false);

        // When
        QQGlowWrapper result = (QQGlowWrapper) vendorDigitRequestMapper.map(dto);

        // Then
        List<QQGlowWrapper.Person> persons = result.getQqGlow().getBasicDetails().getPersons();
        assertEquals("NON_SMOKER", persons.get(0).getSmokerStatus());
        assertEquals("NON_ALCOHOLIC", persons.get(0).getAlcoholStatus());
    }

    @Test
    void testIncomeRangeNullHandling() {
        // Given
        QuickQuoteRequestDto dto = createQuickQuoteRequest();
        dto.setIncomeRange(null);

        // When
        QQGlowWrapper result = (QQGlowWrapper) vendorDigitRequestMapper.map(dto);

        // Then
        List<QQGlowWrapper.Person> persons = result.getQqGlow().getBasicDetails().getPersons();
        assertEquals("300000", persons.get(0).getIncomeRange());
        assertEquals("300000", persons.get(1).getIncomeRange());
    }

    @Test
    void testVendorDataNullHandling() {
        // Given
        when(vendorMasterDataConfig.getVendorData("DIGIT")).thenReturn(null);
        QuickQuoteRequestDto dto = createQuickQuoteRequest();

        // When
        QQGlowWrapper result = (QQGlowWrapper) vendorDigitRequestMapper.map(dto);

        // Then
        assertNotNull(result);
        assertEquals("10406", result.getQqGlow().getBasicDetails().getProductCode());
        assertEquals("singlelife", result.getQqGlow().getBasicDetails().getPolicyType());
    }

    @Test
    void testStateMapping() {
        // Given
        QuickQuoteRequestDto dto = createQuickQuoteRequest();
        dto.setState("Maharashtra");
        
        // Mock state mapping
        when(vendorMasterDataConfig.mapState("DIGIT", "Maharashtra")).thenReturn("27");
        when(vendorMasterDataConfig.mapState("DIGIT", "Delhi")).thenReturn("7");
        when(vendorMasterDataConfig.mapState("DIGIT", "Karnataka")).thenReturn("29");
        when(vendorMasterDataConfig.mapState("DIGIT", "Tamil Nadu")).thenReturn("33");

        // When
        QQGlowWrapper result = (QQGlowWrapper) vendorDigitRequestMapper.map(dto);

        // Then
        List<QQGlowWrapper.Person> persons = result.getQqGlow().getBasicDetails().getPersons();
        assertEquals("27", persons.get(0).getState());
        assertEquals("27", persons.get(1).getState());
        
        // Test with different state
        dto.setState("Delhi");
        result = (QQGlowWrapper) vendorDigitRequestMapper.map(dto);
        persons = result.getQqGlow().getBasicDetails().getPersons();
        assertEquals("7", persons.get(0).getState());
        assertEquals("7", persons.get(1).getState());
        
        // Test with Tamil Nadu
        dto.setState("Tamil Nadu");
        result = (QQGlowWrapper) vendorDigitRequestMapper.map(dto);
        persons = result.getQqGlow().getBasicDetails().getPersons();
        assertEquals("33", persons.get(0).getState());
        assertEquals("33", persons.get(1).getState());
    }

    @Test
    void testStateMappingWithNullState() {
        // Given
        QuickQuoteRequestDto dto = createQuickQuoteRequest();
        dto.setState(null);
        
        // Mock state mapping for null
        when(vendorMasterDataConfig.mapState("DIGIT", null)).thenReturn("");

        // When
        QQGlowWrapper result = (QQGlowWrapper) vendorDigitRequestMapper.map(dto);

        // Then
        List<QQGlowWrapper.Person> persons = result.getQqGlow().getBasicDetails().getPersons();
        assertEquals("", persons.get(0).getState());
        assertEquals("", persons.get(1).getState());
    }

    private QuickQuoteRequestDto createQuickQuoteRequest() {
        QuickQuoteRequestDto dto = new QuickQuoteRequestDto();
        dto.setName("John Doe");
        dto.setGender("male");
        dto.setDateOfBirth("1990-05-15");
        dto.setSmoker(false);
        dto.setAlcoholic(false);
        dto.setOccupation("Software Engineer");
        dto.setPaymentFrequency("annual");
        dto.setSumAssured(Arrays.asList(1000000L));
        dto.setCoverageEndAge("65");
        dto.setIncomeRange("10-25 Lakhs");
        return dto;
    }
} 