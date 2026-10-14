# Proposal

## Why

O repositório ainda não tem código, e as etapas seguintes do roadmap (`docs/08-roadmap.md`) dependem de quatro coisas que ainda não existem: a base executável, a segurança, o cadastro de clientes e os contratos que permitem aos módulos evoluírem em paralelo (onda 0 de `docs/11-divisao-equipe.md`). Sem essa fundação, catálogo, promoções, pagamento e frete não têm onde se apoiar nem como se integrar.

## What Changes

- Projeto Java 21 + Spring Boot (Maven Wrapper), com a estrutura de pacotes por módulo de `docs/03-arquitetura.md` (raiz `br.com.fatec.ecommerce`).
- PostgreSQL via Docker Compose para desenvolvimento e migrações versionadas com Flyway.
- Autenticação stateless com JWT (login por e-mail e senha), senhas armazenadas somente como hash e autorização por perfil (`CLIENTE`, `ADMIN`, `OPERADOR_COMERCIAL`, `ESTOQUE`, `PROMOCOES`).
- Cadastro de cliente (UC-01) e gestão dos próprios dados e endereços (UC-02), com isolamento entre clientes.
- Gestão de usuários administrativos e seus perfis por um `ADMIN` (UC-A11), impedindo remover o último `ADMIN`.
- Respostas de erro padronizadas (Problem Details) e documentação da API com OpenAPI/Swagger UI, com suporte a token Bearer.
- Usuários de demonstração com dados fictícios, criados apenas no perfil de execução de desenvolvimento.
- Interfaces dos contratos centrais compartilhados entre integrantes, sem implementação: `Promocao`, `CondicaoElegibilidade`, `PoliticaConflito`, `EtapaPreco`, `GatewayPagamento`, `ProvedorFrete`, `CanalNotificacao` e `EventoDominio`, com os tipos de valor mínimos que elas exigem. `EstadoPedido` fica para a etapa 4.
- Testes automatizados de integração com PostgreSQL real via Testcontainers.
- Atualização de `docs/10-execucao.md` (instruções reais de execução) e dos documentos afetados.
- Nenhum commit: as alterações ficam no working tree para revisão do autor.

## Design Patterns

Esta change **prepara** os patterns do enunciado definindo os contratos e o papel de cada um. A implementação concreta fica nas etapas de cada dono. Detalhes em `design.md` D13.

| Pattern | Contrato preparado | Requisito do PDF que o justifica |
|---|---|---|
| Strategy | `Promocao`, `PoliticaConflito` | Promoções: incluir nova promoção "com impacto reduzido"; Conflitos entre promoções |
| Composite | `CondicaoElegibilidade` | Condições de elegibilidade: "várias condições simultâneas" sem métodos grandes |
| Chain of Responsibility | `EtapaPreco` | Formação do preço: evitar um único método com muitas condicionais |
| Adapter | `GatewayPagamento`, `ProvedorFrete` | Pagamentos: fornecedores com interfaces diferentes; Entrega: novos serviços |
| Observer | `EventoDominio`, `PublicadorEventos` | Eventos do sistema: nenhuma classe deve conhecer todos os interessados |

Os módulos `seguranca` e `cliente` não têm ponto de variação real e não introduzem pattern próprio, o que o enunciado exige para evitar patterns artificiais. Pelo mesmo motivo, as specs desta change não trazem requisitos de extensibilidade: nenhum ponto de variação ganha comportamento observável nesta etapa. Esses requisitos entram nas specs das etapas 3 a 8. O Factory Method (criação) e o State entram nas etapas 5 e 4.

## Capabilities

### New Capabilities
- `autenticacao`: login, emissão e validação de token, armazenamento seguro de senha, autorização por perfil e gestão de usuários administrativos e perfis.
- `cadastro-cliente`: criação de conta de cliente, consulta e alteração dos próprios dados e gestão de endereços, com isolamento entre clientes.
- `plataforma-api`: comportamento transversal da API: formato padronizado de erros, documentação OpenAPI acessível e verificação de saúde da aplicação.

### Modified Capabilities

Nenhuma (não há specs existentes).

## Impact

- **Código novo:** projeto Maven completo (`pom.xml`, `mvnw`, `src/main`, `src/test`), `docker-compose.yml` e migrações Flyway iniciais.
- **API nova:** `/api/auth/*`, `/api/clientes/me/**`, `/api/admin/usuarios/**`, `/swagger-ui`, `/v3/api-docs` e `/actuator/health`.
- **Dependências:** Spring Web, Validation, Data JPA, Security, OAuth2 Resource Server (JWT), Flyway, driver PostgreSQL, springdoc-openapi, Actuator, Testcontainers.
- **Ambiente:** exige JDK 21 ou superior e Docker (para o banco de desenvolvimento e os testes). Maven é obtido pelo wrapper.
- **Documentação:** `docs/10-execucao.md`, `docs/03-arquitetura.md`, `docs/02-modelo-dominio.md`, `docs/09-testes.md` e `README.md` (seção de execução).
