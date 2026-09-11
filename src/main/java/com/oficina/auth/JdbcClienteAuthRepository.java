package com.oficina.auth;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClienteAuthRepository implements ClienteAuthRepository {
    private final JdbcTemplate jdbc;
    public JdbcClienteAuthRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<ClienteAuth> findByCpf(String cpf) {
        return jdbc.query("select id, cpf_cnpj, status from cliente where cpf_cnpj = ?", rs ->
                rs.next() ? Optional.of(new ClienteAuth(rs.getObject("id", java.util.UUID.class),
                        rs.getString("cpf_cnpj"), rs.getString("status"))) : Optional.empty(), cpf);
    }
}
