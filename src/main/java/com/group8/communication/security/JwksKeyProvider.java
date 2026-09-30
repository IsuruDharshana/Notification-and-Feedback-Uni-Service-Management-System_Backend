package com.group8.communication.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwksKeyProvider {
    private final RestClient restClient;
    private final String jwksUrl;
    private volatile Map<String, RSAPublicKey> cachedKeys = Map.of();

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
            cachedKeys = loadKeys();
            key = cachedKeys.get(keyId);
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
            if (!"RSA".equals(jwk.path("kty").asText())
                    || jwk.path("kid").asText().isBlank()
                    || jwk.path("n").asText().isBlank()
                    || jwk.path("e").asText().isBlank()) {
                continue;
            }
            try {
                byte[] modulusBytes = Base64.getUrlDecoder().decode(jwk.path("n").asText());
                byte[] exponentBytes = Base64.getUrlDecoder().decode(jwk.path("e").asText());
                RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(
                                new BigInteger(1, modulusBytes),
                                new BigInteger(1, exponentBytes)));
                result.put(jwk.path("kid").asText(), publicKey);
            } catch (Exception exception) {
                throw new IllegalArgumentException("Invalid RSA key in JWKS", exception);
            }
        }
        return Map.copyOf(result);
    }
}
