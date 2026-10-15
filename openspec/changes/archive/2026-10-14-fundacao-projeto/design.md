# Design

## Context

O repositório tem apenas documentação (`docs/`) e configuração do OpenSpec. A motivação está em `proposal.md` (Why) e o comportamento esperado nas specs `autenticacao`, `cadastro-cliente` e `plataforma-api`. Ambiente observado: JDK 26 instalado, Docker disponível, sem Maven nem PostgreSQL locais. A arquitetura alvo (camadas `web`/`aplicacao`/`dominio`/`infra` por módulo) e os contratos centrais estão em `docs/03-arquitetura.md`. Decisões confirmadas com o autor: autenticação por **JWT stateless** e testes de integração com **Testcontainers**.

## Goals / Non-Goals

**Goals:**
- Base executável que as etapas 2 a 9 estendem sem reestruturar.
- Regra de dependência entre camadas verificada por teste, não só por convenção.
- Contratos da onda 0 compiláveis, para que cada integrante implemente o seu módulo contra eles.

**Non-Goals:**
- Refresh token, recuperação de senha, confirmação de e-mail e bloqueio por tentativas.
- Implementações dos contratos centrais e o contrato `EstadoPedido` (etapa 4, mesmo dono do checkout).
- Pipeline de CI e deploy.
- Auditoria das operações administrativas (etapa 8). As tabelas de usuário já guardam `criado_em` e `atualizado_em`.

## Decisions

### D1. Build e versões
Maven com **Maven Wrapper** (`mvnw`), já que Maven não está instalado. **Spring Boot na linha 4.x estável mais recente**, conferida no start.spring.io no momento da implementação, compilando com `--release 21` (o JDK 26 local compila para 21 sem problema). springdoc-openapi na versão compatível com essa linha do Boot.
- Alternativa: Gradle. Rejeitado porque o roadmap e o `10-execucao.md` já preveem Maven e a equipe tem menos fricção com ele.
- Alternativa: Spring Boot 3.5. Rejeitado porque sai de suporte durante o projeto.

### D2. Estrutura de pacotes
Raiz `br.com.fatec.ecommerce`. Nesta etapa só existem os módulos com conteúdo:
- `compartilhado`: erros, `Dinheiro` e a base de eventos.
- `seguranca`: usuário, perfis, JWT e configuração do Spring Security.
- `cliente`: cliente e endereços.
- Os pacotes `dominio` dos contratos da onda 0: `promocao`, `preco`, `pagamento`, `entrega` e `notificacao`.

Os demais módulos são criados por seus donos. Não se criam pacotes vazios.

**Ajuste em relação ao `03-arquitetura.md`:** `Usuario` e `Perfil` ficam em `seguranca` (identidade e credenciais), e `Cliente` em `cliente` referencia o usuário pelo id. Assim a autenticação de administradores não depende do módulo de cliente. O `03` deve ser atualizado.

### D3. Modelo de dados (Flyway `V1`)
- `usuario`: `id`, `email` (único, armazenado em minúsculas), `senha_hash`, `ativo`, `criado_em`, `atualizado_em`.
- `usuario_perfil`: `usuario_id`, `perfil` (texto com `CHECK` nos 5 valores). Chave primária composta.
- `cliente`: `id`, `usuario_id` (único, FK), `nome`, `cpf` (único, 11 dígitos sem máscara), `telefone`.
- `endereco`: `id`, `cliente_id` (FK), `apelido`, `cep`, `logradouro`, `numero`, `complemento`, `bairro`, `cidade`, `uf`, `principal`, `ativo`, `criado_em`.
- **Índice único parcial** em `endereco(cliente_id) WHERE principal AND ativo`: o banco garante no máximo um endereço principal.

`spring.jpa.hibernate.ddl-auto=validate` e `open-in-view=false`.
- Alternativa: perfis como coluna única ou tabela de papéis com permissões. Rejeitado: cinco perfis fixos não justificam um modelo de permissões.

### D4. JWT
Uso de `spring-boot-starter-oauth2-resource-server`, com emissão via `JwtEncoder` (Nimbus) em HS256 e segredo vindo de `APP_JWT_SECRET`. Apenas o perfil `dev` tem um valor padrão; fora dele, a aplicação não sobe sem o segredo. Detalhes:
- Claims: `sub` com o id do usuário, `email` e `perfis`. Um conversor transforma `perfis` em authorities `ROLE_<PERFIL>`.
- Expiração configurável (`app.jwt.expiracao`, padrão 60 min).
- `SessionCreationPolicy.STATELESS`, CSRF desabilitado (API sem cookies).

