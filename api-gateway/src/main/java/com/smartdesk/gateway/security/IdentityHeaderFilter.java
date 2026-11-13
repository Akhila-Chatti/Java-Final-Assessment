package com.smartdesk.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Turns the verified JWT into X-User-* headers for the services behind the gateway, and removes any
 * X-User-* header the client sent itself, so nobody can fake their identity.
 */
public class IdentityHeaderFilter extends OncePerRequestFilter {

    private static final String PREFIX = "x-user-";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Map<String, String> identity = new HashMap<>();
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
            Jwt jwt = auth.getToken();
            identity.put("x-user-id", jwt.getSubject());
            putIfPresent(identity, "x-user-email", jwt.getClaimAsString("email"));
            putIfPresent(identity, "x-user-name", jwt.getClaimAsString("name"));
            putIfPresent(identity, "x-user-role", jwt.getClaimAsString("role"));
            putIfPresent(identity, "x-user-dept", jwt.getClaimAsString("dept"));
        }
        chain.doFilter(new IdentityRequest(request, identity), response);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null) map.put(key, value);
    }

    private static final class IdentityRequest extends HttpServletRequestWrapper {
        private final Map<String, String> identity;

        IdentityRequest(HttpServletRequest request, Map<String, String> identity) {
            super(request);
            this.identity = identity;
        }

        private static boolean isIdentity(String name) {
            return name.toLowerCase(Locale.ROOT).startsWith(PREFIX);
        }

        @Override
        public String getHeader(String name) {
            return isIdentity(name) ? identity.get(name.toLowerCase(Locale.ROOT)) : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (!isIdentity(name)) return super.getHeaders(name);
            String value = getHeader(name);
            return Collections.enumeration(value == null ? List.of() : List.of(value));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>(identity.keySet());
            Collections.list(super.getHeaderNames()).stream().filter(n -> !isIdentity(n)).forEach(names::add);
            return Collections.enumeration(names);
        }
    }
}
