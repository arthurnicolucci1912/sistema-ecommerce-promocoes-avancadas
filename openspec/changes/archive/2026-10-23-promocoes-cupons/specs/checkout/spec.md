## MODIFIED Requirements

### Requirement: Prévia atual da compra
O sistema SHALL fornecer prévia com itens, quantidades, preços unitários, subtotais, descontos, frete, total e endereço copiado, consultando dados correntes. A prévia MUST identificar a versão do carrinho e as insuficiências de estoque; MUST NOT criar pedido, reservar estoque ou modificar carrinho. Carrinho vazio MUST retornar 409. Campos obrigatórios ausentes ou inválidos MUST retornar 400. O detalhamento SHALL incluir código de cupom selecionado, estado de validade e descontos com origem e identidade, avaliados com endereço de entrega escolhido e histórico real do cliente. Um cupom selecionado que ficou inválido SHALL ser sinalizado na prévia sem conceder desconto, permitindo remoção ou substituição antes de concluir.

#### Scenario: Prévia sem compromisso de estoque
- **WHEN** o cliente consulta prévia de 2 unidades de R$ 10,00, sem benefícios elegíveis, e apenas 1 está disponível
- **THEN** recebe detalhamento de R$ 20,00 e aviso de insuficiência, sem alterar seleção ou saldo

### Requirement: Revalidação e nova confirmação
A conclusão SHALL exigir o detalhamento da prévia confirmado pelo cliente e recalcular os valores com dados correntes, sem usar valores enviados para formar o pedido. Mudança na versão da seleção, itens, quantidades, preços, código/validade do cupom, origem/identidade/valor dos descontos, frete, totais ou endereço MUST retornar 409 com divergências e prévia atualizada, sem pedido nem reserva. Itens ausentes ou indisponíveis para venda MUST bloquear a conclusão com 409 identificando os SKUs. Promoções e cupons SHALL usar suas configurações vigentes; cupom selecionado inválido MUST impedir a conclusão com motivo estruturado e prévia atualizada. Frete de produção permanece zero até a integração de entrega.

#### Scenario: Mudança de preço
- **WHEN** o cliente confirma 2 unidades a R$ 10,00, sem benefícios elegíveis, e o preço corrente passou a R$ 12,50
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

#### Scenario: Campanha termina após a prévia
- **WHEN** a promoção que concedia R$ 20,00 expira antes da conclusão
- **THEN** o checkout retorna 409 com divergências e prévia sem esse desconto, sem pedido, reserva ou uso de cupom

#### Scenario: Mesmos valores com outra origem
- **WHEN** a seleção muda de um cupom para outro que concede o mesmo valor
- **THEN** código, identidade do benefício e versão alterados ainda exigem nova confirmação

#### Scenario: Cupom selecionado inválido
- **WHEN** o cliente confirma uma prévia que aponta cupom expirado sem removê-lo ou substituí-lo
- **THEN** recebe 409 com motivo de validade e nenhuma utilização é consumida

### Requirement: Pedido e reserva atômicos
A conclusão SHALL criar um pedido próprio em AGUARDANDO_PAGAMENTO com cópia comercial dos itens, descrição da variação quando aplicável, valores e endereço, reservar todos os componentes, consumir o uso de cupom com benefício efetivo e esvaziar o carrinho como uma única operação. O snapshot SHALL preservar descontos por origem, alvo e valor, sem mudar quando a configuração administrativa mudar. Falha em qualquer parte MUST desfazer pedido, histórico, reservas, alterações de saldo, consumo do cupom e limpeza do carrinho. Sucesso MUST retornar 201 com o pedido e sua localização para consulta.

#### Scenario: Compra concluída
- **WHEN** o cliente confirma 2 unidades de R$ 10,00, sem benefícios elegíveis, com endereço válido e saldo 5
- **THEN** recebe pedido AGUARDANDO_PAGAMENTO de R$ 20,00, saldo fica 3 disponível e 2 reservado, e o carrinho fica vazio

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