Alternativas consideradas:
- Biblioteca jjwt com filtro próprio. Rejeitado: mais código de segurança mantido à mão.
- Sessão HTTP. Rejeitado na decisão com o autor.

### D5. Senhas
`PasswordEncoderFactories.createDelegatingPasswordEncoder()`, que usa bcrypt por padrão e permite migrar de algoritmo. A política de senha (mínimo 8 caracteres, com letra e dígito) é uma anotação de validação própria, aplicada nos DTOs de entrada.

### D6. Erros
`@RestControllerAdvice` gera `ProblemDetail` para:
- validação (400, com a propriedade `campos` listando campo e mensagem);
- exceções de domínio do `compartilhado`: `RecursoNaoEncontradoException` (404) e `ConflitoException` (409);
- qualquer outra exceção (500, mensagem genérica, stack trace só no log).

`AuthenticationEntryPoint` e `AccessDeniedHandler` próprios escrevem o mesmo formato para 401 e 403. Cada módulo novo só cria exceções que estendem essas bases.

### D7. Isolamento do cliente
Os recursos do cliente ficam sob `/api/clientes/me/**`. A identidade vem sempre do token, nunca de um id na URL. Os endereços são buscados por `(id, clienteId)`, e quando não existe o par a resposta é 404 (cobre a spec de isolamento sem revelar a existência do recurso).

### D8. Endpoints

| Método e rota | Acesso | Função |
|---|---|---|
| `POST /api/auth/login` | público | emitir token |
| `POST /api/clientes` | público | criar conta |
| `GET`, `PUT /api/clientes/me` | `CLIENTE` | dados próprios |
| `GET`, `POST /api/clientes/me/enderecos` | `CLIENTE` | listar e criar endereços |
| `GET`, `PUT`, `DELETE /api/clientes/me/enderecos/{id}` | `CLIENTE` | gerir um endereço |
| `PUT /api/clientes/me/enderecos/{id}/principal` | `CLIENTE` | marcar como principal |
| `GET`, `POST /api/admin/usuarios` | `ADMIN` | listar e criar administrativos |
| `PUT /api/admin/usuarios/{id}/perfis` | `ADMIN` | alterar perfis |
| `DELETE /api/admin/usuarios/{id}` | `ADMIN` | inativar |
| `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health` | público | documentação e saúde |

A autorização é por `requestMatchers` na configuração (visão central) e `@PreAuthorize` nos casos de uso administrativos. A regra do último `ADMIN` fica no caso de uso, dentro de uma transação, contando os `ADMIN` ativos com lock (`SELECT ... FOR UPDATE`) para evitar condição de corrida entre duas remoções simultâneas.

### D9. Perfis de execução e banco
- `docker-compose.yml` sobe PostgreSQL 17 com volume nomeado.
- Perfil `dev`: aponta para o compose, define o segredo JWT padrão e carrega os usuários de demonstração.
- Testes: `@ServiceConnection` com `PostgreSQLContainer` (Testcontainers) numa configuração compartilhada, reutilizada por todas as classes de teste de integração.
- Sem perfil: lê `DB_URL`, `DB_USER`, `DB_PASSWORD` e `APP_JWT_SECRET` do ambiente.

### D10. Usuários de demonstração
`ApplicationRunner` com `@Profile("dev")`, idempotente: cria um usuário só se o e-mail não existir.
- Usuários: `admin@demo.local`, `comercial@demo.local`, `estoque@demo.local`, `promocoes@demo.local` e `cliente@demo.local`.
- Senha comum fictícia: `Demo1234`.
- O cliente tem CPF fictício válido e um endereço.

As credenciais ficam em `docs/10-execucao.md`.
- Alternativa: seed por migração Flyway. Rejeitado porque os usuários entrariam em todos os ambientes, com hash fixo no SQL.

### D11. Contratos da onda 0
Interfaces no pacote `dominio` de cada módulo dono, conforme `docs/03-arquitetura.md` §5.1 a §5.8, sem implementação. Cada uma ganha Javadoc com o propósito e a marca de contrato provisório. Os tipos de que as interfaces precisam são `record`s mínimos, também provisórios:
- `ContextoPreco`, `DescontoCandidato`, `DescontoAplicado`, `ModoAcumulacao`, `AlvoDesconto`, `CalculoPreco`
- `FormaPagamento`, `SolicitacaoCobranca`, `ResultadoCobranca`, `SolicitacaoEstorno`, `ResultadoEstorno`
- `ContextoFrete`, `CotacaoFrete`
- `Notificacao`

