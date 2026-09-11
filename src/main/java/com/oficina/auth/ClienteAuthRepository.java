package com.oficina.auth;

import java.util.Optional;
import java.util.UUID;

public interface ClienteAuthRepository {
    Optional<ClienteAuth> findByCpf(String cpf);
    record ClienteAuth(UUID id, String cpf, String status) { }
}
