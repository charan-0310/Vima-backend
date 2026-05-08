package com.vimainsurance.vimaadmin.service.wellness.mantra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import com.vimainsurance.vimaadmin.service.wellness.exception.MantraCareException;

class MantraCareClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private MantraCareClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new MantraCareClient();
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    @SuppressWarnings("null")
    void postAndParse_returnsRedirectUrl() {
        server.expect(MockRestRequestMatchers.requestTo("https://api.mantracare.com/partner/user"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"redirect_url\":\"https://web.mantracare.com/login/magic-link?token=abc\"}",
                        MediaType.APPLICATION_JSON));

        String url = client.postAndParse(
                restTemplate,
                "https://api.mantracare.com",
                "/partner/user",
                3000,
                8000,
                null,
                "signed-jwt");
        assertEquals("https://web.mantracare.com/login/magic-link?token=abc", url);
    }

    @Test
    void postAndParse_missingRedirectUrl_throws() {
        server.expect(MockRestRequestMatchers.requestTo("https://api.mantracare.com/partner/user"))
                .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(MantraCareException.class, () -> client.postAndParse(
                restTemplate,
                "https://api.mantracare.com",
                "/partner/user",
                3000,
                8000,
                null,
                "jwt"));
    }

    @Test
    void postAndParse_withCookie_addsCookieHeader() {
        server.expect(MockRestRequestMatchers.requestTo("https://api.mantracare.com/partner/user"))
                .andExpect(MockRestRequestMatchers.header("Cookie", "connect.sid=abc"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"redirect_url\":\"https://web.mantracare.com/login/magic-link?token=abc\"}",
                        MediaType.APPLICATION_JSON));

        String url = client.postAndParse(
                restTemplate,
                "https://api.mantracare.com",
                "/partner/user",
                3000,
                8000,
                "connect.sid=abc",
                "signed-jwt");
        assertEquals("https://web.mantracare.com/login/magic-link?token=abc", url);
    }
}
