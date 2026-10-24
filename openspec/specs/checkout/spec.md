# checkout Specification

## Purpose

Concluir a seleção do cliente como um pedido com valores confirmados, endereço preservado e reserva de estoque, mantendo a compra inteira consistente mesmo sob falhas ou concorrência.

## Requirements

### Requirement: Checkout do cliente autenticado
O sistema SHALL disponibilizar prévia e conclusão do checkout exclusivamente ao perfil CLIENTE, usando a identidade autenticada para obter o cliente e seu carrinho. Endereço MUST ser ativo e pertencer a esse cliente. Sem autenticação MUST retornar 401; perfil sem CLIENTE, 403; endereço inexistente, inativo ou de outro cliente, 404. Identificadores de cliente enviados pelo chamador MUST NOT escolher o proprietário da compra.

#### Scenario: Endereço de outro cliente
- **WHEN** o cliente A tenta consultar prévia ou concluir usando endereço do cliente B
- **THEN** recebe 404 sem criar pedido, reservar estoque ou alterar qualquer carrinho

#### Scenario: Acesso indevido
- **WHEN** visitante ou usuário exclusivamente ADMIN tenta prévia ou conclusão
- **THEN** recebe respectivamente 401 ou 403 sem efeito de compra

### Requirement: Prévia atual da compra
O sistema SHALL fornecer prévia com itens, quantidades, preços unitários, subtotais, descontos, frete, total e endereço copiado, consultando dados correntes. A prévia MUST identificar a versão do carrinho e as insuficiências de estoque; MUST NOT criar pedido, reservar estoque ou modificar carrinho. Carrinho vazio MUST retornar 409. Campos obrigatórios ausentes ou inválidos MUST retornar 400. A prévia SHALL exigir seleção de entrega e informar fornecedor, modalidade, prazo, custo bruto, descontos de frete e custo líquido. O detalhamento SHALL incluir código de cupom selecionado, estado de validade e descontos com origem e identidade, avaliados com endereço de entrega escolhido e histórico real do cliente. Um cupom selecionado que ficou inválido SHALL ser sinalizado na prévia sem conceder desconto, permitindo remoção ou substituição antes de concluir.

#### Scenario: Prévia sem compromisso de estoque
- **WHEN** o cliente consulta prévia de 2 unidades de R$ 10,00, sem benefícios elegíveis, e apenas 1 está disponível
- **THEN** recebe subtotal de R$ 20,00, frete e total discriminados e aviso de insuficiência, sem alterar seleção ou saldo

### Requirement: Revalidação e nova confirmação
A conclusão SHALL exigir o detalhamento da prévia confirmado pelo cliente e recalcular os valores com dados correntes, sem usar valores enviados para formar o pedido. Mudança na versão da seleção, itens, quantidades, preços, código/validade do cupom, origem/identidade/valor dos descontos, frete, totais ou endereço MUST retornar 409 com divergências e prévia atualizada, sem pedido nem reserva. Itens ausentes ou indisponíveis para venda MUST bloquear a conclusão com 409 identificando os SKUs. Promoções e cupons SHALL usar suas configurações vigentes; cupom selecionado inválido MUST impedir a conclusão com motivo estruturado e prévia atualizada. A opção de entrega SHALL ser obrigatória. Fornecedor, modalidade, prazo e custo bruto SHALL ser recotados no servidor, usando endereço e carrinho correntes. Alteração de opção, fornecedor, prazo, custo ou frete líquido MUST exigir nova confirmação mesmo se o total final não mudar. Opção indisponível MUST retornar 409 com opções atuais, sem escolher outra automaticamente.

#### Scenario: Mudança de preço
- **WHEN** o cliente confirma 2 unidades a R$ 10,00, sem benefícios elegíveis, e o preço corrente passou a R$ 12,50
- **THEN** recebe 409 com a divergência e subtotal atualizado de R$ 25,00 e frete/total discriminados, preservando carrinho e estoque

#### Scenario: Alterações compensadas no total
- **WHEN** um preço aumenta e outro diminui mantendo o total que o cliente confirmou
- **THEN** a alteração dos preços por linha ainda exige nova confirmação

#### Scenario: Confirmar novamente
- **WHEN** o cliente envia o detalhamento atualizado e nenhuma informação relevante mudou desde então
- **THEN** o checkout pode concluir pelos valores correntes, sujeito à reserva de estoque

#### Scenario: Endereço alterado após a prévia
- **WHEN** o endereço escolhido muda depois da prévia
- **THEN** a conclusão devolve 409 com o endereço atualizado para confirmação

#### Scenario: Campanha termina após a prévia
- **WHEN** a promoção que concedia R$ 20,00 expira antes da conclusão
- **THEN** o checkout retorna 409 com divergências e prévia sem esse desconto, sem pedido, reserva ou uso de cupom

#### Scenario: Mesmos valores com outra origem
- **WHEN** a seleção muda de um cupom para outro que concede o mesmo valor
- **THEN** código, identidade do benefício e versão alterados ainda exigem nova confirmação

#### Scenario: Cupom selecionado inválido
- **WHEN** o cliente confirma uma prévia que aponta cupom expirado sem removê-lo ou substituí-lo
- **THEN** recebe 409 com motivo de validade e nenhuma utilização é consumida

#### Scenario: Custo alterado mesmo com frete grátis
- **WHEN** uma cotação muda de R$ 25,00 para R$ 30,00 e uma promoção continua zerando o frete líquido
- **THEN** a mudança no custo bruto e no desconto exige reconfirmação, mesmo mantendo o total final

