package com.group8.communication.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    ObjectMapper jwtHeaderObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwt) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/notifications/trigger").permitAll()
                        .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
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
    private final ObjectMapper objectMapper;
    private final String issuer;
    private final String audience;

    JwtFilter(
            JwksKeyProvider keyProvider,
            ObjectMapper objectMapper,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience) {
        this.keyProvider = keyProvider;
        this.objectMapper = objectMapper;
        this.issuer = issuer;
        this.audience = audience;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                String[] tokenParts = token.split("\\.");
                if (tokenParts.length != 3) throw new IllegalArgumentException("Malformed JWT");
                JsonNode tokenHeader = objectMapper.readTree(
                        Base64.getUrlDecoder().decode(tokenParts[0]));
                if (!"RS256".equals(tokenHeader.path("alg").asText())) {
                    throw new IllegalArgumentException("Unsupported JWT algorithm");
                }
                String keyId = tokenHeader.path("kid").asText();
                Jws<Claims> parsed = Jwts.parser()
                        .verifyWith(keyProvider.keyFor(keyId))
                        .requireIssuer(issuer)
                        .requireAudience(audience)
                        .build()
                        .parseSignedClaims(token);
                Claims claims = parsed.getPayload();
                String subject = claims.getSubject();
                List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
                Object roles = claims.get("roles");
                if (roles instanceof List<?> roleList) {
                    roleList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                            .forEach(authorities::add);
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(subject, null, authorities));
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
