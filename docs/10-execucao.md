# 10 - Execução

- **Objetivo:** reunir as instruções para compilar, configurar e executar o sistema, e para rodar os testes e a demonstração. O enunciado exige instruções de execução na entrega final.
- **Etapas do roadmap relacionadas:** etapa 1 (fundação) preenche a maior parte; as etapas 2 a 9 complementam (dados de demonstração, novos endpoints, relatórios). Ver [08-roadmap.md](08-roadmap.md).
- **Status:** implementado (E1), com o pedido de demonstração e a simulação de pagamento da E4 (seção 6.1). As etapas seguintes continuam a preencher a seção 8.

## 1. Pré-requisitos

| Item | Versão | Observação |
|---|---|---|
| JDK | 21 ou superior | Java 21 é a versão-alvo do projeto (`maven.compiler.release`). |
| Docker e Docker Compose | recente | Sobe o PostgreSQL de desenvolvimento e é **obrigatório para os testes de integração** (Testcontainers). |
| Maven | não precisa instalar | Use o wrapper `./mvnw` do repositório; ele baixa o Maven na primeira execução (requer internet nessa vez). |
| Git | qualquer versão recente | Repositório e histórico de commits fazem parte da entrega. |

Os comandos abaixo partem da raiz do repositório.

## 2. Banco de dados (PostgreSQL via Docker Compose)

```bash
docker compose up -d
```

Sobe o PostgreSQL 17 (contêiner `ecommerce-postgres`) com banco, usuário e senha `ecommerce` (credenciais fictícias, só para desenvolvimento) e volume persistente `ecommerce-pgdata`.

> **Porta 5432 ocupada?** Se já houver um PostgreSQL local usando a porta 5432, suba o contêiner em outra porta e informe a URL à aplicação:
>
> ```bash
> DB_PORT=5433 docker compose up -d
> export DB_URL=jdbc:postgresql://localhost:5433/ecommerce
> ```

Para parar: `docker compose down` (mantém os dados). Para apagar também os dados e recriar o banco do zero: `docker compose down -v`.

### Migrações Flyway

As migrações ficam em `src/main/resources/db/migration` (convenção `V<n>__descricao.sql`; a E1 traz `V1__usuarios_clientes_enderecos.sql` e a E4 traz `V2__pedidos.sql`, com as tabelas do pedido). Rodam automaticamente na subida da aplicação, e o Hibernate apenas **valida** o esquema (`ddl-auto: validate`).

## 3. Executar a aplicação (perfil `dev`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O perfil `dev`:
- conecta em `jdbc:postgresql://localhost:5432/ecommerce` com `ecommerce`/`ecommerce` (valores padrão, sobrescritos por `DB_URL`, `DB_USER` e `DB_PASSWORD` se definidos);
- usa um segredo JWT **fixo e inseguro** (`application-dev.yml`), válido só para desenvolvimento local;
- carrega os usuários de demonstração (seção 6).

A aplicação responde na porta 8080.

## 4. Variáveis de ambiente (fora do perfil `dev`)

Fora do `dev` não há valores padrão para conexão nem para o segredo, e **a aplicação não sobe sem `APP_JWT_SECRET`**.

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | sim | URL JDBC, por exemplo `jdbc:postgresql://localhost:5432/ecommerce`. |
| `DB_USER` | sim | Usuário do banco. |
| `DB_PASSWORD` | sim | Senha do banco. |
| `APP_JWT_SECRET` | sim | Segredo de assinatura dos tokens JWT (HS256), com **pelo menos 32 caracteres**. Nunca versionar. |

No perfil `dev`, `DB_URL`, `DB_USER` e `DB_PASSWORD` são opcionais (usados, por exemplo, quando a porta do Docker foi alterada).

## 5. URLs, login e Swagger

| Recurso | URL |
|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui/index.html> (`/swagger-ui.html` redireciona para ela) |
| Especificação OpenAPI | <http://localhost:8080/v3/api-docs> |
| Saúde da aplicação | <http://localhost:8080/actuator/health> (apenas `status`, sem detalhes) |

Essas três URLs são públicas (não exigem token).

### Obter um token e usar no Swagger

1. Faça login (pelo próprio Swagger, no endpoint `POST /api/auth/login`, ou por linha de comando):

   ```bash
   curl -s -X POST http://localhost:8080/api/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"email":"cliente@demo.local","senha":"Demo1234"}'
   ```

   A resposta traz `accessToken`, `tokenType` (`Bearer`) e `expiresIn` (segundos; o token expira em 60 minutos por padrão).
2. No Swagger UI, clique em **Authorize**, cole o valor de `accessToken` (somente o token, sem o prefixo `Bearer`) e confirme.
3. Os endpoints protegidos passam a ser chamados com o cabeçalho `Authorization: Bearer <token>`.

Erros de autenticação (401) e de autorização (403) voltam no formato Problem Details (RFC 9457), o mesmo dos demais erros da API.

## 6. Usuários de demonstração

Criados **somente no perfil `dev`**, de forma idempotente (reiniciar a aplicação não duplica nada). Todos os dados são **fictícios**; não há dado real de pessoa nem de cartão.

| E-mail | Perfil | Senha |
|---|---|---|
| `admin@demo.local` | `ADMIN` | `Demo1234` |
| `comercial@demo.local` | `OPERADOR_COMERCIAL` | `Demo1234` |
| `estoque@demo.local` | `ESTOQUE` | `Demo1234` |
| `promocoes@demo.local` | `PROMOCOES` | `Demo1234` |
| `cliente@demo.local` | `CLIENTE` (com um endereço principal) | `Demo1234` |

