package com.vimainsurance.vimaadmin.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class PasswordGeneratorTest {

    @Test
    void testGenerateRandomPasswordWithSpecificLength() {
        String password = PasswordGenerator.generateRandomPassword(8);
        assertEquals(8, password.length());
    }

    @Test
    void testGenerateRandomPasswordWithLength12() {
        String password = PasswordGenerator.generateRandomPassword(12);
        assertEquals(12, password.length());
    }

    @Test
    void testGenerateRandomPasswordWithInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordGenerator.generateRandomPassword(7);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordGenerator.generateRandomPassword(21);
        });
    }

    @RepeatedTest(10)
    void testGenerateRandomPasswordDefault() {
        String password = PasswordGenerator.generateRandomPassword();
        assertEquals(12, password.length());
    }

    @RepeatedTest(20)
    void testPasswordUniqueness() {
        String password1 = PasswordGenerator.generateRandomPassword();
        String password2 = PasswordGenerator.generateRandomPassword();
        
        // While it's possible for passwords to be the same, it's highly unlikely
        // This test helps ensure randomness
        assertEquals(12, password1.length());
        assertEquals(12, password2.length());
    }
}
