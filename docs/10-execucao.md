# 10 - Execução

- **Objetivo:** reunir as instruções para compilar, configurar e executar o sistema, e para rodar os testes e a demonstração. O enunciado exige instruções de execução na entrega final.
- **Etapas do roadmap relacionadas:** etapa 1 (fundação) preenche a maior parte; as etapas 2 a 9 complementam (dados de demonstração, novos endpoints, relatórios). Ver [08-roadmap.md](08-roadmap.md).
- **Status:** implementado (E1–E9): execução local, perfis, usuários e dados de demonstração, roteiros manuais por etapa (seção 8) e roteiro da demo da defesa. A simulação administrativa de pagamento da E4 foi substituída pelo gateway simulado da E6.

## 1. Pré-requisitos

| Item | Versão | Observação |
|---|---|---|
| JDK | 21 ou superior | Java 21 é a versão-alvo do projeto (`maven.compiler.release`). |
| Docker e Docker Compose | recente | Sobe o PostgreSQL de desenvolvimento e é **obrigatório para os testes de integração** (Testcontainers). |
| Maven | não precisa instalar | Use o wrapper `./mvnw` do repositório; ele baixa o Maven na primeira execução (requer internet nessa vez). |
| Git | qualquer versão recente | Repositório e histórico de commits fazem parte da entrega. |

Os comandos abaixo partem da raiz do repositório.

> **Roteiros por `curl`.** Os roteiros das seções 8 em diante usam `curl`, `jq`, `date -u -d` (GNU date) e arquivos temporários em `/tmp`. No Linux/macOS (no macOS, o `date -d` GNU exige `gdate` do coreutils) basta ter `curl` e `jq`. No Windows, rode-os no **Git Bash** e instale o `jq` (por exemplo, `winget install jqlang.jq`); o `/tmp` do Git Bash existe e funciona, e fora dele troque `/tmp` por qualquer pasta gravável (ex.: `$TMPDIR` ou `.`). Se preferir não instalar nada, quase todos os roteiros podem ser feitos pelo **Swagger UI** (seção 5): faça **Authorize** com o `accessToken` e execute os mesmos verbos e corpos.

## 2. Banco de dados (PostgreSQL via Docker Compose)

```bash
docker compose up -d
```

Sobe o PostgreSQL 17 (contêiner `ecommerce-postgres`) com banco, usuário e senha `ecommerce` (credenciais fictícias, só para desenvolvimento) e volume persistente `ecommerce-pgdata`.

> **Porta 5432 ocupada?** Se já houver um PostgreSQL local usando a porta 5432, suba o contêiner em uma porta livre qualquer (aqui, 5435) e informe a mesma porta à aplicação:
>
> ```bash
> export DB_PORT=5435
> docker compose up -d
> export DB_URL=jdbc:postgresql://localhost:$DB_PORT/ecommerce
> ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
> ```
>
> `DB_PORT` é lida pelo `docker-compose.yml` (mapeia `$DB_PORT` para a 5432 do contêiner) e `DB_URL` pela aplicação; use o mesmo número nas duas.

Para parar: `docker compose down` (mantém os dados). Para apagar também os dados e recriar o banco do zero: `docker compose down -v`.

### Migrações Flyway

As migrações ficam em `src/main/resources/db/migration` (convenção `V<n>__descricao.sql`; a E1 traz `V1__usuarios_clientes_enderecos.sql`, a E4 traz `V2__pedidos.sql`, com as tabelas do pedido, e a E2 traz `V3__catalogo_estoque.sql`, com catálogo, combos, saldo e reservas de estoque). Rodam automaticamente na subida da aplicação, e o Hibernate apenas **valida** o esquema (`ddl-auto: validate`).

## 3. Executar a aplicação (perfil `dev`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O perfil `dev`:
- conecta em `jdbc:postgresql://localhost:5432/ecommerce` com `ecommerce`/`ecommerce` (valores padrão, sobrescritos por `DB_URL`, `DB_USER` e `DB_PASSWORD` se definidos);
- usa um segredo JWT **fixo e inseguro** (`application-dev.yml`), válido só para desenvolvimento local;
- carrega os usuários, o catálogo e o estoque de demonstração e o pedido de exemplo (seções 6, 6.1 e 6.2).

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

> **CPF de exemplo do Swagger.** O CPF `529.982.247-25`, que aparece como exemplo no `POST /api/clientes`, já pertence a `cliente@demo.local` no perfil `dev`: cadastrá-lo de novo responde 409. Para criar um cliente novo, use outro CPF válido, como `111.444.777-35`, e outro e-mail.

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

No perfil `dev`, `cliente@demo.local` também recebe **um pedido de exemplo** em `AGUARDANDO_PAGAMENTO` (valores fictícios: 2 x Camiseta Básica a R$ 59,90 e 1 x Caneca a R$ 34,90, subtotal R$ 154,70, frete econômico R$ 25,00, total R$ 179,70, modalidade/prazo preservados, com o endereço principal do cliente copiado). Ele permanece como roteiro independente do checkout implementado na E4b: usa os SKUs reais do catálogo (seção 6.2), **reserva o estoque** (`ReservarEstoque`) e é criado pelos mesmos serviços usados pelo checkout (`CriarPedido` e `ConfirmarReservaDoPedido`), na mesma transação e **uma única vez** (só se o cliente ainda não tem pedidos; reiniciar a aplicação não o duplica) e **nunca fora do perfil `dev`**.

Roteiro (Swagger ou `curl`; troque de token em **Authorize** ao mudar de usuário):

1. Como `cliente@demo.local`: `GET /api/clientes/me/pedidos` lista o pedido de demonstração (anote o `id`); `GET /api/clientes/me/pedidos/{id}` mostra itens, totais, endereço e o histórico (uma entrada de criação, de nulo para `CRIADO`, e a de `AGUARDANDO_PAGAMENTO`).
2. **Pagar (E6).** Como cliente: `POST /api/clientes/me/pedidos/{id}/pagamento` com `{"forma":"CARTAO_CREDITO"}` cria cobrança (201), aprova por padrão e baixa a reserva. Para controlar a confirmação manualmente, escolha `PIX` ou `BOLETO` e siga a seção de pagamento E6 ao fim deste documento. **Ordem na demonstração:** o primeiro pagamento aprovado do cliente o torna inelegível à campanha `DEMO_PRIMEIRA_COMPRA`; para mostrar promoção e cupom combinados (seção "Promoções e cupons (E5)", totais 88,94 e 113,94), faça a compra do E5 e pague **esse** pedido antes de pagar o pedido de demonstração.
3. **Avançar o andamento com `estoque@demo.local`:** `POST /api/admin/pedidos/{id}/preparacao` (`EM_PREPARACAO`), `POST /api/admin/pedidos/{id}/envio` (`ENVIADO`) e `POST /api/admin/pedidos/{id}/entrega` (`ENTREGUE`). Chamar fora de ordem (por exemplo, `envio` num pedido `PAGO`) responde 409 com `situacaoAtual` e `operacao`.
4. **Cancelamento e transição inválida.** Com o pedido `ENVIADO`, tentar `POST /api/clientes/me/pedidos/{id}/cancelamento` (cliente) ou `POST /api/admin/pedidos/{id}/cancelamento` (`admin@demo.local` ou `comercial@demo.local`), com `{"motivo": "teste"}`, responde 409. Em um pedido `PAGO` ou `EM_PREPARACAO`, o cliente recebe 409 e o administrativo cancela; em `AGUARDANDO_PAGAMENTO`, o cliente cancela (RN-26).
5. Como `cliente@demo.local`, `GET /api/clientes/me/pedidos/{id}` mostra o histórico completo, com responsável e motivo de cada transição. Como `comercial@demo.local`, `GET /api/admin/pedidos?situacao=ENVIADO` filtra por situação.

