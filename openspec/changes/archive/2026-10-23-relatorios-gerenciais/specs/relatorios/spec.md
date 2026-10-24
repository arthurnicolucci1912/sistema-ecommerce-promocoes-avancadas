# Spec Delta

## Purpose

Fornece à administração indicadores gerenciais de vendas, produtos e campanhas de um período, calculados a partir dos pedidos já gravados, sem alterar nenhum dado. Os relatórios não têm ponto de variação previsto no enunciado; por isso não há requisito de extensibilidade nesta capability.

## ADDED Requirements

### Requirement: Critério de venda e período
O sistema SHALL considerar venda todo pedido que foi pago em algum instante do período informado, inclusive nas duas pontas, e que não está cancelado no momento da consulta. O valor de uma venda MUST ser o total do pedido. O período MUST ser informado por `inicio` e `fim`, instantes ISO-8601 com fuso, e o mesmo critério MUST valer para todos os relatórios.

#### Scenario: Pagamento dentro do período
- **WHEN** um pedido foi pago dentro do período, mesmo no instante exato de início ou de fim
- **THEN** o pedido é uma venda do período com valor igual ao seu total

#### Scenario: Pedido pago fora do período ou nunca pago
- **WHEN** um pedido foi pago antes do início, depois do fim, ou está aguardando pagamento ou com pagamento recusado
- **THEN** o pedido não é contado como venda do período

#### Scenario: Pedido pago e depois cancelado
- **WHEN** um pedido pago dentro do período foi cancelado depois
- **THEN** o pedido não é contado como venda

#### Scenario: Pedido enviado ou entregue
- **WHEN** um pedido pago dentro do período está em preparação, enviado ou entregue
- **THEN** o pedido continua sendo venda

### Requirement: Relatório de vendas
GET /api/admin/relatorios/vendas SHALL retornar, para o período, o valor total de vendas, a quantidade de pedidos vendidos, o ticket médio, o total concedido em descontos e a quantidade de cancelamentos. O ticket médio MUST ser o valor total dividido pela quantidade, arredondado a duas casas pelo arredondamento bancário, e zero quando não há vendas. O total de descontos MUST somar os descontos de itens e de frete, de promoções e de cupons, das vendas do período. A quantidade de cancelamentos MUST contar as transições para cancelado ocorridas no período, mesmo de pedidos que não chegaram a ser pagos.

#### Scenario: Indicadores sobre pedidos conhecidos
- **WHEN** o período contém duas vendas de 415,00 e 200,00, com descontos de 20,00 e 10,00
- **THEN** o valor total é 615,00, a quantidade é 2, o ticket médio é 307,50 e o total de descontos é 30,00

#### Scenario: Arredondamento do ticket médio
- **WHEN** o valor total dividido pela quantidade resulta em valor com mais de duas casas
- **THEN** o ticket médio é arredondado a duas casas pelo arredondamento bancário

#### Scenario: Cancelamentos no período
- **WHEN** pedidos foram cancelados dentro do período, pagos ou não, e outro foi cancelado fora dele
- **THEN** a quantidade de cancelamentos conta somente os ocorridos no período

#### Scenario: Período sem pedidos
- **WHEN** nenhum pedido corresponde ao período
- **THEN** a resposta é 200 com valor total, ticket médio, total de descontos, quantidade de pedidos e de cancelamentos iguais a zero

### Requirement: Relatório de produtos mais vendidos
GET /api/admin/relatorios/produtos-mais-vendidos SHALL listar, para o período, os produtos das vendas agrupados por SKU, nome e descrição da variação registrados no pedido, com a quantidade vendida e o valor vendido de cada um. A lista MUST ser ordenada por quantidade decrescente e, em empate, por SKU crescente. O parâmetro `limite` MUST aceitar de 1 a 50, com padrão 10, e a lista MUST conter no máximo `limite` itens.

#### Scenario: Ranking por quantidade
- **WHEN** vendas do período contêm um mesmo SKU em pedidos diferentes
- **THEN** o relatório soma as quantidades e os valores desse SKU em uma única linha, ordenada por quantidade decrescente

#### Scenario: Empate de quantidade
- **WHEN** dois SKUs têm a mesma quantidade vendida
- **THEN** o de SKU menor na ordem alfabética aparece primeiro

#### Scenario: Limite
- **WHEN** o relatório é pedido com limite menor que o número de produtos vendidos
- **THEN** a resposta contém somente os primeiros produtos da ordenação; sem o parâmetro, no máximo 10

