# Design

## Context

O motivo da change está em `proposal.md`, e o comportamento nas specs `catalogo` e `estoque`. Já existem:
- a fundação: `compartilhado` (`Dinheiro`, `Pagina`, exceções com propriedades genéricas, `PublicadorEventos`), segurança JWT e regras ArchUnit, inclusive "compartilhado não depende de módulos";
- o módulo `pedido`, que publica `PedidoPago`, `PedidoPagamentoRecusado` e `PedidoCancelado` (com o campo `de`) dentro da transação do serviço;
- a última migração, `V2`.

Decisões confirmadas com o autor:
- entra o combo básico;
- a consulta do catálogo é pública;
- categorias são uma árvore simples, sem Composite.

Há também decisões dos documentos que estavam em aberto e são fechadas aqui:
- a concorrência do estoque é resolvida nesta etapa (RN-21);
- a tabela de reservas nasce nesta etapa;
- os nomes do Composite são padronizados.

## Goals / Non-Goals

**Goals:**
- Um contrato único de item vendável, para que carrinho, checkout e promoções não distingam variação de combo.
- Reserva correta sob concorrência e efeitos de estoque idempotentes, dirigidos por eventos.
- `estoque` sem dependência de `catalogo`, e `catalogo` sem dependência de `pedido`.

**Non-Goals:**
- Expiração de reserva (etapa 6).
- Histórico e auditoria de preço (etapa 8).
- Imagens, dimensões e peso. O peso fica para a etapa 7, se o frete precisar.
- Busca textual avançada.

## Decisions

### D1. Composite do item vendável (catálogo)
Hierarquia JPA `JOINED`, com raiz `ItemVendavel` (tabela `item_vendavel`). Campos: `id`, `sku` (único no catálogo inteiro), `nome`, `preco` (`Dinheiro`, > 0), `ativo` e `tipo` (discriminador).

Operações do componente:
- `preco()`;
- `estaDisponivelParaVenda()`;
- `componentesDeEstoque(int quantidade)`, que devolve `List<ComponenteEstoque(variacaoId, quantidade)>`;
- `categoriasAncestrais()`.

Participantes:
- **Folha `VariacaoProduto`** (tabela `variacao_produto`): pertence a um `Produto`, tem `atributos` (`@ElementCollection` nome/valor), e `componentesDeEstoque(q)` devolve `[(this.id, q)]`.
- **Composto `Combo`** (tabela `combo`): tem `itens: List<ItemCombo(item: ItemVendavel, quantidade)>`. `componentesDeEstoque(q)` soma recursivamente `item.componentesDeEstoque(q × quantidade)`, agregando variações repetidas. `estaDisponivelParaVenda()` exige o combo e todos os componentes ativos.
- **Cliente:** quem consome o catálogo recebe só `ItemVendavel` pelo serviço `ConsultarItensVendaveis.porSku(sku)` e chama `componentesDeEstoque`, sem `instanceof`.

A validação de ciclo no cadastro do combo percorre os componentes e rejeita o próprio combo (409). O `Produto` não é item vendável: ele agrupa variações e carrega `nome`, `descricao`, `marca` e `categoria`.

Alternativas descartadas:
- Flag `ehCombo` dentro de `Produto`, com condicionais no carrinho, no preço e no estoque. Cada consumidor precisaria de `if`, e um terceiro tipo de item mexeria em todos.
- Combo sem SKU próprio. O carrinho e as promoções precisam endereçá-lo como qualquer item.

### D2. Categorias (sem pattern)
Tabela `categoria` (`id`, `nome`, `pai_id`), com nome único por pai (índice único em `coalesce(pai_id, 0)`, `lower(nome)`). Mover valida que o novo pai não é a própria categoria nem uma descendente. Os descendentes e ancestrais são calculados em memória a partir da árvore inteira, porque a quantidade de categorias é pequena e evita CTE específica do banco. Se crescer, troca-se por CTE recursiva atrás do mesmo serviço.