Para repetir o roteiro com um pedido novo em `AGUARDANDO_PAGAMENTO` (e com a reserva de estoque dele), recrie o banco (`docker compose down -v` e suba de novo): o pedido de demonstração só é criado quando o cliente não tem pedidos. O **estoque** já reage às transições (baixa no pagamento aprovado, liberação na recusa e no cancelamento antes do pagamento, reposição no cancelamento depois do pagamento); o **estorno simulado** reage ao cancelamento pago na E6, preservando pendências recuperáveis se o fornecedor falhar.

### 6.2 Catálogo e estoque de demonstração (E2)

Criados **somente no perfil `dev`** pela `CatalogoDeDemonstracao`, com os casos de uso reais (dados fictícios). A carga é **idempotente por SKU**: reiniciar a aplicação não duplica nada nem soma estoque de novo (o estoque inicial só é lançado junto com a criação do produto).

| Categoria | Produto (SKU) | Preço | Estoque inicial |
|---|---|---|---|
| Roupas > Camisetas | Camiseta Básica, variações P, M e G, cor Azul (`CAM-BAS-P`, `CAM-BAS-M`, `CAM-BAS-G`) | R$ 59,90 cada | 20 de cada |
| Casa > Cozinha | Caneca de Cerâmica 300 ml (`CAN-CER-300`) | R$ 34,90 | 15 |
| Casa > Cozinha | Café Especial 250 g (`CAF-ESP-250`) | R$ 29,90 | 30 |
| Eletrônicos > Periféricos | Teclado Mecânico (`TEC-MEC-01`) | R$ 249,90 | **1** (para a demo da última unidade) |
| (combo, sem categoria) | **Kit Café** (`KIT-CAFE`): 1 caneca + 2 cafés | R$ 79,90 | sem saldo próprio |

Depois da carga e do pedido de demonstração (2 x `CAM-BAS-M` e 1 x `CAN-CER-300` reservados), os saldos são: Camiseta M 18 disponíveis e 2 reservados, Caneca 14 e 1, demais como na tabela. A disponibilidade do Kit Café é o mínimo de `disponivel / quantidade` entre os componentes: caneca 14 / 1 e café 30 / 2 = 15, ou seja, **14**.

**Roteiro (Swagger ou `curl`).** Para achar os ids das variações, use o detalhe público do produto.

1. **Vitrine, sem token.** `GET /api/catalogo/categorias` (árvore); `GET /api/catalogo/produtos?categoriaId=<id de Roupas>` lista a Camiseta Básica mesmo ela estando em uma subcategoria; `GET /api/catalogo/produtos/{id}` mostra as variações com `disponivel`; `GET /api/catalogo/combos` mostra o Kit Café com `disponibilidade` 14.
2. **Estoque baixo, como `estoque@demo.local`.** `GET /api/admin/estoque?abaixoDe=5` lista só o Teclado (disponível 1). O mesmo pedido com `comercial@demo.local` responde 403.
3. **Baixa, liberação e reposição:** consulte `GET /api/admin/estoque/{variacaoId}` de `CAM-BAS-M`. O pedido de demonstração reserva 2 unidades: saldo 18/2. Inicie cartão como cliente para aprovar: saldo 18/0. Cancele o pedido pago como ADMIN para repor: 20/0 e estorno registrado. Para recusa, inicie PIX e envie RECUSADO como ADMIN na confirmação E6: disponível volta a 20, reservado a 0.
4. **Roteiro da última unidade (Teclado).** (a) Sem token, `GET /api/catalogo/produtos?nome=Teclado` mostra `disponivel: 1` na variação `TEC-MEC-01` (anote o `id` dela). (b) Como `estoque@demo.local`, `GET /api/admin/estoque/{id}` mostra 1 disponível e 0 reservado. (c) `POST /api/admin/estoque/{id}/ajustes` com `{"quantidade": -2, "motivo": "teste"}` responde 409 (deixaria o estoque negativo) e o saldo continua 1; com `{"quantidade": -1, ...}` vai a 0, e `{"quantidade": 5, ...}` registra uma entrada. (d) A **disputa** pela última unidade não tem rota HTTP porque a reserva é do checkout (etapa 4): ela é demonstrada pelo teste de concorrência, `./mvnw test -Dtest=ReservaConcorrenteTests` (10 threads pela única unidade: exatamente 1 sucesso e 9 `EstoqueInsuficienteException`, saldo final 0 disponível e 1 reservado; o teste exige Docker).

> **Aviso: recriar a carga.** Alterar o estoque (como no passo 4) ou pagar o pedido de demonstração muda os saldos e, como a carga só semeia o que não existe, reiniciar a aplicação **não** restaura os valores originais. Para voltar ao estado inicial (Teclado com 1 unidade, pedido de demonstração com reserva), recrie o banco: `docker compose down -v` e suba de novo. Isso também é necessário se o banco foi criado antes da E2: o pedido de demonstração antigo não tem reserva, e a carga não cria outro pedido enquanto o cliente já tiver um.

## 7. Build e testes

```bash
./mvnw test      # testes unitários, de arquitetura e de integração
./mvnw verify    # compila, testa e valida o artefato
```

- Os **testes de integração exigem Docker em execução**: usam Testcontainers e sobem seu próprio PostgreSQL 17 descartável, sem relação com o banco do `docker compose`. Não é preciso subir o Compose para testar.
- Os testes unitários e de arquitetura (ArchUnit) não precisam de Docker, mas rodam no mesmo comando.
- Estratégia e cenários em [09-testes.md](09-testes.md).

## 8. Roteiros por etapa

| Etapa | Situação e onde está o roteiro |
|---|---|
| 3 | Implementado: roteiro na seção "Carrinho do cliente (E3)" (a carga do catálogo e do estoque está na seção 6.2). |
| 5 | Implementado: roteiro na seção "Promoções e cupons (E5)". |
| 4 (`checkout`) | Implementado: roteiro na seção "Checkout completo da E4b" (o pedido de demonstração da seção 6.1 permanece como roteiro independente). |
| 6 | Implementado: cobrança por cartão/PIX/boleto, confirmação idempotente e recuperação de estorno (roteiro abaixo). |
| 7 | Implementado: roteiro na seção "Entrega e cotação de frete (E7)". |
| 8 | Implementado: roteiro na seção "Eventos, notificações e consulta de auditoria (E8)". |
| 9 | Implementado: relatórios gerenciais (roteiro abaixo). O roteiro da demonstração final segue o checklist de [08-roadmap.md](08-roadmap.md) (seção 5). |

## 9. Solução de problemas

