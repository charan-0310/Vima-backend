package com.vimainsurance.vimaadmin.service.wellness.mantra;

import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.vimainsurance.vimaadmin.service.wellness.exception.MantraCareException;

@Service
public class MantraCareClient {

    private static final Logger logger = LoggerFactory.getLogger(MantraCareClient.class);
    private static final Pattern CONNECT_SID_PATTERN = Pattern.compile("(?i)(^|;\\s*)connect\\.sid=([^;]+)");

    public String exchangeTokenForRedirectUrl(
            String apiBaseUrl,
            String setUserPath,
            int connectTimeoutMs,
            int readTimeoutMs,
            String requestCookie,
            String jws) {
        String correlationId = MDC.get("correlationId");
        RestTemplate restTemplate = createRestTemplate(connectTimeoutMs, readTimeoutMs);
        long startedAt = System.currentTimeMillis();
        String configuredCookie = normalizeCookie(requestCookie);
        try {
            String url = postAndParse(restTemplate, apiBaseUrl, setUserPath, connectTimeoutMs, readTimeoutMs, configuredCookie, jws);
            logger.info(
                    "[correlationId:{}] MantraCare exchange success: endpoint={}{} elapsedMs={}",
                    correlationId,
                    apiBaseUrl,
                    setUserPath,
                    System.currentTimeMillis() - startedAt);
            return url;
        } catch (ResourceAccessException first) {
            if (isLikelyReadTimeout(first)) {
                logger.warn(
                        "[correlationId:{}] MantraCare read timed out (no retry; repeating same token can yield HTTP 400 from partner): endpoint={}{} elapsedMs={} reason={}",
                        correlationId,
                        apiBaseUrl,
                        setUserPath,
                        System.currentTimeMillis() - startedAt,
                        first.getMessage());
                throw new MantraCareException("MantraCare read timed out", first);
            }
            logger.warn(
                    "[correlationId:{}] MantraCare request failed (attempt=1, retrying): endpoint={}{} elapsedMs={} reason={}",
                    correlationId,
                    apiBaseUrl,
                    setUserPath,
                    System.currentTimeMillis() - startedAt,
                    first.getMessage());
            try {
                long retryStartedAt = System.currentTimeMillis();
                String url = postAndParse(
                        createRestTemplate(connectTimeoutMs, readTimeoutMs),
                        apiBaseUrl,
                        setUserPath,
                        connectTimeoutMs,
                        readTimeoutMs,
                        configuredCookie,
                        jws);
                logger.info(
                        "[correlationId:{}] MantraCare exchange success after retry: endpoint={}{} elapsedMs={}",
                        correlationId,
                        apiBaseUrl,
                        setUserPath,
                        System.currentTimeMillis() - retryStartedAt);
                return url;
            } catch (ResourceAccessException second) {
                throw new MantraCareException("MantraCare unreachable after retry", second);
            }
        } catch (MantraCareException e) {
            logger.warn(
                    "[correlationId:{}] MantraCare exchange failed: endpoint={}{} elapsedMs={} reason={}",
                    correlationId,
                    apiBaseUrl,
                    setUserPath,
                    System.currentTimeMillis() - startedAt,
                    e.getMessage());
            throw e;
        }
    }

    /**
     * Exposed for tests with a {@link RestTemplate} bound to {@link org.springframework.test.web.client.MockRestServiceServer};
     * production code uses {@link #exchangeTokenForRedirectUrl(String, String, int, int, String, String)}.
     */
    public String postAndParse(
            RestTemplate restTemplate,
            String apiBaseUrl,
            String setUserPath,
            int connectTimeoutMs,
            int readTimeoutMs,
            String cookieToSend,
            String jws) {
        String correlationId = MDC.get("correlationId");
        String url = apiBaseUrl + setUserPath;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cookieToSend != null && !cookieToSend.isBlank()) {
            headers.add(HttpHeaders.COOKIE, cookieToSend);
        }
        logger.info(
                "[correlationId:{}] MantraCare request prepared: endpoint={}{} hasCookie={} source={} connectTimeoutMs={} readTimeoutMs={}",
                correlationId,
                apiBaseUrl,
                setUserPath,
                cookieToSend != null && !cookieToSend.isBlank(),
                resolveSource(cookieToSend),
                connectTimeoutMs,
                readTimeoutMs);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("token", jws), headers);
        return doPost(restTemplate, url, entity);
    }

    private static RestTemplate createRestTemplate(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

    @SuppressWarnings("null")
    private static String doPost(RestTemplate restTemplate, String url, HttpEntity<Map<String, String>> entity) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                logger.warn(
                        "[correlationId:{}] MantraCare non-2xx/empty response: status={} bodyPreview={}",
                        MDC.get("correlationId"),
                        response.getStatusCode(),
                        previewBody(body));
                throw new MantraCareException("MantraCare returned status " + response.getStatusCode());
            }
            logger.info(
                    "[correlationId:{}] MantraCare response received: status={} bodyKeys={}",
                    MDC.get("correlationId"),
                    response.getStatusCode(),
                    body.keySet());
            Object redirectUrl = body.get("redirect_url");
            if (!(redirectUrl instanceof String s) || s.isBlank()) {
                throw new MantraCareException("MantraCare did not return redirect_url");
            }
            return s;
        } catch (HttpStatusCodeException e) {
            String bodyPreview = truncate(e.getResponseBodyAsString());
            logger.warn(
                    "[correlationId:{}] MantraCare HTTP {} {} body={}",
                    MDC.get("correlationId"),
                    e.getStatusCode(),
                    e.getStatusText(),
                    bodyPreview);
            throw new MantraCareException("MantraCare API error: " + e.getStatusCode() + " body=" + bodyPreview, e);
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 500 ? body.substring(0, 500) + "…" : body;
    }

    private static String previewBody(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return "{}";
        }
        String text = body.toString();
        return text.length() > 500 ? text.substring(0, 500) + "…" : text;
    }

    private static String normalizeCookie(String rawCookie) {
        if (rawCookie == null || rawCookie.isBlank()) {
            return null;
        }
        Matcher matcher = CONNECT_SID_PATTERN.matcher(rawCookie.trim());
        if (!matcher.find()) {
            return rawCookie.trim();
        }
        return "connect.sid=" + matcher.group(2);
    }

    private static String resolveSource(String cookieToSend) {
        if (cookieToSend == null || cookieToSend.isBlank()) {
            return "none";
        }
        return "configured";
    }

    /**
     * Retrying the same JWT after a read timeout often yields HTTP 400 from MantraCare (token/session
     * consumed while the client stopped waiting). Used to skip automatic retry for read timeouts only.
     */
    static boolean isLikelyReadTimeout(ResourceAccessException ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException ste) {
                String m = ste.getMessage();
                if (m != null && m.toLowerCase(Locale.ROOT).contains("read")) {
                    return true;
                }
            }
            String msg = t.getMessage();
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains("read timed out")) {
                return true;
            }
        }
        return false;
    }
}
