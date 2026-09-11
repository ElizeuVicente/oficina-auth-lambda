package com.oficina.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private final CpfAuthService service;
    public AuthController(CpfAuthService service) { this.service = service; }
    @PostMapping("/auth/cpf")
    public ResponseEntity<TokenResponse> authenticate(@RequestBody CpfRequest request) {
        try { return ResponseEntity.ok(new TokenResponse(service.authenticate(request.cpf()), "Bearer", 900)); }
        catch (CpfAuthService.AuthException e) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
    }
    public record CpfRequest(String cpf) { }
    public record TokenResponse(String accessToken, String tokenType, int expiresIn) { }
}