#### Scenario: Itens de pedidos que não são vendas
- **WHEN** há itens em pedidos cancelados, não pagos ou pagos fora do período
- **THEN** esses itens não entram no ranking

#### Scenario: Limite inválido
- **WHEN** o limite é zero, negativo, maior que 50 ou não numérico
- **THEN** a resposta é 400 e nenhuma consulta é feita

### Requirement: Relatório de campanhas
GET /api/admin/relatorios/campanhas SHALL listar, para o período, o desconto concedido por campanha ou cupom nas vendas, com origem, identificador da campanha ou do cupom, descrição, código do cupom quando houver, quantidade de pedidos distintos e valor concedido. Descontos sem identificador registrado MUST ser agrupados por origem e descrição. A lista MUST ser ordenada por quantidade de pedidos decrescente, valor concedido decrescente e descrição crescente. O relatório SHALL responder tanto às campanhas mais utilizadas quanto ao desconto concedido por campanha.

#### Scenario: Desconto por campanha
- **WHEN** uma campanha concedeu 20,00 em um pedido vendido e 15,00 em outro
- **THEN** o relatório traz uma linha dessa campanha com 2 pedidos e valor concedido de 35,00

#### Scenario: Campanha e cupom no mesmo pedido
- **WHEN** um pedido vendido usou uma promoção e um cupom
- **THEN** cada um aparece em sua própria linha, com o valor que concedeu naquele pedido

#### Scenario: Desconto de frete
- **WHEN** a campanha concedeu desconto sobre o frete de uma venda
- **THEN** o valor entra no total concedido da campanha

#### Scenario: Pedidos que não são vendas
- **WHEN** o desconto pertence a pedido cancelado, não pago ou pago fora do período
- **THEN** o desconto não entra no relatório

#### Scenario: Ordenação
- **WHEN** campanhas têm quantidades de pedidos diferentes ou iguais
- **THEN** a maior quantidade vem primeiro e, em empate, o maior valor concedido, depois a descrição em ordem alfabética

#### Scenario: Descontos sem identificador
- **WHEN** descontos de vendas antigas não registram o identificador da campanha
- **THEN** eles são agrupados por origem e descrição

### Requirement: Validação do período
Os três relatórios SHALL exigir `inicio` e `fim`. Parâmetro ausente, valor que não é instante ISO-8601 com fuso, ou `inicio` posterior a `fim` MUST resultar em 400 sem consultar os dados. Um período em que `inicio` é igual a `fim` MUST ser aceito.

#### Scenario: Parâmetro ausente
- **WHEN** a consulta omite `inicio` ou `fim`
- **THEN** a resposta é 400

#### Scenario: Instante malformado
- **WHEN** `inicio` ou `fim` não é um instante ISO-8601 com fuso
- **THEN** a resposta é 400

#### Scenario: Intervalo invertido
- **WHEN** `inicio` é posterior a `fim`
- **THEN** a resposta é 400

### Requirement: Acesso aos relatórios por perfil
Os relatórios de vendas e de produtos mais vendidos SHALL ser acessíveis somente a ADMIN e OPERADOR_COMERCIAL. O relatório de campanhas SHALL ser acessível a ADMIN, OPERADOR_COMERCIAL e PROMOCOES. Chamada sem autenticação MUST receber 401 e perfil sem permissão MUST receber 403, inclusive quando o serviço é chamado fora da API. Nenhuma rota de relatório MUST permitir criação, edição ou exclusão de dados.

#### Scenario: Operador comercial
- **WHEN** um usuário OPERADOR_COMERCIAL consulta qualquer relatório
- **THEN** recebe 200

#### Scenario: Responsável por promoções
- **WHEN** um usuário PROMOCOES consulta campanhas
- **THEN** recebe 200
- **WHEN** o mesmo usuário consulta vendas ou produtos mais vendidos
- **THEN** recebe 403

#### Scenario: Perfis sem acesso
- **WHEN** um usuário ESTOQUE ou CLIENTE consulta qualquer relatório
- **THEN** recebe 403

#### Scenario: Sem autenticação
- **WHEN** a consulta não traz credenciais
- **THEN** recebe 401

#### Scenario: Somente leitura
- **WHEN** alguém tenta criar, editar ou excluir por uma rota de relatório
- **THEN** nenhum dado é alterado e a operação não é oferecida