| Sintoma | Causa provável e solução |
|---|---|
| `docker compose up` falha com "port is already allocated" | A porta 5432 está ocupada: use uma porta livre qualquer, por exemplo `DB_PORT=5435 docker compose up -d` e `DB_URL=jdbc:postgresql://localhost:5435/ecommerce` (seção 2). |
| A aplicação falha na subida com erro de conexão | O contêiner não está de pé (`docker compose ps`) ou `DB_URL` aponta para a porta errada. |
| A aplicação falha na subida reclamando do segredo JWT | Fora do `dev`, defina `APP_JWT_SECRET` com 32 caracteres ou mais; ou rode com o perfil `dev`. |
| Testes de integração falham ao iniciar o contêiner | O Docker não está em execução ou o usuário não tem permissão para usá-lo. |
| Falha de validação de esquema após trocar de branch | O volume do banco tem um esquema antigo: `docker compose down -v` e suba de novo. |
| O Teclado não está mais com 1 unidade, ou o pedido de demonstração não tem reserva de estoque | A carga é idempotente e não restaura saldos, e o pedido de demonstração só é criado se o cliente não tem pedidos: `docker compose down -v` e suba de novo (seção 6.2). |

## Carrinho do cliente (E3)

Com a aplicação no perfil `dev`, obtenha o token conforme a seção 5 e defina `TOKEN_CLIENTE` com o `accessToken`. Os SKUs abaixo já existem na carga de demonstração. No Swagger, use **Authorize** com esse token.

```bash
curl -s http://localhost:8080/api/clientes/me/carrinho \
  -H "Authorization: Bearer $TOKEN_CLIENTE"
curl -s -X POST http://localhost:8080/api/clientes/me/carrinho/itens \
  -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' \
  -d '{"sku":"KIT-CAFE","quantidade":1}'
curl -s -X POST http://localhost:8080/api/clientes/me/carrinho/itens \
  -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' \
  -d '{"sku":"CAF-ESP-250","quantidade":2}'
curl -s -X PUT http://localhost:8080/api/clientes/me/carrinho/itens/KIT-CAFE \
  -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' \
  -d '{"quantidade":2}'
curl -s -X DELETE http://localhost:8080/api/clientes/me/carrinho/itens/KIT-CAFE \
  -H "Authorization: Bearer $TOKEN_CLIENTE"
```

Partindo de um carrinho vazio e preços iniciais da demonstração: um kit custa 79,90; com dois cafés avulsos o total é 139,70; ao alterar para dois kits vai a 219,60; removendo os kits resta 59,80. Descontos ficam vazios e frete é zero. Repetir POST soma à quantidade existente. Para reiniciar o exemplo, remova os SKUs pelo DELETE.

Todas as respostas trazem a seleção e o resumo atual. `insuficiencias` lista `{variacaoId, solicitada, disponivel}` quando a demanda agregada excede o saldo. Teste quantidade 1000 de `TEC-MEC-01` para observar aviso de estoque sem reserva; depois remova a linha. Confira o estoque com o token de ESTOQUE antes/depois: operações de carrinho não alteram disponível nem reservado. GET seguinte reflete alteração administrativa de preço. A seleção continua após novo login ou reinício.

Quantidade 0, negativa, 1.5 ou acima de 2147483647 responde 400; SKU desconhecido responde 404; item inativado não aceita inclusão/aumento (409), mas pode ser reduzido ou removido. Use token de ADMIN na rota do carrinho para verificar 403 e sem token para verificar 401. Checkout implementado na E4b: veja a seção seguinte. Verificação automatizada: `./mvnw test -Dtest=CarrinhoTests,CarrinhoPersistenciaTests,PipelinePrecoTests,CarrinhoAplicacaoTests,CarrinhoWebTests`.


## Checkout completo da E4b

Requer aplicação no perfil `dev`, `curl` e `jq`. O fluxo usa o preço atual e a quantidade disponível dos SKUs de demonstração; deixe apenas as duas linhas abaixo no carrinho. Faça login como cliente e consulte seu endereço ativo:

```bash
TOKEN_CLIENTE=$(curl -fsS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"cliente@demo.local","senha":"Demo1234"}' | jq -r '.accessToken')
ENDERECO_ID=$(curl -fsS http://localhost:8080/api/clientes/me/enderecos \
  -H "Authorization: Bearer $TOKEN_CLIENTE" | jq -r '.[0].id')
curl -fsS -X POST http://localhost:8080/api/clientes/me/carrinho/itens \
  -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' \
  -d '{"sku":"KIT-CAFE","quantidade":1}'
curl -fsS -X POST http://localhost:8080/api/clientes/me/carrinho/itens \
  -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' \
  -d '{"sku":"CAF-ESP-250","quantidade":1}'

curl -fsS -X POST http://localhost:8080/api/clientes/me/checkout/previa \
  -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' \
  -d "{\"enderecoId\":$ENDERECO_ID,\"opcaoEntrega\":\"economica-simulada:ECONOMICA\"}" > /tmp/checkout-previa.json
jq . /tmp/checkout-previa.json
jq --argjson enderecoId "$ENDERECO_ID" \
  '{enderecoId: $enderecoId, opcaoEntrega: .confirmacao.opcaoEntrega.codigo, confirmacao: .confirmacao}' \
  /tmp/checkout-previa.json > /tmp/checkout-confirmacao.json
curl -sS -X POST http://localhost:8080/api/clientes/me/checkout \
  -D /tmp/checkout-headers.txt \
  -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' \
  --data-binary @/tmp/checkout-confirmacao.json > /tmp/checkout-resposta.json
cat /tmp/checkout-headers.txt
jq . /tmp/checkout-resposta.json
```

A prévia retorna `confirmacao` (versão, linhas, descontos, subtotal, frete, total e endereço) e `insuficiencias`. A leitura não cria pedido nem reserva. Com preços iniciais e uma unidade de cada SKU, subtotal R$ 109,80; entrega econômica em SP custa R$ 25,00 e o total sem benefícios é R$ 134,80. O POST de conclusão retorna **201**, `Location: /api/clientes/me/pedidos/{id}` e pedido **AGUARDANDO_PAGAMENTO**. O carrinho fica vazio e os componentes do kit mais o café avulso passam de disponível para reservado; componentes compartilhados são somados. A confirmação recebida é apenas comparada; o snapshot usa os valores recalculados no servidor.

Para testar a reconfirmação, obtenha a prévia e, antes de concluir, altere o preço de uma variação usando `PUT /api/admin/produtos/variacoes/{id}` com token ADMIN e corpo `{"preco":32.50}` (ids consultáveis na vitrine do catálogo). A conclusão com o arquivo anterior retorna **409** com `divergencias` e `previaAtualizada`, sem pedido ou reserva. Revise o detalhamento; só então monte outro arquivo a partir de `.previaAtualizada.confirmacao` e reenvie. Alterações de endereço ou quantidade também exigem confirmação; apenas o total ser igual não dispensa a comparação dos preços por linha.

Se a resposta for 201, consulte e cancele o pedido para verificar liberação da reserva:

