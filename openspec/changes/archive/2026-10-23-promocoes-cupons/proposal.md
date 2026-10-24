# Proposal

## Why

O checkout já cria pedidos com reserva atômica, mas a compra ainda não aplica campanhas nem cupons. A E5 entrega os descontos configuráveis e os pontos de extensão centrais do enunciado, usando o carrinho, a cadeia de preço e a confirmação comercial existentes.

## What Changes

- Administração de campanhas e cupons para ADMIN/PROMOCOES: configuração persistente, vigência, condições, prioridade/acumulação, ativação e desativação.
- Promoções de leve X/pague Y por SKU, percentual por categoria, valor fixo acima de mínimo, percentual na primeira compra e frete grátis; árvores de condições E/OU/NÃO e política D4 trocável. Novos tipos entram por registro, sem alterar os existentes.
- Primeira compra significa não haver pagamento aprovado anterior, inclusive no histórico de pedido depois cancelado. Pedidos recusados/cancelados antes de pagar não eliminam a elegibilidade, conforme a orientação do autor de favorecer descontos.
- Aplicação/remoção de um cupom persistido no carrinho, benefícios percentual, fixo e de frete, rejeições com motivo e preservação do cupom anterior quando uma substituição falhar.
- Descontos discriminados por origem e identidade no cálculo, na confirmação e no snapshot do pedido; mudança no detalhamento por campanha/cupom exige nova confirmação, sem usar preços fornecidos pelo cliente.
- Uso de cupom consumido somente no checkout, com limite total e limite opcional por cliente protegidos sob concorrência, rollback integral e devolução idempotente por cancelamento/recusa.
- Frete continua zero na API de produção até a E7. Os benefícios de frete ficam implementados e testáveis com cotação simulada de entrada, sem introduzir seleção de transportadora.

## Capabilities

### New Capabilities

- `promocoes`: campanhas configuráveis, condições combináveis, benefícios, política de conflito e extensibilidade.
- `cupons`: cadastro, validação, benefícios, consumo concorrente e devolução por pedido.

### Modified Capabilities

- `carrinho`: persistir um cupom, permitir sua aplicação/remoção e mostrar descontos reais com recálculo e avisos recuperáveis.
- `formacao-preco`: incluir promoções e cupom na ordem definida, respeitando bases, tetos e origem dos descontos.
- `checkout`: revalidar descontos/cupom, preservar seu detalhamento e incluir consumo do cupom na transação da compra.

Os requisitos principais de pedido e estoque não mudam; os tipos de snapshot evoluem de forma aditiva para preservar referências dos descontos, e os eventos existentes são reutilizados. As specs de E3/E4b foram sincronizadas sem arquivamento para permitir deltas sobre a base real.

## Impact

Módulo `promocao` em domínio/aplicação/infra/web, handlers de preço, migração V5 aditiva, evolução compatível dos tipos da onda 0, do resumo do carrinho e da confirmação de checkout. Integração pontual com consultas do histórico do pedido e seus eventos; preservação das fronteiras existentes e refinamento delimitado de ArchUnit para implementações de negócio de promoções. Atualização de docs/02–05 e 07–10, roadmap, board, demonstração e testes. Gateways de pagamento, transportadoras e auditoria administrativa permanecem nas E6–E8; sem commit ou arquivamento automáticos.

## Design Patterns

- Strategy: tipos de promoção, benefícios de cupom e política de conflito, justificados pelos RF-27/28/31 e RF-34; varia o cálculo de benefício e a seleção de campanhas.
- Composite/Specification: condições E/OU/NÃO, RF-29/30; variam folhas e combinações sem um método central de condicionais.
- Factory Method com registro: materialização dos tipos configurados, RF-32; nova estratégia e fábrica são registradas sem editar o motor.
- Chain of Responsibility: passos de promoção/cupom na cadeia já existente, RF-36/37; novos handlers não alteram subtotal e total.
- Observer: devolução de uso do cupom por eventos do pedido, RN-07/26; novo efeito sem acoplar pedido a cupom. Facade existente mantém a atomicidade do checkout.

CRUD, normalização de código, validações de dados, contadores e persistência não precisam de patterns próprios.
