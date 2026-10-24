# Proposal

## Why

O carrinho está implementado e foi aprovado pelo autor, mas ainda não pode originar uma compra. A etapa 4b do roadmap conecta os contratos existentes de catálogo, preços, clientes, pedido e estoque, entregando um pedido aguardando pagamento com reserva atômica.

## What Changes

- Consulta de prévia do checkout e conclusão pelo cliente autenticado, com endereço ativo próprio e carrinho não vazio.
- Revalidação dos itens e preços correntes; mudanças no detalhamento confirmado retornam conflito com divergências e prévia atualizada, sem criar pedido nem reservar estoque (RN-22).
- Snapshot comercial do pedido com endereço copiado, preço próprio de kits e quantidades físicas agregadas de todos os componentes.
- Uma transação para criar pedido, reservar tudo-ou-nada, confirmar a reserva e esvaziar o carrinho; concorrência não pode consumir a mesma seleção duas vezes nem perder alterações de carrinho.
- Exercitar cancelamento e recusa sobre reservas reais usando os ouvintes já existentes; atualizar documentação, exemplos e testes.

## Capabilities

### New Capabilities

- `checkout`: prévia, confirmação de valores, validação de propriedade, criação atômica de pedido e consumo do carrinho.

### Modified Capabilities

Nenhuma. Os requisitos existentes de pedido e estoque permanecem válidos; esta change integra seus serviços. A limpeza do carrinho é especificada como efeito da conclusão do checkout, preservando o CRUD já aprovado em `carrinho-compras`.

## Impact

Novo módulo `checkout` nas camadas aplicação/web, reutilizando os serviços e portas existentes. Extensão pontual do agregado do carrinho para esvaziamento com controle de versão. API em `/api/clientes/me/checkout`; documentação 02–05 e 07–10, board e Javadoc dos participantes. Testes JUnit, MockMvc, PostgreSQL/Testcontainers e ArchUnit. Sem dependências externas novas previstas, sem cobrança automática; promoções/cupons, gateway e entrega continuam nas etapas 5, 6 e 7. A simulação administrativa de pagamento permanece até a etapa 6.

## Design Patterns

Facade resolve a orquestração do checkout exigida no enunciado sem levar a sequência transacional para o controller. O ponto de evolução é a coordenação dos subsistemas: preço, reserva, pedido e, nas etapas posteriores, pagamento e entrega. Reutiliza Composite para itens vendáveis e Chain of Responsibility para cálculo, sem discriminar tipos no checkout; State e Observer já cuidam do pedido e das reações do estoque. As validações de entrada, identidade e persistência não justificam novos patterns.
