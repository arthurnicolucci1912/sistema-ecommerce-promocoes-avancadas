# Tasks

> Não fazer commits. Ao final de cada grupo, as alterações ficam no working tree para revisão e commit do autor.

## 1. Esqueleto do projeto

- [x] 1.1 Gerar o projeto Maven com Maven Wrapper, Spring Boot 4.x estável (D1), `--release 21`, `groupId` `br.com.fatec`, pacote raiz `br.com.fatec.ecommerce` e as dependências da proposta (incluindo ArchUnit e Testcontainers no escopo de teste). Verificar: `./mvnw -q -DskipTests package` conclui com sucesso.
- [x] 1.2 Criar `docker-compose.yml` com PostgreSQL 17 e volume nomeado, além de `.gitignore` para Java/Maven/IDE. Verificar: `docker compose up -d` deixa o banco aceitando conexões.
- [x] 1.3 Configurar `application.yml` (variáveis de ambiente, `ddl-auto=validate`, `open-in-view=false`), `application-dev.yml` (compose e segredo JWT padrão de desenvolvimento) e a configuração compartilhada de Testcontainers com `@ServiceConnection` (D9). Verificar: a aplicação sobe com `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`, e sem perfil falha se `APP_JWT_SECRET` estiver ausente.

## 2. Plataforma da API (`compartilhado`)

- [ ] 2.1 Criar a migração Flyway `V1` com as tabelas e o índice parcial de D3. Verificar: um teste de integração sobe contra o banco vazio do Testcontainers e a validação do esquema passa (spec plataforma-api, "Esquema do banco versionado").
- [ ] 2.2 Implementar as exceções base (`RecursoNaoEncontradoException`, `ConflitoException`) e o `@RestControllerAdvice` com `ProblemDetail` de D6, incluindo a propriedade `campos`. Verificar: testes de integração dos cenários "Erro de validação", "Recurso inexistente" e "Erro inesperado" (sem stack trace no corpo).
- [ ] 2.3 Configurar springdoc com esquema de segurança Bearer e expor `/actuator/health` apenas com status. Verificar: testes de "Especificação disponível" e "Aplicação saudável", sem token.
- [ ] 2.4 Implementar o objeto de valor `Dinheiro` (D11) com testes unitários de soma, subtração, multiplicação, arredondamento `HALF_EVEN` e imutabilidade.

## 3. Autenticação e autorização (`seguranca`)

- [ ] 3.1 Implementar `Usuario`, o enum `Perfil`, o repositório e o mapeamento JPA de `usuario` e `usuario_perfil`. Verificar: teste de integração de persistência mostrando que o e-mail é normalizado em minúsculas e que o e-mail duplicado é rejeitado.
- [ ] 3.2 Configurar Spring Security stateless com resource server JWT, conversor `perfis` → `ROLE_*`, rotas públicas de D8, e `AuthenticationEntryPoint` e `AccessDeniedHandler` em Problem Details. Verificar: testes de "Requisição sem token", "Token expirado ou adulterado", "Endpoints públicos" e "Erros de segurança no mesmo formato".
- [ ] 3.3 Implementar `POST /api/auth/login` com emissão de token HS256, expiração configurável e mensagem genérica para falha. Verificar: testes de "Login bem-sucedido", "Senha incorreta", "E-mail inexistente" e "Usuário inativo".
- [ ] 3.4 Implementar o `DelegatingPasswordEncoder` e a anotação de política de senha (D5). Verificar: testes unitários da política (`abcdefgh` e `1234567` rejeitadas, `Demo1234` aceita) e teste de integração "Persistência da senha" (valor no banco difere da senha e é verificável).
- [ ] 3.5 Implementar a gestão de usuários administrativos (`/api/admin/usuarios`: criar, listar, alterar perfis, inativar) com a regra do último `ADMIN` sob lock (D8). Verificar: testes unitários da regra e testes de integração de "Criar usuário administrativo", "Perfil inexistente", "Atribuir CLIENTE a usuário administrativo", "Remover o último ADMIN", "Cliente em área administrativa" e "Perfil administrativo sem permissão".

## 4. Cadastro de cliente (`cliente`)