#### Scenario: Prazo alterado com mesmo preço
- **WHEN** o prazo da entrega escolhida muda de 3 para 5 dias úteis antes da conclusão
- **THEN** o cliente recebe 409 com a prévia atualizada e nenhum pedido ou consumo parcial é criado

### Requirement: Pedido e reserva atômicos
A conclusão SHALL criar um pedido próprio em AGUARDANDO_PAGAMENTO com cópia comercial dos itens, descrição da variação quando aplicável, valores, endereço e opção de entrega com fornecedor/modalidade/prazo, reservar todos os componentes, consumir o uso de cupom com benefício efetivo e esvaziar o carrinho como uma única operação. O snapshot SHALL preservar descontos por origem, alvo e valor, sem mudar quando a configuração administrativa mudar. Falha em qualquer parte MUST desfazer pedido, histórico, reservas, alterações de saldo, consumo do cupom e limpeza do carrinho. Sucesso MUST retornar 201 com o pedido e sua localização para consulta.

#### Scenario: Compra concluída
- **WHEN** o cliente confirma 2 unidades de R$ 10,00, sem benefícios elegíveis, com endereço válido e saldo 5
- **THEN** recebe pedido AGUARDANDO_PAGAMENTO com subtotal R$ 20,00 mais frete líquido confirmado, saldo fica 3 disponível e 2 reservado, e o carrinho fica vazio

#### Scenario: Falha posterior à reserva
- **WHEN** a confirmação do pedido ou a limpeza persistida do carrinho falha após reservar
- **THEN** nenhum pedido da tentativa permanece, todos os saldos são restaurados e o carrinho conserva a seleção

#### Scenario: Estoque parcialmente insuficiente
- **WHEN** uma compra exige 1 caneca com saldo 10 e 4 cafés com saldo 3
- **THEN** recebe 409 identificando café e saldo 3, sem pedido persistido nem reserva de caneca ou café

#### Scenario: Compra com cupom
- **WHEN** o cliente confirma um cupom com desconto efetivo e a compra é concluída
- **THEN** um uso é vinculado ao pedido, o snapshot identifica a origem CUPOM e o carrinho fica sem itens nem cupom

#### Scenario: Cupom esgotado na disputa
- **WHEN** o último uso é consumido por outra compra durante a tentativa
- **THEN** a tentativa perdedora recebe 409 sem pedido, reserva ou consumo parcial e preserva sua seleção

### Requirement: Itens compostos e demanda agregada
O checkout SHALL cobrar o preço comercial de cada item e reservar a demanda física agregada de todas as linhas, inclusive kits aninhados e componentes compartilhados. Novo tipo de item vendável que forneça preço, disponibilidade e componentes pelo contrato existente MUST poder ser comprado sem alterar o fluxo de checkout. Quantidades cujo produto ou soma exceda a capacidade de reserva MUST retornar 400 sem efeitos parciais.

#### Scenario: Kit e componente avulso
- **WHEN** um kit de R$ 35,00 usa 2 cafés e 1 caneca e a seleção inclui mais 1 café de R$ 10,00
- **THEN** o pedido totaliza R$ 45,00 e reserva 3 cafés e 1 caneca

#### Scenario: Novo tipo vendável
- **WHEN** um novo tipo de item oferece seu preço e sua composição pelo contrato comum
- **THEN** a compra usa esse preço e reserva essa composição sem tratamento especial no checkout

### Requirement: Concorrência e consumo único da seleção
Conclusões concorrentes MUST NOT consumir mais do que o estoque disponível. A mesma versão do carrinho MUST NOT gerar dois pedidos confirmados. Alteração concorrente da seleção MUST NOT ser perdida nem incluída silenciosamente na compra; conflito MUST retornar 409 com rollback da tentativa perdedora.

#### Scenario: Última unidade entre dois clientes
- **WHEN** dois clientes concluem simultaneamente compras da última unidade disponível
- **THEN** apenas um pedido é confirmado e somente seu carrinho é esvaziado; o outro recebe 409 e mantém a seleção

#### Scenario: Mesmo carrinho confirmado duas vezes
- **WHEN** duas requisições tentam concluir a mesma versão do carrinho
- **THEN** no máximo uma cria pedido e reserva estoque, e a outra retorna 409

#### Scenario: Inclusão concorrente
- **WHEN** uma inclusão no carrinho e uma conclusão disputam a mesma versão
- **THEN** as duas escritas não são aplicadas silenciosamente sobre a mesma versão; a perdedora retorna 409 e a seleção resultante corresponde à operação vencedora

### Requirement: Reserva acompanha o pedido resultante
Reservas de pedidos criados no checkout SHALL ser liberadas quando cancelados em AGUARDANDO_PAGAMENTO ou quando o pagamento simulado for recusado, uma única vez. Aprovação simulada SHALL baixar a reserva. Essas operações MUST preservar os snapshots comerciais e manter pedido e estoque consistentes.

#### Scenario: Cancelamento pelo comprador
- **WHEN** o comprador cancela seu pedido aguardando pagamento com 2 unidades reservadas
- **THEN** as 2 unidades voltam ao disponível e a reserva fica liberada

#### Scenario: Recusa e repetição
- **WHEN** o pagamento simulado de um pedido do checkout é recusado e a liberação é processada novamente
- **THEN** o pedido fica PAGAMENTO_RECUSADO e as unidades voltam ao disponível uma única vez

#### Scenario: Aprovação simulada
- **WHEN** o pagamento simulado do pedido é aprovado
- **THEN** o pedido fica PAGO e a reserva é baixada sem nova redução do disponível
