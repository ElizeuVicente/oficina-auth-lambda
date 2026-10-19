variable "region" {
  type    = string
  default = "us-east-1"
}

variable "lambda_role_arn" {
  description = "ARN do LabRole preexistente no Learner Lab (trust para lambda.amazonaws.com)."
  type        = string
}

variable "private_subnet_ids" {
  description = "Outputs private_subnet_ids da infraestrutura EKS."
  type        = list(string)
}

variable "vpc_id" {
  type = string
}

variable "lambda_security_group_id" {
  description = "Output lambda_security_group_id do módulo RDS."
  type        = string
}

variable "db_endpoint" {
  description = "Output db_endpoint do módulo RDS."
  type        = string
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "jwt_secrets" {
  description = "Chaves distintas de homolog e prod, cada uma compartilhada somente com a API do mesmo ambiente."
  type        = map(string)
  sensitive   = true

  validation {
    condition     = length(var.jwt_secrets["prod"]) >= 32 && length(var.jwt_secrets["homolog"]) >= 32 && var.jwt_secrets["prod"] != var.jwt_secrets["homolog"]
    error_message = "Forneça chaves distintas de ao menos 32 caracteres para prod e homolog."
  }
}

variable "lambda_artifact_path" {
  description = "JAR -lambda.jar gerado por mvn verify antes do terraform plan/apply."
  type        = string
}

variable "backend_listener_arn" {
  description = "ARN opcional do listener NLB/ALB interno do EKS. Vazio deixa somente /auth/cpf."
  type        = string
  default     = ""
}
