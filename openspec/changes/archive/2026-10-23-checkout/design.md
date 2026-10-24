# Design

## Context

Ver `proposal.md` para motivação e `specs/checkout/spec.md` para o comportamento. A entrega local de `carrinho-compras` foi aprovada pelo autor; não foi arquivada nem commitada. Ela contém `Carrinho` com versão otimista e revisão explícita para alterações de filhos, `GerenciarCarrinho`, `ResumoCarrinho` e `PipelinePreco`. A consulta do carrinho é tolerante a SKUs ausentes, portanto seu resumo sozinho não valida uma compra.

`CriarPedido` já persiste um snapshot em CRIADO e `ConfirmarReservaDoPedido` leva a AGUARDANDO_PAGAMENTO e publica PedidoCriado. `ReservarEstoque.para` precisa do id do pedido, agrega componentes e atualiza saldo condicionalmente, tudo-ou-nada. Seus ouvintes já liberam/baixam/repõem em eventos de pedido. `EnderecoRepository.buscarAtivo(id, clienteId)` já garante propriedade e situação. `ConflitoException` permite propriedades adicionais em Problem Details. `ItemVendavelInfo` ainda não expõe a descrição dos atributos da variação.

## Goals / Non-Goals

**Goals:** coordenar subsistemas existentes numa transação, exigir confirmação do detalhamento corrente, preservar propriedade dos dados e impedir perda/duplicação da seleção sob concorrência. Manter catálogo e pedido independentes de checkout, e pedido independente de estoque.

**Non-Goals:** cobrança, cupons, promoções, cálculo de frete, expiração de reserva e notificações externas. Não introduzir infraestrutura de idempotência geral: a versão e o esvaziamento do carrinho impedem duplicação da mesma seleção; idempotência de pagamentos fica na E6. Revisão conjunta dos contratos da onda 0 não será declarada feita por esta implementação.

## Decisions

### API e confirmação explícita

`CheckoutController` em `/api/clientes/me/checkout` delega para `CheckoutFacade`, com CLIENTE exigido tanto na aplicação quanto na web. A fachada resolve cliente pelo JWT, seguindo o padrão atual de `GerenciarCarrinho`; não recebe clienteId do consumidor.

- `POST /previa`: corpo `{ "enderecoId": 123 }`; responde 200 com `PreviaCheckout`.
- `POST` na raiz: corpo `{ "enderecoId": 123, "confirmacao": <detalhamento da previa> }`; responde 201, Location `/api/clientes/me/pedidos/{id}` e `PedidoResponse` existente.
- Prévia inclui `versaoCarrinho`, itens comerciais (SKU, nome, descrição, quantidade, preço e subtotal), descontos, subtotal, frete, total, endereço copiado e insuficiências. `ConfirmacaoCheckout` contém os mesmos campos comerciais e versão; exclui insuficiências, que são informativas e revalidadas pela reserva. O DTO da prévia expõe esse objeto de confirmação pronto para reenvio, evitando transcrição manual.

Valores enviados são apenas expectativa de confirmação: o pedido usa exclusivamente dados recalculados. Comparar listas canônicas por SKU e valores monetários normalizados, não apenas o total; incluir endereço e versão para detectar alteração da seleção e do destino. Requisição sem confirmação ou com campos inválidos recebe 400. A configuração JSON desabilita ACCEPT_FLOAT_AS_INT para rejeitar truncamento de ids, versões e quantidades; a regressão completa verifica compatibilidade com as demais APIs. Versão obsoleta ou diferença comercial recebe `CheckoutAlteradoException`, derivada de `ConflitoException`, com `divergencias` (campos e valores anterior/atual) e `previaAtualizada`. Endereço ausente/inativo/alheio responde 404; carrinho vazio e item ausente/inativo respondem 409 (SKUs em propriedade adicional). Não reutilizar silenciosamente preços de referência de itens ausentes.

Escolha de prévia explícita resolve a divergência entre UC-07 A1 (decisão ainda aberta na documentação) e RN-22 (exige reconfirmação): adotar RN-22 e atualizar UC-07 na mesma entrega. Alternativa de comparar apenas total rejeitada porque variações opostas em duas linhas poderiam ocultar mudanças; token opaco persistido rejeitado por exigir armazenamento/expiração sem necessidade nesta etapa. Uma confirmação fornecida pelo cliente não autoriza preços próprios.

### Revalidação e snapshot comercial

`RevalidarCheckout` carrega carrinho, endereço e catálogo correntes e monta `CompraRevalidada`: prévia, dados comerciais e componentes a reservar, todos derivados da mesma leitura. Consulta tolerante do catálogo serve para identificar todos os SKUs ausentes e rejeitar; itens inativos, inclusive componentes inativos de kit, também bloqueiam. Calcula preços através de `PipelinePreco` com contexto do cliente e UF do endereço. Não chama `GerenciarCarrinho.consultar` e depois relê linhas para criar o pedido, evitando duas representações divergentes na mesma operação.

Adicionar descrição comercial polimórfica ao contrato de `ItemVendavel`: implementação padrão sem descrição; `VariacaoProduto` devolve atributos ordenados por nome, em formato estável `nome: valor`. Expor esse campo de modo aditivo em `ItemVendavelInfo`, preservando construtor de conveniência existente para consumidores/testes. `ConsultarItensVendaveis` chama o contrato sem condicionais por tipo. Esse dado alimenta `DadosNovoPedido.Item.descricaoVariacao`; kit conserva nome e preço próprios. Não carregar entidades concretas de catálogo no checkout.

Expandir componentes de cada linha e agregar por variação com multiplicação e soma verificadas em `long`, rejeitando demanda final acima de `Integer.MAX_VALUE` antes de converter para `ComponenteAReservar`. Entregar ao estoque um componente por variação evita o `Integer::sum` não verificado existente no serviço ao consumir o checkout. Saldo informativo insuficiente pode aparecer na prévia, mas a reserva condicional é a autoridade na conclusão.