```bash
PEDIDO_ID=$(jq -r '.id' /tmp/checkout-resposta.json)
curl -fsS "http://localhost:8080/api/clientes/me/pedidos/$PEDIDO_ID" \
  -H "Authorization: Bearer $TOKEN_CLIENTE"
curl -fsS -X POST "http://localhost:8080/api/clientes/me/pedidos/$PEDIDO_ID/cancelamento" \
  -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' \
  -d '{"motivo":"Cancelamento da demonstração"}'
curl -fsS http://localhost:8080/api/clientes/me/carrinho \
  -H "Authorization: Bearer $TOKEN_CLIENTE"
```

O cancelamento preserva o carrinho vazio e devolve as unidades ao disponível uma única vez. Para aprovar/recusar em outro pedido aguardando pagamento, inicie PIX como comprador e confirme APROVADO/RECUSADO como ADMIN conforme roteiro E6 abaixo: aprovação baixa a reserva, recusa a libera e devolve cupom. O checkout não inicia cobrança automaticamente. Promoções/cupons, pagamento e entrega estão integrados nas E5–E7.

Erros: 400 para corpo/confirmacao ausentes ou dados inválidos (inclusive números fracionários em ids/quantidades); 401 sem token; 403 sem CLIENTE; 404 para endereço inativo, inexistente ou alheio; 409 para carrinho vazio, SKU indisponível, confirmação antiga, estoque insuficiente ou disputa da versão. Estoque insuficiente informa `variacaoId` e `disponivel`; nada da tentativa permanece reservado e a seleção é preservada. Duas conclusões da mesma seleção não geram dois pedidos.

Evidência automatizada dos verbos, payloads e respostas: `CheckoutIntegracaoTests`, incluindo o fluxo variação/kit → mudança de preço → reconfirmação → reserva → cancelamento, OpenAPI, rollback após flush e concorrência real em PostgreSQL. Execute `./mvnw -o -Dtest=RevalidarCheckoutTests,ConfirmacaoCheckoutTests,CheckoutIntegracaoTests test`; para toda a regressão e JAR, `./mvnw -o verify`.

### Campanhas e cupons de demonstração (perfil `dev`)

Criados na subida, uma única vez (idempotente por nome/código), depois do catálogo de demonstração (seção 6.2), pela `PromocoesDeDemonstracao`. Cada campanha passa pelo mesmo `PromocaoFactoryRegistry` (Factory Method) e pela mesma validação de SKU/categoria da API administrativa. Todas as campanhas abaixo nascem **ativas**, com vigência de 100 anos; o administrativo as lista em `GET /api/admin/promocoes` (ADMIN ou PROMOCOES, ex.: `promocoes@demo.local`).

| Nome | Tipo | Regra (SKU/categoria) | Modo | Prioridade |
|---|---|---|---|---|
| `DEMO_LEVE3_PAGUE2_CAMISETA` | `LEVE_X_PAGUE_Y` | leve 3 e pague 2 na Camiseta Básica M (`CAM-BAS-M`) | acumulável | 30 |
| `DEMO_CAMISETAS_15` | `PERCENTUAL_CATEGORIA` | 15% nos itens da categoria Camisetas (`CAM-BAS-P`, `-M`, `-G`) | não acumulável | 20 |
| `DEMO_ACIMA_DE_200_MENOS_30` | `VALOR_FIXO_ACIMA_DE` | R$ 30,00 em compras a partir de R$ 200,00 | não acumulável | 10 |
| `DEMO_FRETE_GRATIS_ACIMA_DE_200` | `FRETE_GRATIS` | frete bruto zerado em compras a partir de R$ 200,00 (condição `VALOR_MINIMO`) | acumulável | 5 |
| `DEMO_PRIMEIRA_COMPRA` | `PERCENTUAL_PRIMEIRA_COMPRA` | 10% para quem nunca teve pagamento aprovado; **inicia inativa** (ver roteiro E5 abaixo) | acumulável | 10 |

Cupom: `DEMO10` (10% após as promoções, limite global 100 e individual 1), aplicado pelo cliente em `PUT /api/clientes/me/carrinho/cupom` conforme o roteiro E5.

**Carrinho de exemplo que dispara múltiplas promoções e conflito.** Como `cliente@demo.local`, limpe o carrinho e adicione 3 x `CAM-BAS-M` e 1 x `CAN-CER-300` (subtotal R$ 214,60). Candidatas elegíveis: leve 3 pague 2 (R$ 59,90, acumulável), 15% em Camisetas (R$ 26,96, não acumulável), R$ 30,00 acima de R$ 200 (não acumulável) e frete grátis (acumulável, efetivo na prévia do checkout, que é onde o frete é cotado). Pela política padrão (RN-02): as duas acumuláveis são aplicadas; entre as não acumuláveis vence a de R$ 30,00 e `DEMO_CAMISETAS_15` é **descartada por conflito**. Desconto de itens: R$ 89,90 (itens: R$ 124,70 antes do cupom). Na prévia do checkout (endereço em SP), o frete bruto vem da fórmula da seção "Entrega e cotação de frete (E7)": base do Sudeste R$ 25,00 + seguro de 1% sobre o que passa de R$ 200,00 (1% x R$ 14,60 = R$ 0,146, arredondado uma vez) = **R$ 25,15** na econômica (**R$ 40,29** na expressa: R$ 40,00 + 2% x R$ 14,60 = R$ 0,292). Com 4 itens e sem peso não há adicional de item nem de peso. O frete grátis gera então um desconto de frete de R$ 25,15 (R$ 40,29 se a expressa for escolhida) e o frete líquido fica em **R$ 0,00**; total da prévia R$ 124,70. O frete de R$ 25,00 do pedido de demonstração (seção 6.1) vale para o subtotal dele, R$ 154,70, abaixo da franquia de R$ 200,00. Com `ecommerce.promocoes.politica-conflito=maior-desconto` (reinicie com `ECOMMERCE_PROMOCOES_POLITICA_CONFLITO=maior-desconto`), só a de maior desconto (leve 3 pague 2, R$ 59,90) é aplicada. Evidência automatizada: `PromocoesDemonstracaoTests` e `PoliticaConflitoSelecionavelTests`.

Efeito sobre os outros roteiros: os carrinhos deles ficam abaixo de R$ 200 e fora de Camisetas, então as campanhas acima não os alteram; no roteiro "Compra notificada e consulta da trilha" (E8), use como `DEMO_SKU` um SKU fora de Camisetas (ex.: `CAN-CER-300`). Para repetir a carga com valores originais, recrie o banco (`docker compose down -v`).

## Promoções e cupons (E5)

A API administrativa exige ADMIN ou PROMOCOES. O cliente aplica um cupom por código, e o servidor obtém primeira compra, perfis e endereço: campos do corpo como `primeiraCompra`, `perfis` ou `clienteId` não determinam elegibilidade.

No perfil `dev`, há cupom `DEMO10` ativo (10%, limite global 100 e individual 1) e campanha `DEMO_PRIMEIRA_COMPRA` **inicialmente inativa** (10% para quem nunca teve pagamento aprovado). A carga não restaura usos nem duplica registros em reinícios. Ative explicitamente a campanha para este roteiro, usando `curl` e `jq`:

