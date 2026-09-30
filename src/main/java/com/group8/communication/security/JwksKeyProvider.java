package com.group8.communication.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Group 5 signing keys from the JWKS endpoint. Keys are cached and re-fetched only when a token
 * arrives with an unknown kid (key rotation), at most once per {@link #MIN_REFRESH_INTERVAL}.
 *
 * The response is read as a Jackson 3 {@link JsonNode}: Spring Boot 4's RestClient converts JSON
 * with Jackson 3, so asking it for a Jackson 2 (com.fasterxml) JsonNode fails and no key is ever loaded.
 */
@Component
public class JwksKeyProvider {
    static final Duration MIN_REFRESH_INTERVAL = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final String jwksUrl;
    private volatile Map<String, RSAPublicKey> cachedKeys = Map.of();
    private volatile Instant lastRefresh = Instant.EPOCH;

    public JwksKeyProvider(
            RestClient.Builder restClientBuilder,
            @Value("${jwt.jwks-url}") String jwksUrl) {
        this.restClient = restClientBuilder.build();
        this.jwksUrl = jwksUrl;
    }

    public RSAPublicKey keyFor(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("JWT key id is missing");
        }
        RSAPublicKey key = cachedKeys.get(keyId);
        if (key != null) return key;

        synchronized (this) {
            key = cachedKeys.get(keyId);
            if (key != null) return key;
            if (cachedKeys.isEmpty() || Instant.now().isAfter(lastRefresh.plus(MIN_REFRESH_INTERVAL))) {
                cachedKeys = loadKeys();
                lastRefresh = Instant.now();
                key = cachedKeys.get(keyId);
            }
        }
        if (key == null) throw new IllegalArgumentException("JWT key id is unknown");
        return key;
    }

    private Map<String, RSAPublicKey> loadKeys() {
        JsonNode document = restClient.get().uri(jwksUrl).retrieve().body(JsonNode.class);
        if (document == null || !document.path("keys").isArray()) {
            throw new IllegalArgumentException("Invalid JWKS response");
        }

        Map<String, RSAPublicKey> result = new HashMap<>();
        for (JsonNode jwk : document.path("keys")) {
            String kid = text(jwk, "kid");
            String modulus = text(jwk, "n");
            String exponent = text(jwk, "e");
            if (!"RSA".equals(text(jwk, "kty")) || kid.isBlank() || modulus.isBlank() || exponent.isBlank()) {
                continue;
            }
            try {
                RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(
                                new BigInteger(1, Base64.getUrlDecoder().decode(modulus)),
                                new BigInteger(1, Base64.getUrlDecoder().decode(exponent))));
                result.put(kid, publicKey);
            } catch (Exception exception) {
                throw new IllegalArgumentException("Invalid RSA key in JWKS", exception);
            }
        }
        return Map.copyOf(result);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isString() ? value.stringValue() : "";
    }
}