### D3. Saldo e concorrência (estoque)
Tabela `saldo_estoque` (`variacao_id` PK, `disponivel >= 0`, `reservado >= 0`, ambos com `CHECK`).

A reserva é uma **atualização condicional atômica** (RN-21):

```sql
update saldo_estoque set disponivel = disponivel - :q, reservado = reservado + :q
where variacao_id = :v and disponivel >= :q
```

Zero linhas afetadas lança `EstoqueInsuficienteException`, que estende `ConflitoException` e traz as propriedades `variacaoId` e `disponivel`. Como tudo roda em uma transação, a exceção desfaz as reservas anteriores (tudo-ou-nada). Os componentes são processados ordenados por `variacaoId` para evitar deadlock entre pedidos que disputam as mesmas variações.

O ajuste usa a mesma técnica: `disponivel + :delta >= 0`.
- Alternativa descartada: lock otimista com `@Version` e retry. Gera retentativas sob disputa e mais código, enquanto o UPDATE condicional resolve em uma ida ao banco.
- Alternativa descartada: lock pessimista. Serializa leituras desnecessariamente.

### D4. Reservas por pedido
Tabela `reserva_estoque`: `id`, `pedido_id`, `variacao_id`, `quantidade`, `situacao` (`ATIVA`, `LIBERADA`, `BAIXADA`, `REPOSTA`) e `atualizado_em`, com unicidade em `(pedido_id, variacao_id)`. As transições:
- `liberar(pedidoId)`: só `ATIVA` vai para `LIBERADA`, e o reservado volta para o disponível;
- `baixar(pedidoId)`: só `ATIVA` vai para `BAIXADA`, e o reservado diminui;
- `repor(pedidoId)`: só `BAIXADA` vai para `REPOSTA`, e o disponível aumenta.

A idempotência vem da troca de situação condicional (`update ... where situacao = 'ATIVA'`), e uma repetição não tem efeito. O `pedido_id` não tem FK para `pedido`, para o `estoque` não depender do esquema do pedido. É só a referência.

A reserva usa o `pedidoId`: o checkout vai criar o pedido em `CRIADO`, reservar e confirmar a reserva na mesma transação. O serviço é `ReservarEstoque.para(pedidoId, componentes)`.

### D5. Observer: ouvintes do estoque
`OuvinteDePedidosDoEstoque` (pacote `estoque.aplicacao`) usa `@EventListener` **síncrono**, ou seja, na mesma transação de quem publicou:
- `PedidoPago`: `baixar`;
- `PedidoPagamentoRecusado`: `liberar`;
- `PedidoCancelado` com `de = AGUARDANDO_PAGAMENTO`: `liberar`;
- `PedidoCancelado` com `de` em `PAGO` ou `EM_PREPARACAO`: `repor`.

Os casos de `de` são resolvidos por um mapa `situação de origem → ação`, sem `switch`. O síncrono foi escolhido para o pedido e o estoque nunca divergirem. A alternativa, `@TransactionalEventListener(AFTER_COMMIT)`, foi descartada porque um erro depois do commit deixaria pedido cancelado com reserva presa.

O `estoque` depende de `pedido.dominio.evento`, que é o contrato público do evento. O `pedido` não depende do `estoque`.

O `catalogo` publica `VariacaoCadastrada(variacaoId)`, e o estoque cria o saldo zero. Assim o `estoque` não precisa consultar o `catalogo`.

A dependência inversa é permitida: `catalogo` consulta `ConsultarSaldo` do `estoque` para mostrar a disponibilidade. A disponibilidade de um combo é o mínimo de `disponivel / quantidade` entre os componentes.

### D6. API e segurança
**Públicos (`GET`):**
- `/api/catalogo/categorias` (árvore);
- `/api/catalogo/produtos?categoriaId&nome&precoMin&precoMax&page&size`;
- `/api/catalogo/produtos/{id}`;
- `/api/catalogo/combos` e `/api/catalogo/combos/{id}`.