```bash
TOKEN_PROMOCOES=$(curl -fsS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"promocoes@demo.local","senha":"Demo1234"}' | jq -r '.accessToken')
curl -fsS http://localhost:8080/api/admin/promocoes/tipos \
  -H "Authorization: Bearer $TOKEN_PROMOCOES"
curl -fsS http://localhost:8080/api/admin/promocoes/condicoes \
  -H "Authorization: Bearer $TOKEN_PROMOCOES"
curl -fsS 'http://localhost:8080/api/admin/promocoes?page=0&size=100' \
  -H "Authorization: Bearer $TOKEN_PROMOCOES" \
  | jq '.conteudo[] | select(.nome == "DEMO_PRIMEIRA_COMPRA")' > /tmp/campanha-demo.json
CAMPANHA_ID=$(jq -r '.id' /tmp/campanha-demo.json)
jq '{versao}' /tmp/campanha-demo.json > /tmp/campanha-versao.json
curl -fsS -X POST "http://localhost:8080/api/admin/promocoes/$CAMPANHA_ID/ativacao" \
  -H "Authorization: Bearer $TOKEN_PROMOCOES" -H 'Content-Type: application/json' \
  --data-binary @/tmp/campanha-versao.json
```

Tipos de promoção: `LEVE_X_PAGUE_Y` (sku/leve/pague), `PERCENTUAL_CATEGORIA` (categoriaId/percentual), `VALOR_FIXO_ACIMA_DE` (minimo/valor), `PERCENTUAL_PRIMEIRA_COMPRA` (percentual) e `FRETE_GRATIS`. Os catálogos `/tipos` e `/condicoes` vêm dos registros usados na criação, aceitando extensões registradas. A política D4 é a padrão; exclusiva impede cupom e vence outras campanhas; sem exclusiva, aplicam-se acumuláveis e a melhor não acumulável.

Exemplo de cadastro de campanha e cupom (vigência UTC; ajuste as datas se executar depois de 2029):

```bash
curl -fsS -X POST http://localhost:8080/api/admin/promocoes \
  -H "Authorization: Bearer $TOKEN_PROMOCOES" -H 'Content-Type: application/json' \
  -d '{"nome":"Compra acima de 300","tipo":"VALOR_FIXO_ACIMA_DE","inicio":"2026-01-01T00:00:00Z","fim":"2030-01-01T00:00:00Z","prioridade":10,"modo":"ACUMULAVEL","parametros":{"minimo":300,"valor":50},"condicao":{"tipo":"REGIAO","parametros":{"ufs":["SP","RJ"]}}}'
curl -fsS -X POST http://localhost:8080/api/admin/cupons \
  -H "Authorization: Bearer $TOKEN_PROMOCOES" -H 'Content-Type: application/json' \
  -d '{"codigo":"EXEMPLO15","nome":"Cupom de exemplo","tipo":"PERCENTUAL","inicio":"2026-01-01T00:00:00Z","fim":"2030-01-01T00:00:00Z","acumulavel":true,"limiteTotal":50,"limiteCliente":1,"parametros":{"percentual":15}}'
curl -fsS 'http://localhost:8080/api/admin/cupons?page=0&size=20' \
  -H "Authorization: Bearer $TOKEN_PROMOCOES"
```

POST retorna 201 e Location; GET `/{id}` consulta, PUT `/{id}` altera exigindo a versão devolvida, POST `/{id}/ativacao` ou `/{id}/inativacao` recebe `{"versao":0}` com a versão corrente. Código de cupom é imutável, normalizado para maiúsculas e único. Não reduzir limites abaixo do consumo ativo. Benefícios de cupom: `PERCENTUAL`, `VALOR_FIXO`, `FRETE_GRATIS` e `REDUCAO_FRETE`; consulte parâmetros em `/api/admin/cupons/tipos`.

Com `TOKEN_CLIENTE` do roteiro anterior, aplique/remova cupom:

```bash
curl -fsS -X PUT http://localhost:8080/api/clientes/me/carrinho/cupom \
  -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' \
  -d '{"codigo":"DEMO10"}'
curl -fsS http://localhost:8080/api/clientes/me/carrinho \
  -H "Authorization: Bearer $TOKEN_CLIENTE"
# Rode esta remocao apenas para retirar o cupom; omita-a para comprar com ele.
curl -fsS -X DELETE http://localhost:8080/api/clientes/me/carrinho/cupom \
  -H "Authorization: Bearer $TOKEN_CLIENTE"
```

Partindo de um carrinho contendo apenas 1 `KIT-CAFE` e 1 `CAF-ESP-250`, com preços iniciais, campanha de primeira compra ativa e cliente sem pagamento aprovado anterior: subtotal 109,80; promoção 10,98; cupom DEMO10 de 9,88 sobre 98,82; resumo de itens **88,94** antes da entrega; na prévia com econômica em SP, frete bruto R$ 25,00 e total **113,94** sem benefício adicional de frete. Se já houver pagamento aprovado, inclusive de pedido depois cancelado, a campanha não se aplica. Pedidos recusados/cancelados antes de pagar mantêm a elegibilidade. O carrinho usa o endereço principal para região; a prévia usa o escolhido.

Para concluir com cupom, mantenha-o selecionado e repita a obtenção de prévia e envio de `.confirmacao` da seção de checkout. O detalhamento agora inclui `cupom` (código, id, versão e motivo) e descontos com `origem`, `referenciaId` e `codigo`. Mudança de campanha, cupom, endereço ou histórico entre prévia e conclusão retorna 409 para reconfirmação; o preço enviado nunca é usado para formar o pedido. O pedido conserva os descontos por origem e a compra limpa itens e cupom. Consumo ocorre apenas na conclusão com benefício positivo; aplicar/consultar/prévia não consomem.

Para observar devolução, consulte `/api/admin/cupons/{id}`, cancele o pedido aguardando pagamento como no roteiro anterior e consulte novamente: o uso ativo volta, uma única vez. A recusa simulada também devolve; aprovação seguida de cancelamento administrativo devolve o uso do cupom, preservando o snapshot. Limites global e individual resistem a concorrência, e falhas da compra desfazem pedido, estoque, uso e seleção em conjunto. Benefício de valor zero (por exemplo frete ainda zero) não consome utilização.

Rejeições de cupom são 409 com `motivo`: INEXISTENTE, INATIVO, FORA_DA_VIGENCIA, ESGOTADO, LIMITE_CLIENTE ou INCOMPATIVEL; alteração detectada durante consumo usa CONFIGURACAO_ALTERADA. Código malformado é 400. Tentar substituir por inválido preserva o cupom anterior. Se o selecionado perder validade, o resumo mostra `cupom.motivo` e totaliza sem o benefício; ler e alterar itens continua possível. Checkout com esse código fica bloqueado até remoção/substituição, mesmo reenviando o resumo sem desconto. Sem token dá 401, perfis sem permissão 403, versão administrativa antiga/código duplicado 409 e alvo inexistente 404.

Frete grátis/redução já são regras de benefício testadas com cotação bruta simulada. A E7 liga cotação real simulada ao checkout; o carrinho continua resumo antes da escolha. Pagamento foi integrado na E6; auditoria completa permanece E8. Testes: `./mvnw -o -Dtest=PromocoesDominioTests,PromocoesCuponsIntegracaoTests,MigracaoPromocionalTests,PromocoesDemonstracaoTests test`; a suíte completa e o JAR usam `./mvnw -o verify`.


