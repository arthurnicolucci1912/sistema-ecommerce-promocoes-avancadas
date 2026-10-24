# Spec Delta

## Purpose
Permitir pagamentos simulados seguros e rastreáveis, com confirmação única e estorno recuperável, preservando a consistência comercial de cada pedido.

## ADDED Requirements

### Requirement: Cobrança do próprio pedido
O cliente SHALL iniciar e consultar cobrança do próprio pedido com CARTAO_CREDITO, PIX ou BOLETO. O valor MUST vir do total preservado no pedido. A primeira cobrança MUST exigir AGUARDANDO_PAGAMENTO. Repetição com a mesma forma SHALL retornar a cobrança existente sem cobrar novamente; troca de forma MUST retornar 409. Visitante MUST receber 401, perfil indevido 403 e pedido de terceiro 404. Dados inválidos MUST retornar 400.

#### Scenario: Cobrança repetida
- **WHEN** o dono inicia duas vezes PIX para o mesmo pedido de R$ 90,00
- **THEN** recebe a mesma transação de R$ 90,00, inicialmente pendente, sem segunda cobrança

#### Scenario: Pedido de terceiro
- **WHEN** um cliente tenta pagar ou consultar a cobrança de outro cliente
- **THEN** recebe 404 sem efeito

### Requirement: Resultados dos meios simulados
O sistema SHALL oferecer cartão com resultado síncrono configurável para aprovação ou recusa em ambiente simulado e PIX/boleto pendentes de confirmação. Resultados terminais MUST passar pelas mesmas garantias de confirmação. O cliente MUST NOT escolher um resultado de pagamento no corpo da compra.

#### Scenario: Cartão aprovado
- **WHEN** o fornecedor simulado retorna aprovação da cobrança por cartão
- **THEN** o pedido fica PAGO e sua reserva é baixada uma única vez

#### Scenario: Recusa
- **WHEN** uma cobrança é recusada
- **THEN** o pedido fica PAGAMENTO_RECUSADO, reserva e uso de cupom são devolvidos uma única vez e o snapshot permanece intacto

### Requirement: Confirmação autenticada e idempotente
A integração simulada SHALL aceitar confirmação aprovada ou recusada apenas de ADMIN autenticado, vinculada a cobrança existente pelo identificador externo único. Confirmação idêntica repetida MUST responder sucesso sem repetir transição, histórico, eventos ou efeitos. Reutilizar o identificador com resultado divergente MUST responder 409. Identificador desconhecido MUST responder 404.

#### Scenario: Entregas simultâneas
- **WHEN** duas confirmações aprovadas da mesma transação chegam simultaneamente
- **THEN** ambas terminam com sucesso e apenas uma transição, um evento de pedido pago e uma baixa são produzidos

#### Scenario: Conteúdo conflitante
- **WHEN** a mesma transação já aprovada recebe recusa
- **THEN** recebe 409 sem efeitos adicionais

#### Scenario: Cliente tenta confirmar
- **WHEN** um CLIENTE tenta enviar confirmação externa
- **THEN** recebe 403 e o pedido continua inalterado

### Requirement: Atomicidade e disputas
Registro da confirmação e efeitos comerciais SHALL formar uma única operação persistente. Falha de estoque, cupom ou publicação MUST desfazer o processamento para permitir repetição. Confirmação inédita de cobrança cujo pedido já está cancelado MUST retornar 409 sem alterar pedido nem registrar confirmação aceita. Pagamento e cancelamento concorrentes MUST preservar uma sequência válida, sem estoque negativo ou devolução duplicada.

#### Scenario: Falha após transição
- **WHEN** o processamento falha depois de mudar o estado mas antes de concluir seus efeitos
- **THEN** pedido, confirmação, histórico, estoque e cupom retornam à condição anterior e a confirmação pode ser reenviada

#### Scenario: Confirmação após cancelamento
- **WHEN** chega aprovação inédita de cobrança pendente de pedido cancelado
- **THEN** recebe 409 sem reabrir pedido ou baixar estoque

### Requirement: Estorno único e recuperável
Cancelamento de pedido pago ou em preparação SHALL solicitar estorno integral ao fornecedor da cobrança original. Sucesso MUST ficar registrado com identificador externo e MUST NOT ser repetido. Falha simulada MUST manter o cancelamento e as devoluções de estoque/cupom, registrar pendência consultável e permitir nova tentativa exclusiva de ADMIN. Cancelamento sem pagamento aprovado MUST NOT gerar estorno.

#### Scenario: Estorno realizado
- **WHEN** ADMIN cancela pedido pago de R$ 90,00
- **THEN** o pedido fica CANCELADO, o estoque é reposto, o cupom devolvido e existe um único estorno de R$ 90,00

#### Scenario: Falha e nova tentativa
- **WHEN** o estorno falha e ADMIN solicita nova tentativa que tem sucesso
- **THEN** o pedido permanece CANCELADO, a pendência fica resolvida e estoque e cupom não são devolvidos novamente

### Requirement: Substituição de fornecedor
Um fornecedor simulado registrado para uma forma suportada SHALL substituir o anterior por configuração sem alterar regras de compra, pedido ou confirmação. Cobranças anteriores MUST continuar associadas ao fornecedor original para seu estorno.

#### Scenario: Outro fornecedor de cartão
- **WHEN** outro fornecedor de cartão é registrado e selecionado
- **THEN** novas cobranças usam o novo fornecedor e confirmações e estornos preservam o comportamento contratado
