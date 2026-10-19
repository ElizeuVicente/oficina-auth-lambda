package com.oficina.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/** Authorizer REQUEST v2 para as rotas privadas do HTTP API. A API repete a checagem de papel/posse. */
public class JwtAuthorizerHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final String secret;

    public JwtAuthorizerHandler() { this(requiredEnv("SECURITY_JWT_SECRET")); }

    JwtAuthorizerHandler(String secret) { this.secret = secret; }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            Object rawHeaders = event.get("headers");
            if (!(rawHeaders instanceof Map<?, ?> headers)) return deny();
            String authorization = null;
            for (var entry : headers.entrySet()) {
                if ("authorization".equals(String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT))) {
                    authorization = String.valueOf(entry.getValue());
                    break;
                }
            }
            if (authorization == null || !authorization.startsWith("Bearer ")) return deny();
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build().parseSignedClaims(authorization.substring(7)).getPayload();
            if (claims.getExpiration() == null || !claims.getExpiration().after(new Date())
                    || claims.getSubject() == null || claims.getSubject().isBlank()) return deny();
            // Tokens da Lambda são de cliente. Tokens administrativos vêm da API e não têm claim role.
            if (claims.containsKey("role") && !("CLIENTE".equals(claims.get("role"))
                    && "oficina-auth".equals(claims.getIssuer())
                    && claims.getAudience() != null && claims.getAudience().contains("oficina-api"))) return deny();
            return Map.of("isAuthorized", true);
        } catch (JwtException | IllegalArgumentException | ClassCastException ex) {
            return deny();
        }
    }

    private static Map<String, Object> deny() { return Map.of("isAuthorized", false); }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " não configurado");
        return value;
    }
}