## Pagamento, confirmação e recuperação de estorno (E6)

Use um pedido em AGUARDANDO_PAGAMENTO criado pelo checkout. Os exemplos são corpos JSON para Swagger ou curl com `Authorization: Bearer <token>` e `Content-Type: application/json`.

1. Como CLIENTE dono: `POST /api/clientes/me/pedidos/{id}/pagamento` com `{"forma":"PIX"}`. Resposta 201 com Location, total, fornecedor, `idTransacaoExterna` e situacao PENDENTE. Anote a transação; repita a mesma requisição para obter 200 e a mesma transação. Trocar a forma depois disso dá 409. BOLETO também fica pendente; CARTAO_CREDITO aprova de forma síncrona por padrão.
2. Como ADMIN: `POST /api/admin/pagamentos/confirmacoes` com `{"idTransacaoExterna":"<transacao retornada>","situacao":"APROVADO"}`. Retorna 200. O pedido fica PAGO e a reserva BAIXADA. Repita: continua 200, sem novo histórico, evento ou baixa. Em outro pedido pendente, envie RECUSADO para liberar reserva e devolver uso de cupom. PENDENTE não é aceito no callback (400). Outro resultado para transação terminal dá 409; desconhecida dá 404.
3. Como comprador: `GET /api/clientes/me/pedidos/{id}/pagamento` consulta a cobrança e `estorno`. Outro cliente recebe 404; ADMIN não usa a área do cliente. GET do detalhe do pedido mostra histórico com responsável SISTEMA e fornecedor/transação no motivo.
4. Como ADMIN ou OPERADOR_COMERCIAL: cancele pedido PAGO ou EM_PREPARACAO em `POST /api/admin/pedidos/{id}/cancelamento` com `{"motivo":"desistência"}`. O pedido fica CANCELADO, estoque é reposto, cupom devolvido e o fornecedor original faz estorno integral. Consulte o pagamento como comprador: `estorno.situacao=CONCLUIDO`, `tentativas=1`, comprovante externo. Como ADMIN, `POST /api/admin/pagamentos/{id}/estorno/tentativas` retorna o sucesso existente sem repetir estorno. Pedido cancelado sem pagamento aprovado não gera estorno.
5. Para demonstrar falha recuperável, configure o servidor com `ECOMMERCE_PAGAMENTO_SIMULADO_FALHAR_ESTORNO=true`, reinicie preservando banco e cancele outro pedido pago. O cancelamento permanece e `estorno.situacao=FALHOU`. Restaure false, reinicie e faça o POST de tentativas como ADMIN: CONCLUIDO, tentativas incrementadas, sem nova reposição/devolução. Para cartão recusado síncrono, use `ECOMMERCE_PAGAMENTO_SIMULADO_CARTAO_APROVADO=false`. Essas flags são configuração do servidor; o cliente não escolhe o resultado.

Seleção opcional por configuração: `ecommerce.pagamento.fornecedores.cartao_credito=cartao-simulado`, `.pix=pix-simulado`, `.boleto=boleto-simulado`. Para trocar fornecedor, registre outro bean implementando GatewayPagamento com identidade estável e configure sua seleção; mantenha o fornecedor antigo registrado para estornos de cobranças antigas. Seleção inexistente/ambígua impede startup.

A rota temporária de aprovação direta foi removida. Confirmação inédita após cancelamento recebe 409; replay de resultado já aceito continua 200 após cancelamento. Pedidos legados pagos pelo antigo atalho podem registrar pendência de conciliação sem transação conhecida; retentativa dá 409, sem inventar cobrança financeira. Expiração automática de pendências ainda não existe; use o cancelamento explícito. Todos os fornecedores são simulados, sem dados reais de cartão.


## Entrega e cotação de frete (E7)

O checkout agora exige seleção explícita; pedidos anteriores preservam frete e têm opcaoEntrega=null. A versão do carrinho não muda ao consultar frete ou prévia. Prazo em dias úteis é estimado após aprovação do pagamento, sem cálculo de feriados nem envio automático.

1. Como CLIENTE, com carrinho não vazio e endereço ativo próprio, faça `POST /api/clientes/me/checkout/fretes` com `{"enderecoId":<id>}`. A resposta é `{ "opcoes": [...] }` com código, fornecedor, modalidade, custoBruto e prazoDias. Por padrão econômica em SP é `economica-simulada:ECONOMICA`, R$ 25,00/5 dias; expressa é `expressa-simulada:EXPRESSA`, R$ 40,00/2 dias. Consulta não inclui desconto: a prévia aplica benefícios para a opção escolhida.
2. Envie `POST /api/clientes/me/checkout/previa` com `{"enderecoId":<id>,"opcaoEntrega":"economica-simulada:ECONOMICA"}`. Guarde `.confirmacao` inteiro. Inclui `opcaoEntrega`, `frete` bruto, `freteLiquido`, descontos e total. Para 2 itens de R$ 10,00 em SP, total sem benefícios é R$ 45,00 na econômica ou R$ 60,00 na expressa.
3. Envie `POST /api/clientes/me/checkout` com `{"enderecoId":<id>,"opcaoEntrega":"economica-simulada:ECONOMICA","confirmacao":<bloco retornado>}`. Resposta 201 preserva modalidade e prazo. Se preço/prazo/fornecedor mudar, 409 exige revisar `previaAtualizada`; se perder cobertura, escolha em `opcoesEntrega` e peça outra prévia. Ausência da escolha é 400; nenhum serviço atendendo gera lista vazia; falha de todos os necessários gera 503.
4. Para demonstrar frete grátis, cadastre campanha `FRETE_GRATIS` no cadastro de promoções já documentado, com condições/vigência válidas e parâmetros `{}`. A prévia mostra frete bruto, desconto FRETE e freteLiquido=0, sem subtrair duas vezes. Cupom de frete só consome uso se acrescentar benefício positivo; se a campanha já zera o frete, não há uso consumido. Exclusiva de itens respeita política e impede campanha de frete grátis.
5. Pague o pedido pelo roteiro E6. Como ADMIN/ESTOQUE, avance preparação→envio→entrega pelas rotas existentes. Consulte o pedido como comprador para ver histórico e opção preservados. Alterar tarifa depois não muda cobrança ou estorno. Cancelamento de pedido pago devolve o total originalmente pago e mantém devolução única de estoque/cupom.

Tabela simulada padrão:

| Região | Econômica (R$ / dias úteis) | Expressa (R$ / dias úteis) |
|---|---|---|
| Sudeste | 25 / 5 | 40 / 2 |
| Sul | 30 / 6 | 48 / 3 |
| Centro-Oeste | 35 / 7 | 56 / 3 |
| Nordeste | 40 / 9 | 64 / 4 |
| Norte | 45 / 12 | 72 / 6 |

