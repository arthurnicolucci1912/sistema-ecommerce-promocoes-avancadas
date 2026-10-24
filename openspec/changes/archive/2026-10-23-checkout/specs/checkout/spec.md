## Purpose

Concluir a seleção do cliente como um pedido com valores confirmados, endereço preservado e reserva de estoque, mantendo a compra inteira consistente mesmo sob falhas ou concorrência.

## ADDED Requirements

### Requirement: Checkout do cliente autenticado
O sistema SHALL disponibilizar prévia e conclusão do checkout exclusivamente ao perfil CLIENTE, usando a identidade autenticada para obter o cliente e seu carrinho. Endereço MUST ser ativo e pertencer a esse cliente. Sem autenticação MUST retornar 401; perfil sem CLIENTE, 403; endereço inexistente, inativo ou de outro cliente, 404. Identificadores de cliente enviados pelo chamador MUST NOT escolher o proprietário da compra.

#### Scenario: Endereço de outro cliente
- **WHEN** o cliente A tenta consultar prévia ou concluir usando endereço do cliente B
- **THEN** recebe 404 sem criar pedido, reservar estoque ou alterar qualquer carrinho

#### Scenario: Acesso indevido
- **WHEN** visitante ou usuário exclusivamente ADMIN tenta prévia ou conclusão
- **THEN** recebe respectivamente 401 ou 403 sem efeito de compra

### Requirement: Prévia atual da compra
O sistema SHALL fornecer prévia com itens, quantidades, preços unitários, subtotais, descontos, frete, total e endereço copiado, consultando dados correntes. A prévia MUST identificar a versão do carrinho e as insuficiências de estoque; MUST NOT criar pedido, reservar estoque ou modificar carrinho. Carrinho vazio MUST retornar 409. Campos obrigatórios ausentes ou inválidos MUST retornar 400.

#### Scenario: Prévia sem compromisso de estoque
- **WHEN** o cliente consulta prévia de 2 unidades de R$ 10,00 e apenas 1 está disponível
- **THEN** recebe detalhamento de R$ 20,00 e aviso de insuficiência, sem alterar seleção ou saldo

### Requirement: Revalidação e nova confirmação
A conclusão SHALL exigir o detalhamento da prévia confirmado pelo cliente e recalcular os valores com dados correntes, sem usar valores enviados para formar o pedido. Mudança na versão da seleção, itens, quantidades, preços, descontos, frete, totais ou endereço MUST retornar 409 com divergências e prévia atualizada, sem pedido nem reserva. Itens ausentes ou indisponíveis para venda MUST bloquear a conclusão com 409 identificando os SKUs. Nesta etapa descontos são vazios e frete é zero.

#### Scenario: Mudança de preço
- **WHEN** o cliente confirma 2 unidades a R$ 10,00 e o preço corrente passou a R$ 12,50
- **THEN** recebe 409 com a divergência e o novo total de R$ 25,00, preservando carrinho e estoque

#### Scenario: Alterações compensadas no total
- **WHEN** um preço aumenta e outro diminui mantendo o total que o cliente confirmou
- **THEN** a alteração dos preços por linha ainda exige nova confirmação

#### Scenario: Confirmar novamente
- **WHEN** o cliente envia o detalhamento atualizado e nenhuma informação relevante mudou desde então
- **THEN** o checkout pode concluir pelos valores correntes, sujeito à reserva de estoque

#### Scenario: Endereço alterado após a prévia
- **WHEN** o endereço escolhido muda depois da prévia
- **THEN** a conclusão devolve 409 com o endereço atualizado para confirmação

### Requirement: Pedido e reserva atômicos
A conclusão SHALL criar um pedido próprio em AGUARDANDO_PAGAMENTO com cópia comercial dos itens, descrição da variação quando aplicável, valores e endereço, reservar todos os componentes e esvaziar o carrinho como uma única operação. Falha em qualquer parte MUST desfazer pedido, histórico, reservas, alterações de saldo e limpeza do carrinho. Sucesso MUST retornar 201 com o pedido e sua localização para consulta.

#### Scenario: Compra concluída
- **WHEN** o cliente confirma 2 unidades de R$ 10,00 com endereço válido e saldo 5
- **THEN** recebe pedido AGUARDANDO_PAGAMENTO de R$ 20,00, saldo fica 3 disponível e 2 reservado, e o carrinho fica vazio

#### Scenario: Falha posterior à reserva
- **WHEN** a confirmação do pedido ou a limpeza persistida do carrinho falha após reservar
- **THEN** nenhum pedido da tentativa permanece, todos os saldos são restaurados e o carrinho conserva a seleção

#### Scenario: Estoque parcialmente insuficiente
- **WHEN** uma compra exige 1 caneca com saldo 10 e 4 cafés com saldo 3
- **THEN** recebe 409 identificando café e saldo 3, sem pedido persistido nem reserva de caneca ou café

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