- [ ] 4.1 Implementar o validador de CPF (dígitos verificadores, rejeitando sequências repetidas) com testes unitários de CPFs válidos e inválidos.
- [ ] 4.2 Implementar `Cliente` e `POST /api/clientes` (cria o usuário com perfil `CLIENTE` e o cliente na mesma transação). Verificar: testes de "Cadastro válido", "E-mail já cadastrado", "CPF já cadastrado" e "Dados inválidos". Testar também "Respostas sem credenciais" no corpo do cadastro.
- [ ] 4.3 Implementar `GET` e `PUT /api/clientes/me` (somente nome e telefone alteráveis). Verificar: testes de "Consultar os próprios dados" e "Alterar telefone", incluindo que e-mail e CPF enviados no `PUT` são ignorados ou rejeitados.
- [ ] 4.4 Implementar `Endereco` e o CRUD em `/api/clientes/me/enderecos` com remoção por inativação, validação de CEP e UF e busca por `(id, clienteId)`. Verificar: testes de "Cadastrar endereço", "Vários endereços", "Endereço inválido", "Remover endereço" e "Acessar endereço de outro cliente".
- [ ] 4.5 Implementar a regra de endereço principal (primeiro endereço é o principal, troca e promoção do mais antigo na remoção) e `PUT .../{id}/principal`. Verificar: testes unitários da regra e testes de integração de "Primeiro endereço", "Trocar endereço principal" e "Remover o principal".

## 5. Usuários de demonstração

- [ ] 5.1 Implementar o `ApplicationRunner` com `@Profile("dev")` idempotente de D10. Verificar: testes de "Execução em desenvolvimento" (login de cada usuário de demo), "Reinício em desenvolvimento" (sem duplicação) e "Execução fora de desenvolvimento".

## 6. Contratos da onda 0 e patterns

- [ ] 6.1 Criar as interfaces `Promocao`, `CondicaoElegibilidade`, `PoliticaConflito`, `EtapaPreco`, `GatewayPagamento`, `ProvedorFrete`, `CanalNotificacao`, `EventoDominio` e `PublicadorEventos`, com os `record`s mínimos de D11, nos pacotes `dominio` dos módulos donos. Cada contrato ganha Javadoc com propósito, pattern e papel (D13), marca de provisório e link para a seção de `docs/04-design-patterns.md`. Verificar: `./mvnw -q -DskipTests compile` passa e nenhuma interface tem implementação.
- [ ] 6.2 Implementar `PublicadorEventos` sobre `ApplicationEventPublisher` (adaptador em `compartilhado.infra`). Verificar: teste de integração em que um evento de teste publicado chega a um `@EventListener` de teste.
- [ ] 6.3 Criar testes ArchUnit (D12): `dominio` não depende de `web`, `infra` nem de `org.springframework.web`, e `web` não depende de `infra`. Verificar: os testes passam e falham quando se adiciona, temporariamente, uma dependência proibida.

## 7. Documentação

- [ ] 7.1 Atualizar `docs/10-execucao.md` com pré-requisitos reais, `docker compose up -d`, execução no perfil `dev`, `./mvnw test`, URLs do Swagger e da saúde, variáveis de ambiente e credenciais de demonstração. Verificar: seguir o documento do zero sobe a aplicação e permite login pelo Swagger.
- [ ] 7.2 Atualizar `docs/03-arquitetura.md` (`Usuario`/`Perfil` em `seguranca`, pacotes efetivos, contratos como implementados), `docs/02-modelo-dominio.md` (usuário, cliente e endereço reais), `docs/04-design-patterns.md` (contratos da onda 0 com pattern e papel; nota de "uso de framework" no Spring Security, conforme D13) e `docs/09-testes.md` (Testcontainers decidido; cenários T-SG cobertos). Marcar o status desses itens como `implementado` onde couber. Verificar: os nomes de classes e pacotes citados existem no código.

## 8. Verificação final

- [ ] 8.1 Rodar `./mvnw verify` com Docker ativo (todos os testes unitários, de integração e de arquitetura passam) e fazer o roteiro manual: subir com o perfil `dev`, logar como `cliente@demo.local` pelo Swagger, cadastrar um endereço e confirmar 403 ao chamar `/api/admin/usuarios`. Confirmar que nenhum commit foi criado (`git status` mostra as alterações pendentes).
