package com.group8.communication.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Group 5-style RS256 tokens (kid, iss, aud, roles) are verified through the JWKS endpoint. */
class JwtFilterTest {
    private static final String ISSUER = "university-identity-service";
    private static final String AUDIENCE = "university-services-platform";
    private static final String KID = "group5-key-1";

    private KeyPair keys;
    private JwtFilter filter;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keys.getPublic();

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.once(), requestTo("http://group5/.well-known/jwks.json"))
                .andRespond(withSuccess("{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"" + KID + "\",\"use\":\"sig\",\"alg\":\"RS256\","
                        + "\"n\":\"" + base64Url(publicKey.getModulus()) + "\",\"e\":\"" + base64Url(publicKey.getPublicExponent())
                        + "\"}]}", MediaType.APPLICATION_JSON));
        filter = new JwtFilter(new JwksKeyProvider(builder, "http://group5/.well-known/jwks.json"), ISSUER, AUDIENCE);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String token(String audience) {
        return Jwts.builder()
                .header().keyId(KID).and()
                .subject("usr-student-001")
                .issuer(ISSUER)
                .audience().add(audience).and()
                .claim("roles", List.of("STUDENT", "EVENT_ORGANIZER"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(keys.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private Authentication authenticate(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        SecurityContextHolder.clearContext();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void acceptsGroup5TokenWithStringUserIdAndRoles() throws Exception {
        Authentication authentication = authenticate(token(AUDIENCE));

        assertNotNull(authentication);
        assertEquals("usr-student-001", authentication.getName());
        assertEquals(List.of("ROLE_STUDENT", "ROLE_EVENT_ORGANIZER"),
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @Test
    void cachesKeysAcrossRequests() throws Exception {
        assertNotNull(authenticate(token(AUDIENCE)));
        assertNotNull(authenticate(token(AUDIENCE)));
        server.verify();
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        assertNull(authenticate(token("another-platform")));
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String token = token(AUDIENCE);
        assertNull(authenticate(token.substring(0, token.length() - 4) + "AAAA"));
    }
}
