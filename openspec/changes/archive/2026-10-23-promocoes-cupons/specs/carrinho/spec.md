## MODIFIED Requirements

### Requirement: Preços correntes e resumo
Toda consulta e modificação SHALL recalcular valores com os preços atuais do catálogo. A resposta MUST expor SKU, nome, quantidade, preço unitário, subtotal por linha, subtotal geral, descontos, frete e total. A resposta SHALL discriminar cada desconto efetivo com origem (promoção ou cupom), identificador, nome, alvo e valor. Campanhas e cupom selecionado SHALL ser reavaliados a cada cálculo, com contexto autenticado e endereço principal ativo para região quando disponível. Frete de produção MUST continuar zero até a integração de entrega; sem benefícios elegíveis o total MUST ser igual ao subtotal.

#### Scenario: Preço atualizado
- **WHEN** uma linha de quantidade 2 muda de R$ 10,00 para R$ 12,50 no catálogo
- **THEN** a próxima consulta mostra preço unitário R$ 12,50 e subtotal R$ 25,00

#### Scenario: Descontos no carrinho
- **WHEN** subtotal R$ 400,00 recebe R$ 110,00 de promoções e cupom válido de 10%
- **THEN** o resumo discrimina os descontos de promoção e R$ 29,00 de cupom, com total R$ 261,00 sem frete

## ADDED Requirements

### Requirement: Cupom próprio persistido e recuperável
O cliente SHALL aplicar, substituir e remover no máximo um cupom no próprio carrinho, sem consumir uso nem reservar estoque. Aplicar novo cupom inválido MUST preservar o cupom anterior. Remover cupom ausente MUST retornar o resumo atual sem erro. Código selecionado MUST persistir entre sessões e participar da versão do carrinho. Se ficar inválido após seleção, leituras e manutenção dos itens SHALL continuar disponíveis com motivo no resumo, sem conceder o benefício inválido; a conclusão da compra MUST ser bloqueada até remoção ou substituição por cupom válido. Esvaziar o carrinho após compra MUST remover também o cupom selecionado.

#### Scenario: Retomar cupom
- **WHEN** o cliente autentica novamente após selecionar um cupom válido
- **THEN** encontra o mesmo código com benefício reavaliado e nenhum uso ainda consumido

#### Scenario: Cupom expira durante a seleção
- **WHEN** o cupom do carrinho expira e o cliente consulta ou reduz a quantidade de um item
- **THEN** o carrinho permanece utilizável, informa o motivo de validade e totaliza sem o desconto do cupom

#### Scenario: Remover benefício inválido
- **WHEN** o cliente remove o cupom expirado
- **THEN** recebe resumo recalculado sem cupom e pode obter nova confirmação de checkout
