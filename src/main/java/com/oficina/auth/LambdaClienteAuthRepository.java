package com.oficina.auth;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** Acesso JDBC mínimo para a função; não inicia o servidor Spring Boot. */
public class LambdaClienteAuthRepository implements ClienteAuthRepository {
    private final String url;
    private final String username;
    private final String password;

    public LambdaClienteAuthRepository(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public Optional<ClienteAuth> findByCpf(String cpf) {
        String sql = "select id, cpf_cnpj, status from cliente where cpf_cnpj = ?";
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, cpf);
            try (var result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new ClienteAuth(
                        result.getObject("id", UUID.class),
                        result.getString("cpf_cnpj"),
                        result.getString("status")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao consultar cliente", ex);
        }
    }
}