Roteiro rápido: entre como `cliente@demo.local`, consulte `GET /api/clientes/me` e cadastre um endereço em `POST /api/clientes/me/enderecos`; ao chamar `GET /api/admin/usuarios` com esse token, a resposta é 403. Com `admin@demo.local`, a mesma chamada retorna 200.

### 6.1 Pedido de demonstração e ciclo de vida (E4)

No perfil `dev`, `cliente@demo.local` também recebe **um pedido de exemplo** em `AGUARDANDO_PAGAMENTO` (valores fictícios: 2 x Camiseta Básica a R$ 59,90 e 1 x Caneca a R$ 34,90, subtotal R$ 154,70, frete R$ 18,50, total R$ 173,20, com o endereço principal do cliente copiado). Ele existe porque ainda não há checkout (etapa 4, change `checkout`): é criado pelos mesmos serviços que o checkout usará (`CriarPedido` e `ConfirmarReservaDoPedido`), **uma única vez** (só se o cliente ainda não tem pedidos; reiniciar a aplicação não o duplica) e **nunca fora do perfil `dev`**.

Roteiro (Swagger ou `curl`; troque de token em **Authorize** ao mudar de usuário):

1. Como `cliente@demo.local`: `GET /api/clientes/me/pedidos` lista o pedido de demonstração (anote o `id`); `GET /api/clientes/me/pedidos/{id}` mostra itens, totais, endereço e o histórico (uma entrada de criação, de nulo para `CRIADO`, e a de `AGUARDANDO_PAGAMENTO`).
2. **Simular o pagamento (TEMPORÁRIO).** Como `admin@demo.local`: `POST /api/admin/pedidos/{id}/pagamento-simulado` com `{"aprovado": true}` leva o pedido a `PAGO` (com `{"aprovado": false}` vai a `PAGAMENTO_RECUSADO`, que não é reaberto). Este endpoint existe só até a etapa 6 (`pagamento-idempotencia`), que o remove e passa a usar o gateway simulado; ele está marcado como temporário no Swagger.
3. **Avançar o andamento com `estoque@demo.local`:** `POST /api/admin/pedidos/{id}/preparacao` (`EM_PREPARACAO`), `POST /api/admin/pedidos/{id}/envio` (`ENVIADO`) e `POST /api/admin/pedidos/{id}/entrega` (`ENTREGUE`). Chamar fora de ordem (por exemplo, `envio` num pedido `PAGO`) responde 409 com `situacaoAtual` e `operacao`.
4. **Cancelamento e transição inválida.** Com o pedido `ENVIADO`, tentar `POST /api/clientes/me/pedidos/{id}/cancelamento` (cliente) ou `POST /api/admin/pedidos/{id}/cancelamento` (`admin@demo.local` ou `comercial@demo.local`), com `{"motivo": "teste"}`, responde 409. Em um pedido `PAGO` ou `EM_PREPARACAO`, o cliente recebe 409 e o administrativo cancela; em `AGUARDANDO_PAGAMENTO`, o cliente cancela (RN-26).
5. Como `cliente@demo.local`, `GET /api/clientes/me/pedidos/{id}` mostra o histórico completo, com responsável e motivo de cada transição. Como `comercial@demo.local`, `GET /api/admin/pedidos?situacao=ENVIADO` filtra por situação.

Para repetir o roteiro com um pedido novo em `AGUARDANDO_PAGAMENTO`, recrie o banco (`docker compose down -v` e suba de novo): o pedido de demonstração só é criado quando o cliente não tem pedidos. Estoque e estorno ainda não reagem às transições (ouvintes das etapas seguintes).

## 7. Build e testes

```bash
./mvnw test      # testes unitários, de arquitetura e de integração
./mvnw verify    # compila, testa e valida o artefato
```

- Os **testes de integração exigem Docker em execução**: usam Testcontainers e sobem seu próprio PostgreSQL 17 descartável, sem relação com o banco do `docker compose`. Não é preciso subir o Compose para testar.
- Os testes unitários e de arquitetura (ArchUnit) não precisam de Docker, mas rodam no mesmo comando.
- Estratégia e cenários em [09-testes.md](09-testes.md).

## 8. A preencher nas etapas seguintes

| Etapa | O que acrescentar aqui |
|---|---|
| 2 a 3 | Carga de dados de demonstração do catálogo e do estoque. |
| 5 | Promoções e cupons de demonstração. |
| 4 (`checkout`) | Como concluir um checkout de ponta a ponta (hoje o pedido de demonstração da seção 6.1 faz o papel dele). |
| 6 | Como simular a confirmação de pagamento pelo gateway (aprovada, recusada e repetida); substitui o endpoint temporário `pagamento-simulado` da seção 6.1. |
| 7 | Modalidades de entrega simuladas disponíveis. |
| 8 | Como consultar notificações simuladas e a trilha de auditoria. |
| 9 | Roteiro da demonstração final, conforme o checklist de [08-roadmap.md](08-roadmap.md) (seção 5). |

## 9. Solução de problemas

| Sintoma | Causa provável e solução |
|---|---|
| `docker compose up` falha com "port is already allocated" | A porta 5432 está ocupada: use `DB_PORT=5433 docker compose up -d` e `DB_URL=jdbc:postgresql://localhost:5433/ecommerce` (seção 2). |
| A aplicação falha na subida com erro de conexão | O contêiner não está de pé (`docker compose ps`) ou `DB_URL` aponta para a porta errada. |
| A aplicação falha na subida reclamando do segredo JWT | Fora do `dev`, defina `APP_JWT_SECRET` com 32 caracteres ou mais; ou rode com o perfil `dev`. |
| Testes de integração falham ao iniciar o contêiner | O Docker não está em execução ou o usuário não tem permissão para usá-lo. |
| Falha de validação de esquema após trocar de branch | O volume do banco tem um esquema antigo: `docker compose down -v` e suba de novo. |
