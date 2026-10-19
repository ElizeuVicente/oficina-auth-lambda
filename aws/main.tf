resource "aws_cloudwatch_log_group" "lambda" {
  for_each          = toset(["prod", "homolog"])
  name              = "/aws/lambda/oficina-fase3-auth-${each.key}"
  retention_in_days = 1
}

resource "aws_lambda_function" "cpf" {
  for_each         = toset(["prod", "homolog"])
  function_name    = "oficina-fase3-auth-${each.key}"
  role             = var.lambda_role_arn
  runtime          = "java17"
  handler          = "com.oficina.auth.CpfLambdaHandler::handleRequest"
  filename         = var.lambda_artifact_path
  source_code_hash = filebase64sha256(var.lambda_artifact_path)
  memory_size      = 512
  timeout          = 15

  vpc_config {
    subnet_ids         = var.private_subnet_ids
    security_group_ids = [var.lambda_security_group_id]
  }

  environment {
    variables = {
      SPRING_DATASOURCE_URL      = "jdbc:postgresql://${var.db_endpoint}:5432/oficina?currentSchema=oficina_${each.key}"
      SPRING_DATASOURCE_USERNAME = "oficina"
      SPRING_DATASOURCE_PASSWORD = var.db_password
      SECURITY_JWT_SECRET        = var.jwt_secrets[each.key]
    }
  }

  depends_on = [aws_cloudwatch_log_group.lambda]

  # O Terraform cria a infraestrutura; cada branch publica seu código na
  # própria Lambda. Evita que o apply de main sobrescreva homologação.
  lifecycle {
    ignore_changes = [filename, source_code_hash]
  }
}

resource "aws_cloudwatch_log_group" "authorizer" {
  for_each          = toset(["prod", "homolog"])
  name              = "/aws/lambda/oficina-fase3-authorizer-${each.key}"
  retention_in_days = 1
}

resource "aws_lambda_function" "authorizer" {
  for_each         = toset(["prod", "homolog"])
  function_name    = "oficina-fase3-authorizer-${each.key}"
  role             = var.lambda_role_arn
  runtime          = "java17"
  handler          = "com.oficina.auth.JwtAuthorizerHandler::handleRequest"
  filename         = var.lambda_artifact_path
  source_code_hash = filebase64sha256(var.lambda_artifact_path)
  memory_size      = 256
  timeout          = 10
  environment {
    variables = { SECURITY_JWT_SECRET = var.jwt_secrets[each.key] }
  }
  depends_on = [aws_cloudwatch_log_group.authorizer]

  lifecycle {
    ignore_changes = [filename, source_code_hash]
  }
}

resource "aws_apigatewayv2_api" "oficina" {
  name          = "oficina-fase3-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "cpf" {
  for_each               = aws_lambda_function.cpf
  api_id                 = aws_apigatewayv2_api.oficina.id
  integration_type       = "AWS_PROXY"
  integration_uri        = each.value.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "cpf" {
  for_each  = aws_apigatewayv2_integration.cpf
  api_id    = aws_apigatewayv2_api.oficina.id
  route_key = each.key == "prod" ? "POST /auth/cpf" : "POST /homolog/auth/cpf"
  target    = "integrations/${each.value.id}"
}

resource "aws_lambda_permission" "gateway" {
  for_each      = aws_lambda_function.cpf
  statement_id  = "AllowApiGatewayInvoke-${each.key}"
  action        = "lambda:InvokeFunction"
  function_name = each.value.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.oficina.execution_arn}/*/POST/${each.key == "prod" ? "auth/cpf" : "homolog/auth/cpf"}"
}

resource "aws_apigatewayv2_authorizer" "jwt" {
  for_each                          = aws_lambda_function.authorizer
  api_id                            = aws_apigatewayv2_api.oficina.id
  authorizer_type                   = "REQUEST"
  name                              = "oficina-jwt-${each.key}"
  authorizer_uri                    = each.value.invoke_arn
  authorizer_payload_format_version = "2.0"
  enable_simple_responses           = true
  identity_sources                  = ["$request.header.Authorization"]
  authorizer_result_ttl_in_seconds  = 0
}

resource "aws_lambda_permission" "authorizer" {
  for_each      = aws_lambda_function.authorizer
  statement_id  = "AllowGatewayAuthorizer-${each.key}"
  action        = "lambda:InvokeFunction"
  function_name = each.value.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.oficina.execution_arn}/authorizers/${aws_apigatewayv2_authorizer.jwt[each.key].id}"
}

resource "aws_security_group" "vpc_link" {
  count       = var.backend_listener_arn == "" ? 0 : 1
  name_prefix = "oficina-fase3-vpc-link-"
  description = "API Gateway para listener interno EKS"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_egress_rule" "vpc_link_http" {
  count             = var.backend_listener_arn == "" ? 0 : 1
  security_group_id = aws_security_group.vpc_link[0].id
  cidr_ipv4         = "10.42.0.0/16"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_apigatewayv2_vpc_link" "eks" {
  count              = var.backend_listener_arn == "" ? 0 : 1
  name               = "oficina-fase3-eks"
  subnet_ids         = var.private_subnet_ids
  security_group_ids = [aws_security_group.vpc_link[0].id]
}

resource "aws_apigatewayv2_integration" "api" {
  count                  = var.backend_listener_arn == "" ? 0 : 1
  api_id                 = aws_apigatewayv2_api.oficina.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = var.backend_listener_arn
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.eks[0].id
  payload_format_version = "1.0"

  request_parameters = {
    "overwrite:path"                    = "$request.path"
    "overwrite:header.X-Correlation-Id" = "$context.requestId"
  }
}

resource "aws_apigatewayv2_route" "api" {
  for_each           = var.backend_listener_arn == "" ? toset([]) : toset(["prod", "homolog"])
  api_id             = aws_apigatewayv2_api.oficina.id
  route_key          = each.key == "prod" ? "ANY /api/{proxy+}" : "ANY /homolog/api/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.api[0].id}"
  authorization_type = "CUSTOM"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt[each.key].id
}

# O login administrativo precisa ser alcançável antes de possuir um JWT.
resource "aws_apigatewayv2_route" "admin_login" {
  for_each  = var.backend_listener_arn == "" ? toset([]) : toset(["prod", "homolog"])
  api_id    = aws_apigatewayv2_api.oficina.id
  route_key = each.key == "prod" ? "POST /api/auth/login" : "POST /homolog/api/auth/login"
  target    = "integrations/${aws_apigatewayv2_integration.api[0].id}"
}

# Integração de orçamento usa seu próprio segredo e validação na API.
resource "aws_apigatewayv2_route" "budget_webhook" {
  for_each  = var.backend_listener_arn == "" ? toset([]) : toset(["prod", "homolog"])
  api_id    = aws_apigatewayv2_api.oficina.id
  route_key = each.key == "prod" ? "POST /api/ordens-servico/{id}/orcamento/notificacoes" : "POST /homolog/api/ordens-servico/{id}/orcamento/notificacoes"
  target    = "integrations/${aws_apigatewayv2_integration.api[0].id}"
}

resource "aws_cloudwatch_log_group" "gateway" {
  name              = "/aws/apigateway/oficina-fase3"
  retention_in_days = 1
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.oficina.id
  name        = "$default"
  auto_deploy = true

  default_route_settings {
    throttling_burst_limit = 20
    throttling_rate_limit  = 10
  }

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.gateway.arn
    format = jsonencode({
      requestId = "$context.requestId"
      routeKey  = "$context.routeKey"
      status    = "$context.status"
      latency   = "$context.responseLatency"
    })
  }
}
