## Purpose

Comunicar ao cliente os acontecimentos confirmados de sua compra por canais simulados extensíveis, isolando falhas de envio dos efeitos comerciais.

## ADDED Requirements

### Requirement: Comunicação do ciclo da compra
O sistema SHALL enviar ao cliente uma mensagem por canal ativo após confirmar a criação, aprovação ou recusa do pagamento, envio e cancelamento do pedido. A mensagem MUST identificar pedido e acontecimento sem expor credenciais ou dados de pagamento.

#### Scenario: Compra confirmada
- **WHEN** o checkout confirma sua transação
- **THEN** os canais ativos recebem a mensagem de criação destinada ao cliente do pedido

#### Scenario: Eventos relevantes
- **WHEN** uma aprovação, recusa, envio ou cancelamento é confirmado
- **THEN** o cliente recebe mensagem correspondente ao acontecimento e ao pedido correto

### Requirement: Isolamento transacional e de falhas
O envio MUST ocorrer apenas após confirmação da transação comercial. Uma falha de canal ou de preparação da mensagem MUST ser registrada com pedido e acontecimento, sem desfazer pedido, pagamento, estoque ou cupom. A falha de um canal MUST NOT impedir os demais canais de receber a mensagem.

#### Scenario: Rollback
- **WHEN** uma operação publica um acontecimento mas sua transação é revertida
- **THEN** nenhum canal recebe mensagem desse acontecimento

#### Scenario: Falha parcial
- **WHEN** um canal falha ao comunicar um pagamento aprovado
- **THEN** a falha é registrada, os demais canais são executados e pedido e baixa de estoque permanecem confirmados

### Requirement: Repetição de confirmação sem mensagem duplicada
Uma confirmação de pagamento já processada MUST NOT produzir nova notificação. A E8 MUST preservar as garantias de repetição sequencial e concorrente do pagamento.

#### Scenario: Confirmação repetida
- **WHEN** o mesmo resultado de pagamento aceito é recebido novamente
- **THEN** há uma única comunicação da aprovação e uma única baixa de estoque

#### Scenario: Confirmações concorrentes
- **WHEN** duas confirmações idênticas chegam simultaneamente
- **THEN** apenas a confirmação efetivada produz comunicação

### Requirement: Extensão independente dos interessados
Um novo canal ativo ou interessado nos acontecimentos SHALL poder ser registrado sem alterar pedido, pagamento ou canais existentes.

#### Scenario: Novo canal
- **WHEN** um canal adicional é registrado e um pagamento é aprovado
- **THEN** o novo canal e os anteriores recebem a comunicação, preservando o processamento comercial

#### Scenario: Novo interessado
- **WHEN** um interessado adicional é registrado para cancelamentos
- **THEN** recebe o acontecimento com pedido e estado anterior sem modificar a publicação existente
