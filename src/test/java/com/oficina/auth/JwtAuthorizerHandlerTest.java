package com.oficina.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JwtAuthorizerHandlerTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private final JwtAuthorizerHandler handler = new JwtAuthorizerHandler(SECRET);

    @Test void autorizaClienteValidoENegaAusenteOuExpirado() {
        String valid = token(new Date(System.currentTimeMillis() + 60_000), true);
        String expired = token(new Date(System.currentTimeMillis() - 60_000), true);
        assertEquals(true, handler.handleRequest(Map.of("headers", Map.of("authorization", "Bearer " + valid)), null).get("isAuthorized"));
        assertEquals(false, handler.handleRequest(Map.of("headers", Map.of("authorization", "Bearer " + expired)), null).get("isAuthorized"));
        assertEquals(false, handler.handleRequest(Map.of("headers", Map.of()), null).get("isAuthorized"));
    }

    @Test void aceitaTokenAdministrativoAssinadoPelaApi() {
        assertEquals(true, handler.handleRequest(Map.of("headers", Map.of("Authorization", "Bearer " + token(new Date(System.currentTimeMillis() + 60_000), false))), null).get("isAuthorized"));
    }

    @Test void rejeitaTokenAssinadoPeloOutroAmbiente() {
        JwtAuthorizerHandler homolog = new JwtAuthorizerHandler("abcdef0123456789abcdef0123456789");
        assertEquals(false, homolog.handleRequest(Map.of("headers", Map.of("authorization", "Bearer " + token(new Date(System.currentTimeMillis() + 60_000), true))), null).get("isAuthorized"));
    }

    private static String token(Date expiration, boolean client) {
        var builder = Jwts.builder().subject(client ? "12345678909" : "admin")
                .expiration(expiration);
        if (client) builder.issuer("oficina-auth").audience().add("oficina-api").and().claim("role", "CLIENTE");
        return builder.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