Configuração opcional do servidor: `ecommerce.entrega.economica.sudeste.valor=25`, `.prazo-dias=5`, `ecommerce.entrega.economica.ufs=SP,RJ,...`; para expressa troque o prefixo por `ecommerce.entrega.expressa`. Regiões: sudeste, sul, centro_oeste, nordeste, norte. Sem configuração, ambas cobrem as 27 UFs; string de cobertura vazia desabilita atendimento. Custo negativo, prazo não positivo ou UF inválida impedem startup. Coeficientes opcionais por modalidade: `franquia-kg` (1), `adicional-kg` (2 econômica / 4 expressa), `franquia-itens` (5), `adicional-item` (1 / 2), `franquia-valor` (200) e `seguro-percentual` (1 / 2); valores negativos impedem startup. Fórmula em 05-regras-negocio.md. Não são tarifas comerciais reais e não se inventa peso de produto (o checkout envia 0 kg). Novo provedor/modalidade entra por bean do contrato ProvedorFrete, com código único, sem mudança do checkout.

A demonstração dev cria pedido novo com econômica e seus custos correntes (padrão SP: subtotal R$ 154,70, frete R$ 25,00 e total R$ 179,70); reiniciar não altera pedidos já existentes nem suas opções. Se faltar estoque ou serviço de entrega, o pedido de exemplo é omitido com diagnóstico e a API continua disponível. Nenhuma carga existe fora de dev.

## Eventos, notificações e consulta de auditoria (E8)

Grupos 1–4 concluídos e change arquivada em 2026-10-23. A V8 cria a trilha sem fabricar histórico. A carga dev usa autenticação temporária de sistema, sem JWT de usuário, e não cria registros administrativos fictícios.

Os acontecimentos confirmados de criação, aprovação, recusa, envio e cancelamento chegam ao canal `email-simulado` após commit. No log, procure `Notificacao enviada pedido=... acontecimento=... canal=email-simulado`. Logs não imprimem destinatário, corpo ou mensagem arbitrária da exceção. Falha de preparação/canal gera diagnóstico e não desfaz compra/pagamento/estoque/cupom. Não há fila durável ou retentativa automática.

### Compra notificada e consulta da trilha

Com a API dev em execução, `curl` e `jq` instalados, obtenha JWTs de ADMIN e CLIENTE pela seção 5. Defina `DEMO_ADMIN` e `DEMO_CLIENTE` com os valores de `accessToken` (sem `Bearer`), `DEMO_BASE=http://localhost:8080`, `DEMO_VARIACAO` com uma variação ativa e `DEMO_SKU` com seu SKU, e `DEMO_ENDERECO` com um endereço próprio do comprador. O catálogo público `GET /api/catalogo/produtos` e `GET /api/catalogo/produtos/{id}` mostra ids/SKUs; `GET /api/clientes/me/enderecos` retorna os endereços. Prepare carrinho sem outros itens/cupom, estoque disponível de ao menos duas unidades e nenhuma outra campanha elegível para comparar os valores abaixo. Use apenas dados de demonstração: o roteiro altera preço e cria campanha/pedido.

1. Alterar o preço da variação e criar uma campanha. O mínimo de R$ 100 impede que essa campanha desconte a compra de R$ 25; a auditoria registra ambas as operações.

```bash
curl -fsS -X PUT "$DEMO_BASE/api/admin/produtos/variacoes/$DEMO_VARIACAO" \
  -H "Authorization: Bearer $DEMO_ADMIN" -H 'Content-Type: application/json' \
  -d '{"preco":12.50}'

DEMO_INICIO=$(date -u +%Y-%m-%dT%H:%M:%SZ)
DEMO_FIM=$(date -u -d '+1 day' +%Y-%m-%dT%H:%M:%SZ)
jq -n --arg inicio "$DEMO_INICIO" --arg fim "$DEMO_FIM" \
  '{nome:"Campanha auditoria",tipo:"VALOR_FIXO_ACIMA_DE",inicio:$inicio,fim:$fim,prioridade:1,modo:"ACUMULAVEL",parametros:{minimo:100,valor:10}}' \
  | curl -fsS -X POST "$DEMO_BASE/api/admin/promocoes" \
      -H "Authorization: Bearer $DEMO_ADMIN" -H 'Content-Type: application/json' --data-binary @-
```

2. Comprar duas unidades com econômica em SP. O carrinho responde 200, prévia 200, checkout 201; subtotal R$ 25 + frete R$ 25 = total R$ 50 nas condições acima. Guarde a confirmação inteira. O log contém a notificação de criação em `AGUARDANDO_PAGAMENTO`.

```bash
jq -n --arg sku "$DEMO_SKU" '{sku:$sku,quantidade:2}' \
  | curl -fsS -X POST "$DEMO_BASE/api/clientes/me/carrinho/itens" \
      -H "Authorization: Bearer $DEMO_CLIENTE" -H 'Content-Type: application/json' --data-binary @-

DEMO_PREVIA=$(jq -n --argjson endereco "$DEMO_ENDERECO" \
  '{enderecoId:$endereco,opcaoEntrega:"economica-simulada:ECONOMICA"}' \
  | curl -fsS -X POST "$DEMO_BASE/api/clientes/me/checkout/previa" \
      -H "Authorization: Bearer $DEMO_CLIENTE" -H 'Content-Type: application/json' --data-binary @-)

DEMO_PEDIDO=$(jq -n --argjson endereco "$DEMO_ENDERECO" --argjson previa "$DEMO_PREVIA" \
  '{enderecoId:$endereco,opcaoEntrega:"economica-simulada:ECONOMICA",confirmacao:$previa.confirmacao}' \
  | curl -fsS -X POST "$DEMO_BASE/api/clientes/me/checkout" \
      -H "Authorization: Bearer $DEMO_CLIENTE" -H 'Content-Type: application/json' --data-binary @-)
DEMO_PEDIDO_ID=$(printf '%s' "$DEMO_PEDIDO" | jq -r '.id')
```

3. Iniciar PIX como comprador (201), confirmar como ADMIN (200) e cancelar como ADMIN (200). Aprovação e cancelamento comunicam o cliente após commit; cancelamento de pedido pago repõe estoque e estorna o total original. O callback não cria trilha administrativa; cancelamento cria registro com estado anterior `PAGO` e motivo.

```bash
DEMO_COBRANCA=$(curl -fsS -X POST "$DEMO_BASE/api/clientes/me/pedidos/$DEMO_PEDIDO_ID/pagamento" \
  -H "Authorization: Bearer $DEMO_CLIENTE" -H 'Content-Type: application/json' -d '{"forma":"PIX"}')
jq -n --argjson cobranca "$DEMO_COBRANCA" \
  '{idTransacaoExterna:$cobranca.idTransacaoExterna,situacao:"APROVADO"}' \
  | curl -fsS -X POST "$DEMO_BASE/api/admin/pagamentos/confirmacoes" \
      -H "Authorization: Bearer $DEMO_ADMIN" -H 'Content-Type: application/json' --data-binary @-
curl -fsS -X POST "$DEMO_BASE/api/admin/pedidos/$DEMO_PEDIDO_ID/cancelamento" \
  -H "Authorization: Bearer $DEMO_ADMIN" -H 'Content-Type: application/json' -d '{"motivo":"Teste"}'
```

