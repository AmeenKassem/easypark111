package com.example.demo.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String path = request.getRequestURI();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Useful at DEBUG only
            log.debug("action=jwt_filter skip path={} reason=no_bearer", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.parseClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            String role = (String) claims.get("role");

            log.info("DIAGNOSTIC: Path={}", path);
            log.info("DIAGNOSTIC: Token Subject (expecting ID) = '{}'", userId);
            log.info("DIAGNOSTIC: Token Role (expecting String) = '{}'", role);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                List<GrantedAuthority> authorities = switch (role) {
                    case "DRIVER" -> List.of(new SimpleGrantedAuthority("ROLE_DRIVER"));
                    case "OWNER" -> List.of(new SimpleGrantedAuthority("ROLE_OWNER"));
                    case "BOTH" -> List.of(
                            new SimpleGrantedAuthority("ROLE_DRIVER"),
                            new SimpleGrantedAuthority("ROLE_OWNER"),
                            new SimpleGrantedAuthority("ROLE_BOTH")
                    );
                    default -> List.of();
                };

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                authorities
                        );

                ((UsernamePasswordAuthenticationToken) authentication)
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("action=jwt_auth success path={} userId={} role={}", path, userId, role);
            } else {
                log.debug("action=jwt_auth skip path={} reason=already_authenticated", path);
            }
        } catch (Exception ex) {
            // Do not log token. Log the exception type for debugging.
            log.warn("action=jwt_auth fail path={} reason={}", path, ex.getClass().getSimpleName());
            log.error("CRITICAL AUTH ERROR on path={}", path);
            log.error("Exception Type: {}", ex.getClass().getName());
            log.error("Exception Message: {}", ex.getMessage());
            // IMPORTANT: do not block request here; let Spring Security handle authorization failure later.
        }

        filterChain.doFilter(request, response);
    }
}
