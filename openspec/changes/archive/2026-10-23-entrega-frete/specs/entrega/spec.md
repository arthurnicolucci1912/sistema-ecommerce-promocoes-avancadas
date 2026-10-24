# Spec Delta

## Purpose
Oferecer opções simuladas de entrega com custo e prazo conhecidos, permitindo selecionar uma cotação válida e preservar os compromissos comerciais da compra.

## ADDED Requirements

### Requirement: Opções de entrega para endereço próprio
O sistema SHALL permitir que CLIENTE autenticado consulte opções de entrega para seu carrinho não vazio e endereço ativo próprio. Cada opção MUST identificar código estável, fornecedor, modalidade, custo bruto e prazo estimado positivo em dias úteis após aprovação do pagamento. Consultar MUST NOT criar pedido, reservar estoque ou consumir cupom. Sem autenticação MUST responder 401, perfil indevido 403, endereço alheio/inexistente/inativo 404, entrada inválida 400 e carrinho vazio 409.

#### Scenario: Comparar econômica e expressa
- **WHEN** um comprador consulta opções para um endereço atendido pelas duas modalidades
- **THEN** recebe opções identificadas com custos e prazos, sem alteração de seleção, estoque ou utilizações de cupom

#### Scenario: Endereço de outro comprador
- **WHEN** o comprador consulta opções usando endereço de terceiro
- **THEN** recebe 404 sem revelar cotações desse endereço

### Requirement: Cotação regional simulada e cobertura
O sistema SHALL oferecer modalidades econômica e expressa com custos/prazos regionais determinísticos. Econômica no Sudeste SHALL custar R$ 25,00 e no Nordeste R$ 40,00 na configuração de demonstração. Apenas opções que atendam ao destino MUST ser oferecidas. Nenhum atendimento SHALL resultar em lista vazia e impedir compra; falha de fornecedor MUST NOT ser convertida em frete gratuito. Opção indisponível MUST NOT ser substituída silenciosamente.

#### Scenario: Comparar regiões
- **WHEN** o cliente consulta econômica para SP e para BA com a configuração de demonstração
- **THEN** vê respectivamente R$ 25,00 e R$ 40,00 de custo bruto

#### Scenario: Nenhuma cobertura
- **WHEN** nenhum fornecedor registrado atende ao destino
- **THEN** a consulta retorna lista vazia e nenhuma compra pode ser concluída sem opção válida

#### Scenario: Falha em todos os serviços que atenderiam
- **WHEN** todos os serviços capazes de atender falham na cotação
- **THEN** o sistema responde 503 sem oferecer custo zero ou concluir compra

### Requirement: Extensão de fornecedores e modalidades
Um novo fornecedor/modalidade registrado SHALL aparecer entre opções elegíveis sem alterações no fluxo de consulta, preço ou checkout. As identidades das opções MUST ser inequívocas; códigos repetidos MUST ser rejeitados na configuração.

#### Scenario: Nova transportadora
- **WHEN** uma nova transportadora é registrada para uma modalidade que atende SP
- **THEN** sua opção pode ser consultada e selecionada para comprar sem tratamento específico no checkout

### Requirement: Andamento da entrega preserva a compra
O cliente SHALL consultar o andamento do próprio pedido e a entrega escolhida com prazo estimado preservado. Preparação, envio e entrega SHALL seguir as permissões e transições existentes, sem mudar cotação, total ou pagamento. Prazo SHALL ser uma estimativa e MUST NOT provocar transição automática.

#### Scenario: Pedido pago até entrega
- **WHEN** uma compra com entrega expressa é paga, preparada, enviada e entregue pelos responsáveis autorizados
- **THEN** o cliente vê ENTREGUE e histórico completo com fornecedor, modalidade, prazo e valores da compra preservados