O `CalculoPreco` é a fonte de totais. Nesta etapa descontos vazios e frete zero; não habilitar handlers de desconto/frete inexistentes. Uma extensão de teste do pipeline pode observar o contexto e manter a aritmética da etapa, demonstrando execução sem mudar o checkout. Regras e mapeamento de origem de desconto serão integrados com os handlers reais na E5, sem presumir que todo desconto futuro vem de promoção.

### Transação e concorrência

`CheckoutFacade.concluir` é `@Transactional`: resolve identidade, revalida e compara confirmação, cria CRIADO via `CriarPedido` para obter id, reserva via `ReservarEstoque`, confirma via `ConfirmarReservaDoPedido`, esvazia o agregado e persiste/flush via `CarrinhoRepository`. Retorna o pedido para conversão em DTO. A reserva precede a confirmação, mas precisa suceder à criação técnica do registro porque exige pedidoId. Tudo participa da transação da fachada; nenhuma operação usa REQUIRES_NEW. Falhas desfazem inclusive pedido, histórico e reservas já gravados por flush. O tratador global também converte falhas otimistas detectadas por autoflush de consultas em 409, pois o CRUD do carrinho pode detectar o conflito antes de seu salvar explícito.

Adicionar `Carrinho.esvaziar()` incrementando revisão quando houver itens, preservando versão otimista já usada pelo adaptador JPA. Duas conclusões podem progredir até a reserva, mas apenas uma grava a mesma versão do carrinho; a perdedora retorna 409 e reverte seu pedido e estoque. Se a segunda iniciar depois do commit, encontra carrinho vazio e retorna 409. Escritas concorrentes de CRUD também disputam a versão; não remover linhas por SQL que contorne o agregado. Confirmar essa propriedade por integração real com transações/threads independentes, inclusive falha ao persistir a limpeza. Nenhuma restrição ArchUnit existente precisa ser enfraquecida.

A leitura corrente constitui a referência comercial da compra; mudanças ocorridas depois dessa leitura pertencem a operações posteriores e não modificam o snapshot. Estoque recebe proteção adicional pela atualização condicional. Não há bloqueio global de atualizações de catálogo/endereço durante a compra.

### Design Patterns

| Pattern | Papel | Problema do enunciado | Classes participantes | Modificação facilitada |
|---|---|---|---|---|
| Facade | Fachada, subsistemas, cliente | Checkout revalida vários módulos e precisa manter atomicidade | `CheckoutFacade`, `RevalidarCheckout`, `PipelinePreco`, `CriarPedido`, `ReservarEstoque`, `ConfirmarReservaDoPedido`, `CheckoutController` | Evoluir coordenação sem duplicá-la na web nem acoplar pedido ao estoque |
| Composite (existente) | Consumidor do contrato uniforme | Variantes e kits têm preço comercial e composição física | `RevalidarCheckout`, `ConsultarItensVendaveis`, `ItemVendavelInfo`, `ItemVendavel`, `VariacaoProduto`, `Combo` | Comprar novo tipo vendável via contrato comum |
| Chain of Responsibility (existente) | Cliente da cadeia | Preço é calculado em etapas evolutivas | `RevalidarCheckout`, `PipelinePreco`, `EtapaSubtotal`, `EtapaTotal` | Incluir etapas de cálculo sem alterar o fluxo HTTP |
| State/Observer (existentes) | Consumidor da transição e reações | Pedido muda de estado e estoque reage sem dependência reversa | `ConfirmarReservaDoPedido`, `Pedido`, estados existentes, `OuvinteDePedidosDoEstoque` | Reaproveitar transições e ouvintes para pedidos reais |

Alternativas descartadas: orquestração no controller para Facade mistura HTTP e transação; `instanceof`/switch de kit/variação para Composite acopla checkout a tipos concretos; soma manual no checkout para Chain duplica cálculo; alterações diretas de situação e chamadas diretas de liberação no pedido para State/Observer duplicam regras já implementadas. Validação de DTOs, comparação de confirmação, aritmética verificada, resolução de identidade e limpeza de carrinho não usam pattern próprio. Documentar participantes reais em docs/04 e Javadoc, sem criar interfaces artificiais só para contar patterns.

## Risks / Trade-offs

- Confirmação detalhada aumenta o corpo HTTP → objeto pronto na prévia, schemas e exemplos reproduzíveis; comparar somente os campos comerciais definidos.
- Conflito de versão pode ocorrer após reserva → mesma transação e teste de rollback após falha na limpeza, incluindo histórico e pedido.
- Ouvintes síncronos recebem eventos antes do commit → nesta etapa são efeitos transacionais de banco; não adicionar efeito externo irreversível. Notificações da E8 exigirão sua própria decisão.
- Nova descrição no contrato é aditiva, mas atinge consumidores → preservar construção atual, testar catálogo/carrinho e registrar a evolução em docs/03.
- Frete/pagamento/promoções ainda sem serviços concretos → API documenta a fronteira atual; E5–E7 integrarão seus dados sem afirmar suporte antecipado.

## Migration Plan

Sem migração de banco prevista: carrinho já possui versão e pedido já guarda descrição e endereço. Não alterar V1–V4. Verificar subida/migração em PostgreSQL do zero, regressão de carrinho e contratos, testes de checkout e `./mvnw -o verify`. Se a implementação revelar necessidade de esquema adicional, revisar o design antes de criá-lo. Rollback do aplicativo remove as rotas novas; pedidos e reservas já concluídos continuam válidos nos módulos existentes. Entrega para revisão do autor sem commit ou arquivamento automático.