4. Consultar preço, campanha e cancelamento. A consulta retorna `conteudo`, `page`, `size`, `totalElementos`, `totalPaginas`; os registros incluem usuário, instante UTC, operação, tipo/id de recurso e detalhes. Os preços estão em `detalhes.antes.variacoes[].preco` e `detalhes.depois.variacoes[].preco`; o cancelamento tem `detalhes.antes.estado` e `detalhes.depois.motivo`.

```bash
DEMO_TRILHA=$(curl -fsS -G "$DEMO_BASE/api/admin/auditoria" \
  -H "Authorization: Bearer $DEMO_ADMIN" \
  --data-urlencode 'operacao=VARIACAO_PRECO_ALTERADO' --data-urlencode 'size=1')
printf '%s\n' "$DEMO_TRILHA" | jq .
DEMO_ATOR=$(printf '%s' "$DEMO_TRILHA" | jq -r '.conteudo[0].usuarioId')
DEMO_MOMENTO=$(printf '%s' "$DEMO_TRILHA" | jq -r '.conteudo[0].ocorridoEm')
curl -fsS -G "$DEMO_BASE/api/admin/auditoria" -H "Authorization: Bearer $DEMO_ADMIN" \
  --data-urlencode "usuarioId=$DEMO_ATOR" --data-urlencode 'operacao=VARIACAO_PRECO_ALTERADO' \
  --data-urlencode "inicio=$DEMO_MOMENTO" --data-urlencode "fim=$DEMO_MOMENTO"
curl -fsS -G "$DEMO_BASE/api/admin/auditoria" -H "Authorization: Bearer $DEMO_ADMIN" \
  --data-urlencode 'operacao=CAMPANHA_CRIADA'
curl -fsS -G "$DEMO_BASE/api/admin/auditoria" -H "Authorization: Bearer $DEMO_ADMIN" \
  --data-urlencode 'operacao=PEDIDO_CANCELADO'
```

Filtros são opcionais e combinados por AND, com igualdade de operação/usuário e intervalo inclusivo. Ordem `ocorridoEm DESC, id DESC`; `page=0`, `size=20` por padrão, máximo 100. Uma operação desconhecida bem formada responde 200 com lista vazia. `size=101`, id não positivo, operação fora de `[A-Z][A-Z0-9_]{0,99}`, instante sem fuso ou intervalo invertido dão 400. Sem token dá 401; CLIENTE, OPERADOR_COMERCIAL, ESTOQUE e PROMOCOES dão 403. Não existe rota para editar/excluir a trilha, mesmo para ADMIN. OpenAPI documenta os seis parâmetros e as respostas.

### Reprodução automatizada e falha de canal

O mesmo fluxo de requisições/corpos é exercitado por `PagamentoIntegracaoTests.roteiroDeCompraNotificadaEConsultaDaTrilha`, com fixtures isoladas (`PAG-ITEM` a R$ 10 antes da alteração, estoque 10, endereço SP), JWTs reais e PostgreSQL/Testcontainers, via MockMvc. Confere as três notificações, total/estorno R$ 50 e exatamente três registros: preço, campanha e cancelamento. O cenário de falha de canal é `PagamentoIntegracaoTests.falhaDeCanalNaoDesfazPagamentoNemMudaRespostaOuReplay`: spy de teste lança uma exceção ao enviar; confirmação/replay retornam 200, pedido fica PAGO e estoque baixa uma única vez. A falha é injetada somente no teste; não existe endpoint público para configurar falhas de notificação.

Execute da raiz com Docker disponível:

```bash
./mvnw -o -Dtest=ConsultarAuditoriaTests,AuditoriaIntegracaoTests,MigracaoAuditoriaTests,OperacaoAdministrativaTests,PagamentoIntegracaoTests,NotificacoesIntegracaoTests,EnviarNotificacoesTests,RegrasDeArquiteturaTests test
```

Esse comando também cobre autorização, filtros/paginação, imutabilidade da API, transações e migração. A reprodução automatizada executa HTTP por MockMvc; os exemplos curl acima servem à sessão manual dev com os ids/tokens locais.

Evidência do grupo 3 em 2026-10-23: comando de reprodução acima aprovado com **62 testes, zero falhas/erros/ignorados (BUILD SUCCESS)**, incluindo consulta, roteiro de compra notificada/falha de canal, auditoria transacional, migração e ArchUnit. Log `/tmp/bdr-auditoria-grupo3-tests.log`. OpenSpec strict e git diff --check aprovados. `clean verify` completo e geração do JAR atualizado permanecem no grupo 4; progresso 15/17 tarefas.

## Relatórios gerenciais (E9)

Os relatórios são `GET` somente leitura sob `/api/admin/relatorios`, com período obrigatório e inclusivo (`inicio` e `fim`, ISO-8601 com fuso, por exemplo `2026-10-01T00:00:00Z`). Uma **venda** é um pedido pago dentro do período e não cancelado no momento da consulta; o valor é o total do pedido. `ADMIN` e `OPERADOR_COMERCIAL` acessam todos os relatórios; `PROMOCOES` acessa somente `/campanhas`. Com a API dev em execução, `curl` e `jq` instalados, obtenha o JWT de `ADMIN` pela seção 5 e defina `DEMO_ADMIN` (valor de `accessToken`, sem `Bearer`) e `DEMO_BASE=http://localhost:8080`. Ajuste o período para incluir a data de pagamento dos pedidos da demonstração (o pedido de demonstração da seção 6.1 e as compras feitas nos roteiros acima).

```bash
INI=2026-01-01T00:00:00Z; FIM=2026-12-31T23:59:59Z

# Valor total, quantidade de pedidos, ticket médio, total de descontos e cancelamentos
curl -s -H "Authorization: Bearer $DEMO_ADMIN" "$DEMO_BASE/api/admin/relatorios/vendas?inicio=$INI&fim=$FIM" | jq

# Produtos mais vendidos (limite de 1 a 50, padrão 10)
curl -s -H "Authorization: Bearer $DEMO_ADMIN" "$DEMO_BASE/api/admin/relatorios/produtos-mais-vendidos?inicio=$INI&fim=$FIM&limite=5" | jq

# Campanhas e cupons mais utilizados e desconto concedido por campanha
curl -s -H "Authorization: Bearer $DEMO_ADMIN" "$DEMO_BASE/api/admin/relatorios/campanhas?inicio=$INI&fim=$FIM" | jq
```

Para conferir os números, compare com o histórico dos pedidos (`GET /api/admin/pedidos` e `GET /api/admin/pedidos/{id}`): o pedido de demonstração pago entra em `valorTotalVendas` com o seu `total`, os seus itens aparecem em `produtos-mais-vendidos` e os seus descontos em `campanhas` (agrupados por campanha ou cupom). Um pedido pago e depois cancelado deixa de contar como venda, e o cancelamento aparece em `quantidadeCancelamentos` do período em que ocorreu. Período sem pedidos devolve 200 com zeros e listas vazias; período ausente, malformado ou invertido e `limite` fora de 1 a 50 devolvem 400.

Reprodução automatizada, com valores conhecidos e datas controladas (T-RL-01): `./mvnw -o -Dtest=RelatoriosDominioTests,RelatoriosGerenciaisTests,RegrasDeArquiteturaTests test` (exige Docker para o PostgreSQL via Testcontainers).