`Dinheiro` (`BigDecimal`, escala 2, arredondamento `HALF_EVEN`, operações imutáveis) fica em `compartilhado` porque todos os contratos de valor o usam. `EventoDominio` e um `PublicadorEventos` (interface do domínio, implementada sobre o `ApplicationEventPublisher`) também ficam em `compartilhado`. Cada dono pode alterar o contrato do seu módulo via PR revisado pelos consumidores, registrando a mudança no `03`.

### D12. Testes
- **Unitários** (sem Spring): validador de CPF, política de senha, `Dinheiro`, regra de endereço principal e regra do último `ADMIN`.
- **Integração** (`@SpringBootTest` + MockMvc + Testcontainers): um teste por cenário das specs.
- **Arquitetura** (ArchUnit): `dominio` não depende de `web`, `infra` nem do Spring Web, e `web` não acessa `infra` diretamente. É a verificação automática da RNF-03.

### D13. Design Patterns nesta etapa
O enunciado exige patterns ligados a problemas reais e penaliza classes criadas só para casar com um nome. A fundação aplica os patterns **no nível dos contratos**: define o papel de cada interface, e as implementações concretas chegam nas etapas de cada dono.

| Contrato (onda 0) | Pattern e papel | Problema do enunciado | Implementação concreta |
|---|---|---|---|
| `Promocao` | Strategy (interface da estratégia) | Novas campanhas sem cadeia de `if` | E5 |
| `CondicaoElegibilidade` | Composite (componente; `CondicaoE`/`Ou`/`Nao` são os compostos) | Várias condições simultâneas sem métodos grandes | E5 |
| `PoliticaConflito` | Strategy | Regra de acumulação e prioridade trocável | E5 |
| `EtapaPreco` | Chain of Responsibility (pipeline) | Preço em etapas sem método único | E3, E5 |
| `GatewayPagamento` | Adapter (alvo) | Fornecedores com interfaces diferentes | E6 |
| `ProvedorFrete` | Adapter (alvo) | Novas transportadoras | E7 |
| `EventoDominio` e `PublicadorEventos` | Observer (sujeito desacoplado dos ouvintes) | Vários componentes reagindo a um acontecimento | E8 |
| `CanalNotificacao` | Strategy/Adapter de canal | Novos canais sem acoplar pedidos | E8 |

Alternativas descartadas, válidas para todos os contratos acima:
- **Classes concretas sem interface**, com `if/else` ou `switch` por tipo de promoção, gateway ou transportadora. Rejeitada porque cada novo tipo alteraria o código existente, o que viola a extensibilidade exigida.
- **Deixar os contratos para cada etapa.** Rejeitada porque quatro pessoas trabalham em paralelo, e sem contrato comum o checkout e o pipeline não teriam contra o que integrar.

O Javadoc de cada contrato nomeia o pattern, o papel da interface e a seção correspondente de `docs/04-design-patterns.md`.

Os módulos `seguranca` e `cliente` são CRUD com regras simples e **não introduzem pattern artificial**. Usam apenas o que o Spring já oferece: a cadeia de filtros do Spring Security e o `DelegatingPasswordEncoder` como seletor de algoritmo. Isso fica registrado no `04` como "uso de framework", não como pattern próprio do projeto.

O pattern de criação (Factory Method/registro de promoções) e o State do pedido não entram aqui. Entram nas etapas 5 e 4, onde existe o problema que os justifica.

## Risks / Trade-offs

- [Incompatibilidade entre versões do Boot 4.x, springdoc e Testcontainers] → Usar o BOM do Spring Boot e a versão de springdoc indicada para a linha do Boot. Se houver conflito, fixar a versão do Boot anterior que funcione e registrar no `10-execucao.md`.
- [Testes de integração exigem Docker] → Documentar como pré-requisito. Os testes unitários rodam sem Docker.
- [Segredo JWT vazado no repositório] → Valor padrão só no perfil `dev`, marcado como inseguro. Fora do `dev`, ausência do segredo impede a inicialização.
- [Contratos da onda 0 mudarem e quebrarem trabalho em paralelo] → São marcados como provisórios. Mudanças passam por PR revisado pelos consumidores e são registradas no `03-arquitetura.md`.
- [Wrapper do Maven exige download na primeira execução] → Registrar no `10-execucao.md`.

## Migration Plan

Não há dados nem versão anterior. A migração `V1` cria o esquema do zero. Rollback é descartar o volume do PostgreSQL de desenvolvimento.
