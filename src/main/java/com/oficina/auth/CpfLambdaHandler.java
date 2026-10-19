package com.oficina.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/** Handler real de API Gateway HTTP API (payload v2) para AWS Lambda Java. */
public class CpfLambdaHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final CpfAuthService service;

    public CpfLambdaHandler() {
        this(new CpfAuthService(
                new LambdaClienteAuthRepository(
                        requiredEnv("SPRING_DATASOURCE_URL"),
                        requiredEnv("SPRING_DATASOURCE_USERNAME"),
                        requiredEnv("SPRING_DATASOURCE_PASSWORD")),
                requiredEnv("SECURITY_JWT_SECRET"), "oficina-auth", "oficina-api"));
    }

    CpfLambdaHandler(CpfAuthService service) {
        this.service = service;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String correlationId = event != null && event.getRequestContext() != null
                ? event.getRequestContext().getRequestId() : null;
        try {
            if (event == null || event.getBody() == null) return loggedResponse(400, "{\"error\":\"CPF obrigatório\"}", correlationId, context);
            AuthController.CpfRequest request = JSON.readValue(event.getBody(), AuthController.CpfRequest.class);
            String token = service.authenticate(request.cpf());
            return loggedResponse(200, JSON.writeValueAsString(new AuthController.TokenResponse(token, "Bearer", 900)), correlationId, context);
        } catch (CpfAuthService.AuthException ex) {
            return loggedResponse(401, "{\"error\":\"CPF não autorizado\"}", correlationId, context);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return loggedResponse(400, "{\"error\":\"JSON inválido\"}", correlationId, context);
        } catch (RuntimeException ex) {
            return loggedResponse(500, "{\"error\":\"Erro interno\"}", correlationId, context);
        }
    }

    private static APIGatewayV2HTTPResponse loggedResponse(int status, String body, String correlationId, Context context) {
        if (context != null) context.getLogger().log("{\"event\":\"auth_cpf\",\"status\":" + status +
                ",\"requestId\":\"" + (correlationId == null ? context.getAwsRequestId() : correlationId) + "\"}");
        Map<String, String> headers = new java.util.HashMap<>(Map.of("Content-Type", "application/json", "Cache-Control", "no-store"));
        if (correlationId != null) headers.put("X-Correlation-Id", correlationId);
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(headers)
                .withBody(body)
                .build();
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " não configurado");
        return value;
    }
}
