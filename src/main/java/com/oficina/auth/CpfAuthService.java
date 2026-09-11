package com.oficina.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CpfAuthService {
    private final ClienteAuthRepository clientes;
    private final SecretKey key;
    private final String issuer;
    private final String audience;
    public CpfAuthService(ClienteAuthRepository clientes, @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer:oficina-auth}") String issuer,
            @Value("${security.jwt.audience:oficina-api}") String audience) {
        if (secret == null || secret.length() < 32) throw new IllegalArgumentException("SECURITY_JWT_SECRET deve ter ao menos 32 caracteres");
        this.clientes = clientes; this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer; this.audience = audience;
    }
    public String authenticate(String cpf) {
        String normalized = cpf == null ? null : cpf.replaceAll("\\D", "");
        if (!CpfValidator.isValid(normalized)) throw new AuthException("CPF inválido");
        ClienteAuthRepository.ClienteAuth cliente = clientes.findByCpf(normalized)
                .orElseThrow(() -> new AuthException("Cliente não encontrado"));
        if (!"ATIVO".equals(cliente.status())) throw new AuthException("Cliente inativo");
        Date now = new Date(); Date expiry = new Date(now.getTime() + Duration.ofMinutes(15).toMillis());
        return Jwts.builder().subject(cliente.id().toString()).issuer(issuer).audience().add(audience).and()
                .claim("cpf", cliente.cpf()).claim("role", "CLIENTE").issuedAt(now).expiration(expiry).signWith(key).compact();
    }
    public static class AuthException extends RuntimeException { public AuthException(String message) { super(message); } }
}
