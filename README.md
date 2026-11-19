# Sistema de E-commerce com Promoções Avançadas

API REST de um e-commerce de médio porte, feita em **Java 21 e Spring Boot** para um projeto acadêmico de Design Patterns. Ela cobre catálogo com variações e categorias, estoque com reserva, carrinho, checkout, pedidos com ciclo de vida, pagamentos simulados com idempotência, entrega e frete, notificações, auditoria, relatórios e, principalmente, um **mecanismo extensível de promoções e cupons**.

**Stack:** Spring Boot · Spring Data JPA · PostgreSQL 17 · Flyway · Spring Security (JWT) · OpenAPI/Swagger · JUnit 5 + Testcontainers

## Como executar

Pré-requisitos: **JDK 21+** e **Docker**. Não é preciso instalar o Maven, porque o repositório traz o wrapper `./mvnw`.

```bash
docker compose up -d                                   # sobe o PostgreSQL
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # sobe a API com dados de demonstração
```

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- Usuários de demonstração (senha `Demo1234`): `admin@demo.local`, `cliente@demo.local`, `comercial@demo.local`, `estoque@demo.local`, `promocoes@demo.local`

Testes, que exigem o Docker rodando por causa dos testes de integração com Testcontainers:

```bash
./mvnw test
```

O passo a passo completo está em [`docs/10-execucao.md`](docs/10-execucao.md). Lá estão as variáveis de ambiente, como obter o token, os roteiros de demonstração por etapa e a solução de problemas.

## Documentação

| Documento | Conteúdo |
|---|---|
| [Índice e visão geral](docs/README.md) | Descrição do sistema e mapa dos documentos |
| [01 Requisitos](docs/01-requisitos.md) | Requisitos funcionais e não funcionais |
| [02 Modelo de domínio](docs/02-modelo-dominio.md) | Entidades e diagrama de classes |
| [03 Arquitetura](docs/03-arquitetura.md) | Camadas, módulos e decisões |
| [04 Design Patterns](docs/04-design-patterns.md) | Patterns usados: problema, motivo, classes participantes e o que ficou mais fácil de mudar |
| [05 Regras de negócio](docs/05-regras-negocio.md) | Regras de preço, promoções, conflito, estoque, cancelamento |
| [06 Ciclo de vida do pedido](docs/06-ciclo-vida-pedido.md) | Diagrama de estados e transições válidas |
| [07 Casos de uso](docs/07-casos-de-uso.md) | Casos de uso e diagramas de sequência |
| [09 Testes](docs/09-testes.md) | Estratégia e cenários testados |
| [10 Execução](docs/10-execucao.md) | Como rodar, testar e demonstrar |
| [11 Divisão da equipe](docs/11-divisao-equipe.md) | Responsabilidades de cada integrante |

As migrações do banco ficam em [`src/main/resources/db/migration`](src/main/resources/db/migration). As especificações e o histórico das mudanças (OpenSpec) ficam em [`openspec/`](openspec).

## Equipe

- Amanda Duara Ramos
- Arthur Justo Nicolucci
- Lucas Gomes Ferreira
- Mateus da Mata Rocha
