output "gateway_url" {
  value = aws_apigatewayv2_api.oficina.api_endpoint
}

output "lambda_name" {
  value = aws_lambda_function.cpf["prod"].function_name
}

output "lambda_names" {
  value = { for env, function in aws_lambda_function.cpf : env => function.function_name }
}
