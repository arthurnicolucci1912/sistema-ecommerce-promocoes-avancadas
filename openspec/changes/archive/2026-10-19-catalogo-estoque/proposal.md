# Proposal

## Why

Sem catálogo e estoque não há o que vender. O carrinho, o checkout e as promoções dependem de produtos com variações e preço, de categorias hierárquicas e de um estoque que impeça "dois clientes adquirirem simultaneamente a última unidade" (enunciado, seção Estoque). O pedido já existe e publica eventos de pagamento e cancelamento, mas ninguém reage a eles: a reserva, a liberação e a baixa de estoque precisam nascer aqui para que o fluxo de compra fique consistente.

## What Changes

- Módulo `catalogo`:
  - categorias em árvore de profundidade livre (sem ciclos);
  - produtos com variações (SKU, preço e atributos nome/valor) e inativação em vez de exclusão;
  - **combos/kits** com preço próprio, compostos por variações ou outros combos.
- Consulta **pública** do catálogo: árvore de categorias, produtos filtrados por categoria (incluindo subcategorias), nome e faixa de preço, detalhe com variações e disponibilidade, e combos.
- Administração do catálogo: `ADMIN` e `OPERADOR_COMERCIAL`.
- Módulo `estoque`:
  - saldo por variação (disponível e reservado);
  - ajuste administrativo (`ADMIN` e `ESTOQUE`), que nunca deixa o disponível negativo;
  - reserva tudo-ou-nada por pedido, atômica sob concorrência, com liberação e baixa idempotentes.
- O estoque reage aos eventos do pedido:
  - pagamento aprovado vira baixa;
  - pagamento recusado ou cancelamento antes do pagamento libera a reserva;
  - cancelamento depois do pagamento repõe as unidades.
- Serviço de consulta de itens vendáveis por SKU, para o carrinho, o checkout e as promoções: preço, categorias ancestrais e componentes de estoque.
- Carga de demonstração do catálogo e do estoque no perfil `dev`. O pedido de demonstração passa a usar SKUs reais e a reservar estoque.
- Fora de escopo:
  - carrinho e checkout (próximas changes);
  - expiração de reserva;
  - imagens e dimensões;
  - auditoria das alterações de preço (etapa 8).

## Design Patterns

| Pattern | Onde | Requisito do PDF que justifica |
|---|---|---|
| **Composite** (estrutural) | Item vendável: a variação de produto é a folha e o combo é o composto (pode conter variações e outros combos); preço, disponibilidade e componentes de estoque são tratados de forma uniforme | Carrinho: "representar conjuntos de produtos comercializados como combos ou kits"; Catálogo: "acrescentar novos tipos de produtos futuramente sem exigir mudanças excessivas" |
| **Observer** (comportamental), lado dos ouvintes | O estoque ouve os eventos do pedido (`PedidoPago`, `PedidoPagamentoRecusado`, `PedidoCancelado`) e o cadastro de variação (`VariacaoCadastrada`), sem que pedido ou catálogo conheçam o estoque | Eventos do sistema: "quando um pagamento é confirmado, diferentes componentes poderão precisar reagir… evitar que uma única classe conheça diretamente todos os componentes interessados" |

**Sem pattern próprio:** a árvore de categorias é uma hierarquia simples, sem comportamento que varie entre folha e nó. Chamá-la de Composite seria artificial, então roadmap e board são corrigidos. O ajuste de estoque e as consultas também são CRUD sem ponto de variação.

## Capabilities

### New Capabilities
- `catalogo`: categorias hierárquicas, produtos e variações, combos, consulta pública com filtros e disponibilidade, administração do catálogo por perfil.
- `estoque`: saldo por variação, ajuste administrativo, reserva tudo-ou-nada sob concorrência, liberação, baixa e reposição idempotentes, e reação aos acontecimentos do pedido.

### Modified Capabilities

Nenhuma. O comportamento observável do `pedido` não muda: os efeitos de estoque são novos ouvintes.

## Impact

- **Código novo:** módulos `br.com.fatec.ecommerce.catalogo` e `br.com.fatec.ecommerce.estoque`, e a migração Flyway `V3`.
- **API nova:**
  - pública: `/api/catalogo/**`;
  - administrativa: `/api/admin/categorias/**`, `/api/admin/produtos/**`, `/api/admin/combos/**` e `/api/admin/estoque/**`.
- **Segurança:** `GET /api/catalogo/**` público, mais regras de rota por perfil para a administração.
- **Demonstração:** `UsuariosDeDemonstracao` ganha a carga do catálogo e do estoque, e o pedido de exemplo passa a reservar estoque.
- **Documentação:** `docs/02`, `03`, `04` (§3.8 com as classes reais e §3.6 com os ouvintes), `05` (RN-17 a RN-21), `07`, `08`, `09`, `10`, `11` e `12` (remoção do "Composite de categorias").
