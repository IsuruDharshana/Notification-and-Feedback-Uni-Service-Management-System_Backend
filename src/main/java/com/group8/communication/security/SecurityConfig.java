package com.group8.communication.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ProtectedHeader;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwt) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/notifications/trigger").permitAll()
                        .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"code\":\"UNAUTHORIZED\"}");
                        }))
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

@Component
class JwtFilter extends OncePerRequestFilter {
    private final JwksKeyProvider keyProvider;
    private final String issuer;
    private final String audience;

    JwtFilter(
            JwksKeyProvider keyProvider,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience) {
        this.keyProvider = keyProvider;
        this.issuer = issuer;
        this.audience = audience;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parser()
                        .keyLocator(tokenHeader -> {
                            if (!"RS256".equals(tokenHeader.getAlgorithm())
                                    || !(tokenHeader instanceof ProtectedHeader protectedHeader)) {
                                throw new IllegalArgumentException("Unsupported JWT algorithm");
                            }
                            return keyProvider.keyFor(protectedHeader.getKeyId());
                        })
                        .requireIssuer(issuer)
                        .requireAudience(audience)
                        .build()
                        .parseSignedClaims(header.substring(7))
                        .getPayload();
                List<GrantedAuthority> authorities = new ArrayList<>();
                Object roles = claims.get("roles");
                if (roles instanceof List<?> roleList) {
                    roleList.stream()
                            .filter(String.class::isInstance)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .forEach(authorities::add);
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
            } catch (Exception exception) {
                logger.debug("Rejected bearer token: " + exception.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
