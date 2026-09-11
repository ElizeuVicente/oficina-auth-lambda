# Oficina Auth Lambda

Base local para a Function Serverless de autenticação por CPF. Não cria recursos AWS nem armazena segredos.

## Responsabilidade

1. Validar CPF; 2. consultar cliente `ATIVO` no PostgreSQL; 3. emitir JWT curto com `sub`, CPF, papel, emissor e audiência. A consulta e a emissão JWT serão implementadas após a migration de status do cliente no repositório de banco.

## Local

```bash
mvn verify
```

Ao configurar remoto, criar environments `homolog` e `prod`, proteger `main` e adicionar `soat-architecture`. O adaptador Lambda/API Gateway e o Terraform serão ligados ao repositório de infraestrutura; nenhum deploy é disparado por este esqueleto.
