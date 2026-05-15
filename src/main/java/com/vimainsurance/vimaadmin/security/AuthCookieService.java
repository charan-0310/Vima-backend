package com.vimainsurance.vimaadmin.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * F-03 — Helper for issuing and clearing the httpOnly auth cookies.
 *
 * Reserved for a future cookie-based session model. The portal currently stores Keycloak tokens
 * in browser storage; this helper documents intended cookie semantics for security review.
 *
 * Cookie names:
 *   vima_at  — short-lived access token  (Path=/, HttpOnly, Secure, SameSite=Strict)
 *   vima_rt  — long-lived refresh token  (Path=/api/v1/auth, HttpOnly, Secure, SameSite=Strict)
 *
 * SameSite=Strict means the cookies are not sent on top-level navigations from third-party sites,
 * giving CSRF defence. The frontend must use credentials: 'include' on every API call.
 */
@Service
public class AuthCookieService {

    public static final String ACCESS_COOKIE_NAME = "vima_at";
    public static final String REFRESH_COOKIE_NAME = "vima_rt";

    @Value("${auth.cookie.domain:}")
    private String cookieDomain; // empty = host-only (recommended for api.vimainsurance.com)

    @Value("${auth.cookie.secure:true}")
    private boolean secure;

    @Value("${auth.cookie.same-site:Strict}")
    private String sameSite;

    @Value("${jwt.expiration:900000}")
    private long accessExpirationMs;

    @Value("${jwt.refreshexpiration:604800000}")
    private long refreshExpirationMs;

    public ResponseCookie buildAccessCookie(String token) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(ACCESS_COOKIE_NAME, token)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .maxAge(accessExpirationMs / 1000);
        if (cookieDomain != null && !cookieDomain.isBlank()) b.domain(cookieDomain);
        return b.build();
    }

    public ResponseCookie buildRefreshCookie(String token) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .path("/api/v1/auth")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .maxAge(refreshExpirationMs / 1000);
        if (cookieDomain != null && !cookieDomain.isBlank()) b.domain(cookieDomain);
        return b.build();
    }

    public ResponseCookie clearAccessCookie() {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(ACCESS_COOKIE_NAME, "")
                .path("/").httpOnly(true).secure(secure).sameSite(sameSite).maxAge(0);
        // Match the domain set on the build path so the browser actually removes the cookie.
        if (cookieDomain != null && !cookieDomain.isBlank()) b.domain(cookieDomain);
        return b.build();
    }

    public ResponseCookie clearRefreshCookie() {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .path("/api/v1/auth").httpOnly(true).secure(secure).sameSite(sameSite).maxAge(0);
        if (cookieDomain != null && !cookieDomain.isBlank()) b.domain(cookieDomain);
        return b.build();
    }
}
