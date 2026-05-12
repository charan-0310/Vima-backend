package com.vimainsurance.vimaadmin.service.wellness.mantra;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class MantraCareReadTimeoutDetectionTest {

    @Test
    void isLikelyReadTimeout_true_whenSocketTimeoutMessageRead() {
        var cause = new SocketTimeoutException("Read timed out");
        var ex = new ResourceAccessException(
                "I/O error on POST request for \"https://api.mantracare.com/partner/user\": Read timed out", cause);
        assertTrue(MantraCareClient.isLikelyReadTimeout(ex));
    }

    @Test
    void isLikelyReadTimeout_true_whenMessageContainsReadTimedOutOnly() {
        var ex = new ResourceAccessException("I/O error on POST request for \"https://x\": Read timed out");
        assertTrue(MantraCareClient.isLikelyReadTimeout(ex));
    }

    @Test
    void isLikelyReadTimeout_false_whenConnectTimedOut() {
        var cause = new SocketTimeoutException("Connect timed out");
        var ex = new ResourceAccessException("I/O error on POST request for \"https://x\": Connect timed out", cause);
        assertFalse(MantraCareClient.isLikelyReadTimeout(ex));
    }
}