**`ADMIN` e `OPERADOR_COMERCIAL`:**
- `/api/admin/categorias` (`POST`; `PUT /{id}` para nome e pai; `DELETE /{id}`);
- `/api/admin/produtos` (`POST` com variações; `PUT /{id}`; `POST /{id}/variacoes`; `PUT /variacoes/{id}`, que altera o preço; `POST /{id}/inativacao`; `POST /variacoes/{id}/inativacao`);
- `/api/admin/combos` (`POST`, `PUT /{id}`, `POST /{id}/inativacao`).

**`ADMIN` e `ESTOQUE`:**
- `GET /api/admin/estoque/{variacaoId}`;
- `GET /api/admin/estoque?abaixoDe=`;
- `POST /api/admin/estoque/{variacaoId}/ajustes` com `{quantidade, motivo}`.

As regras de rota ficam em `ConfiguracaoSeguranca`, antes de `/api/admin/**`, mais `@PreAuthorize` nos casos de uso.

### D7. Contrato para os próximos módulos
`ConsultarItensVendaveis` (pacote `catalogo.aplicacao`) expõe:
- `porSku(sku)`, que devolve a visão `ItemVendavelInfo(id, sku, nome, preco, ativo, categoriaIds ancestrais, componentes)`;
- `porSkus(Collection)`.

O `ItemPreco` das promoções é montado a partir dessa visão na etapa 3. `ReservarEstoque` e `ConsultarSaldo` (pacote `estoque.aplicacao`) são o contrato do checkout.

### D8. Demonstração
O `UsuariosDeDemonstracao` passa a semear, de forma idempotente por SKU:
- categorias Roupas > Camisetas, Casa > Cozinha e Eletrônicos > Periféricos;
- Camiseta Básica (P, M e G, cor azul), Caneca de Cerâmica 300 ml, Café Especial 250 g e Teclado Mecânico;
- o combo "Kit Café" (1 caneca + 2 cafés);
- estoque inicial, com o Teclado em **1 unidade**, para demonstrar a disputa da última unidade.

O pedido de demonstração passa a usar os SKUs reais e a reservar estoque.

### D9. Design Patterns

| Pattern | Papel | Problema do enunciado | Classes participantes | O que fica mais fácil |
|---|---|---|---|---|
| Composite | Componente | Combos e kits; novos tipos de produto | `ItemVendavel` | Um novo tipo de item vendável é uma subclasse, e carrinho, preço e estoque não mudam |
| Composite | Folha | idem | `VariacaoProduto` | idem |
| Composite | Composto | idem | `Combo`, `ItemCombo` | Kits de kits sem código extra |
| Observer | Ouvintes | Vários componentes reagem ao pagamento e ao cancelamento | `OuvinteDePedidosDoEstoque`, `OuvinteDeCatalogoDoEstoque` | Um novo efeito de estoque é um novo método ouvinte, sem tocar `pedido` nem `catalogo` |
| Observer | Eventos publicados | idem | `VariacaoCadastrada` (catálogo), eventos do `pedido` | idem |

As alternativas descartadas estão em D1 e D5.

**Sem pattern próprio:** categorias (D2), ajuste e consultas de estoque, e os controllers.

## Risks / Trade-offs

- [`@EventListener` síncrono faz uma falha no estoque desfazer a transição do pedido] → É o comportamento desejado: a consistência vem antes da disponibilidade. Os ouvintes são idempotentes e não lançam erro para pedido sem reserva.
- [O mapeamento `JOINED` gera joins nas consultas de itens] → O volume é pequeno. A listagem pública consulta `Produto` com as variações por fetch.
- [Combos aninhados profundos] → A validação de ciclo garante término. A profundidade não é limitada, por simplicidade.
- [O pedido de demonstração existente no banco `dev` não tem reserva] → Os ouvintes ignoram pedidos sem reserva. Para reiniciar os dados, use `docker compose down -v`, já documentado.

## Migration Plan

A migração `V3` cria `categoria`, `produto`, `item_vendavel`, `variacao_produto`, `variacao_atributo`, `combo`, `combo_item`, `saldo_estoque` e `reserva_estoque`. Não há dados a migrar. Rollback: em desenvolvimento, `docker compose down -v`.
