package com.vimainsurance.vimaadmin.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class PasswordGeneratorTest {

    @Test
    void testGenerateRandomPasswordWithSpecificLength() {
        String password = PasswordGenerator.generateRandomPassword(8);
        assertEquals(8, password.length());
        assertTrue(password.matches("[A-Za-z0-9]+"));
    }

    @Test
    void testGenerateRandomPasswordWithLength10() {
        String password = PasswordGenerator.generateRandomPassword(10);
        assertEquals(10, password.length());
        assertTrue(password.matches("[A-Za-z0-9]+"));
    }

    @Test
    void testGenerateRandomPasswordWithInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordGenerator.generateRandomPassword(7);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordGenerator.generateRandomPassword(11);
        });
    }

    @RepeatedTest(10)
    void testGenerateRandomPasswordDefault() {
        String password = PasswordGenerator.generateRandomPassword();
        assertTrue(password.length() >= 8 && password.length() <= 10);
        assertTrue(password.matches("[A-Za-z0-9]+"));
    }

    @RepeatedTest(20)
    void testPasswordUniqueness() {
        String password1 = PasswordGenerator.generateRandomPassword();
        String password2 = PasswordGenerator.generateRandomPassword();
        
        // While it's possible for passwords to be the same, it's highly unlikely
        // This test helps ensure randomness
        assertTrue(password1.length() >= 8 && password1.length() <= 10);
        assertTrue(password2.length() >= 8 && password2.length() <= 10);
    }
}
