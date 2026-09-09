package com.group8.communication.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.*; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.web.*; import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey; import java.io.IOException;

@Configuration public class SecurityConfig {
    @Bean SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwt) throws Exception { return http.csrf(c -> c.disable()).authorizeHttpRequests(a -> a.requestMatchers("/api/notifications/trigger").permitAll().requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll().anyRequest().authenticated()).addFilterBefore(jwt, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class).build(); }
}

@Component class JwtFilter extends OncePerRequestFilter {
    private final SecretKey key;
    JwtFilter(@Value("${jwt.secret}") String secret) { key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) try { String subject = Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload().getSubject(); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(subject, null, java.util.List.of())); } catch (Exception ignored) {}
        chain.doFilter(request, response);
    }
}
