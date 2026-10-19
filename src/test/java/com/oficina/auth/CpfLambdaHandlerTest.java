package com.oficina.auth;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CpfLambdaHandlerTest {
    private static final String SECRET = "segredo-local-de-teste-com-mais-de-trinta-e-dois-caracteres";

    @Test
    void emiteJwtSomenteParaClienteAtivo() {
        var repo = (ClienteAuthRepository) cpf -> Optional.of(new ClienteAuthRepository.ClienteAuth(UUID.randomUUID(), cpf, "ATIVO"));
        var handler = new CpfLambdaHandler(new CpfAuthService(repo, SECRET, "oficina-auth", "oficina-api"));
        var response = handler.handleRequest(APIGatewayV2HTTPEvent.builder().withBody("{\"cpf\":\"529.982.247-25\"}").build(), null);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("accessToken"));
    }

    @Test
    void rejeitaCpfInvalidoEClienteInativo() {
        var repo = (ClienteAuthRepository) cpf -> Optional.of(new ClienteAuthRepository.ClienteAuth(UUID.randomUUID(), cpf, "INATIVO"));
        var handler = new CpfLambdaHandler(new CpfAuthService(repo, SECRET, "oficina-auth", "oficina-api"));
        assertEquals(401, handler.handleRequest(APIGatewayV2HTTPEvent.builder().withBody("{\"cpf\":\"123\"}").build(), null).getStatusCode());
        assertEquals(401, handler.handleRequest(APIGatewayV2HTTPEvent.builder().withBody("{\"cpf\":\"52998224725\"}").build(), null).getStatusCode());
        assertEquals(400, handler.handleRequest(APIGatewayV2HTTPEvent.builder().withBody("{").build(), null).getStatusCode());
    }
}
