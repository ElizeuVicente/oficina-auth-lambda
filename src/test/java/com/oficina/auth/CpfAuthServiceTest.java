package com.oficina.auth;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CpfAuthServiceTest {
    private static final String SECRET = "segredo-local-de-teste-com-mais-de-trinta-e-dois-caracteres";
    @Test void issuesTokenForActiveClient() {
        var repository = (ClienteAuthRepository) cpf -> Optional.of(new ClienteAuthRepository.ClienteAuth(UUID.randomUUID(), cpf, "ATIVO"));
        assertNotNull(new CpfAuthService(repository, SECRET, "issuer", "audience").authenticate("529.982.247-25"));
    }
    @Test void rejectsInactiveClient() {
        var repository = (ClienteAuthRepository) cpf -> Optional.of(new ClienteAuthRepository.ClienteAuth(UUID.randomUUID(), cpf, "INATIVO"));
        assertThrows(CpfAuthService.AuthException.class, () -> new CpfAuthService(repository, SECRET, "issuer", "audience").authenticate("52998224725"));
    }
}
