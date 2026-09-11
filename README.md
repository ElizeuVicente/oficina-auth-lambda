# Oficina Auth Lambda

Base local para a Function Serverless de autenticação por CPF. Não cria recursos AWS nem armazena segredos.

## Responsabilidade

1. Validar CPF; 2. consultar cliente `ATIVO` no PostgreSQL; 3. emitir JWT de 15 minutos com `sub`, CPF, papel, emissor e audiência.

## Local

```bash
mvn verify
```

Com a API e o PostgreSQL locais ativos, execute na porta `8081` e use `POST /auth/cpf` com `{"cpf":"529.982.247-25"}`. Defina `SECURITY_JWT_SECRET` com pelo menos 32 caracteres; ela deve ser igual à secret da API.

Ao configurar remoto, criar environments `homolog` e `prod`, proteger `main` e adicionar `soat-architecture`. O adaptador Lambda/API Gateway e o Terraform serão ligados ao repositório de infraestrutura; nenhum deploy é disparado por este esqueleto.
