# carrinho Specification

## Purpose

Permitir que cada cliente mantenha sua seleção de itens para compra, com quantidades, preços atuais e disponibilidade informativa, preparando a entrada do checkout.

## Requirements

### Requirement: Carrinho próprio e persistente
O cliente autenticado SHALL consultar e modificar apenas seu próprio carrinho. O carrinho MUST persistir entre sessões e uma consulta sem itens MUST retornar 200 com lista vazia e valores zerados. Requisições sem autenticação MUST receber 401 e perfis sem CLIENTE MUST receber 403.

#### Scenario: Retomar seleção em outra sessão
- **WHEN** o cliente autentica novamente após incluir itens
- **THEN** a consulta retorna os mesmos SKUs e quantidades

#### Scenario: Isolamento entre clientes
- **WHEN** dois clientes consultam ou modificam seus carrinhos
- **THEN** cada operação usa a identidade autenticada e não acessa nem modifica o carrinho do outro

#### Scenario: Visitante e perfil administrativo
- **WHEN** um visitante ou um usuário exclusivamente ADMIN tenta modificar um carrinho
- **THEN** recebe respectivamente 401 ou 403 sem alteração de dados

### Requirement: Manutenção de itens
O sistema SHALL incluir itens por SKU, somar quantidades em inclusão repetida, substituir a quantidade de uma linha e remover linhas. Quantidades MUST ser inteiras positivas, sem estouro numérico; dados inválidos MUST retornar 400 sem alteração parcial. Remover linha ausente MUST retornar o carrinho atual sem erro.

#### Scenario: Inclusão repetida e alteração
- **WHEN** o cliente inclui 2 unidades de um SKU, inclui mais 1 e depois altera sua quantidade para 4
- **THEN** há uma única linha com quantidade 4 e o resumo é recalculado a cada operação

#### Scenario: Quantidade inválida
- **WHEN** o cliente informa quantidade zero, negativa, fracionária ou que cause estouro ao somar
- **THEN** recebe 400 e o carrinho persistido permanece intacto

#### Scenario: Remover último item
- **WHEN** o cliente remove a última linha
- **THEN** recebe o carrinho vazio com subtotal e total zerados

### Requirement: Itens de catálogo e combos
O sistema SHALL aceitar variações e combos pelo SKU e preço próprios, sem cobrar novamente seus componentes. Inclusão de SKU inexistente MUST retornar 404 e inclusão ou aumento de item indisponível para venda MUST retornar 409. Itens que se tornem inativos após inclusão MUST permanecer visíveis com aviso e poder ser removidos ou reduzidos.

#### Scenario: Combo com preço próprio
- **WHEN** o cliente inclui 2 kits de preço R$ 79,90 cada
- **THEN** a linha custa R$ 159,80 e os componentes são usados apenas para informar disponibilidade

#### Scenario: Item inativado depois da inclusão
- **WHEN** o catálogo inativa um item já presente no carrinho e o cliente consulta o carrinho
- **THEN** a linha permanece visível, sinalizada como indisponível para compra

### Requirement: Disponibilidade sem reserva
O carrinho SHALL consultar disponibilidade considerando a demanda agregada dos componentes de todas as linhas e informar insuficiência por componente. Falta de estoque MUST gerar aviso sem impedir manutenção do carrinho. Nenhuma operação de carrinho MUST reservar, baixar ou liberar estoque.

#### Scenario: Componentes compartilhados
- **WHEN** um kit demanda 2 cafés, outra linha demanda 2 cafés e existem 3 disponíveis
- **THEN** a resposta informa demanda 4 e disponibilidade 3, sem alterar o estoque

#### Scenario: Estoque acaba depois da inclusão
- **WHEN** outro pedido consome o estoque de um item do carrinho
- **THEN** a próxima consulta sinaliza insuficiência e preserva as quantidades selecionadas

### Requirement: Preços correntes e resumo
Toda consulta e modificação SHALL recalcular valores com os preços atuais do catálogo. A resposta MUST expor SKU, nome, quantidade, preço unitário, subtotal por linha, subtotal geral, descontos, frete e total. A resposta SHALL discriminar cada desconto efetivo com origem (promoção ou cupom), identificador, nome, alvo e valor. Campanhas e cupom selecionado SHALL ser reavaliados a cada cálculo, com contexto autenticado e endereço principal ativo para região quando disponível. Frete de produção MUST continuar zero até a integração de entrega; sem benefícios elegíveis o total MUST ser igual ao subtotal.

#### Scenario: Preço atualizado
- **WHEN** uma linha de quantidade 2 muda de R$ 10,00 para R$ 12,50 no catálogo
- **THEN** a próxima consulta mostra preço unitário R$ 12,50 e subtotal R$ 25,00

#### Scenario: Descontos no carrinho
- **WHEN** subtotal R$ 400,00 recebe R$ 110,00 de promoções e cupom válido de 10%
- **THEN** o resumo discrimina os descontos de promoção e R$ 29,00 de cupom, com total R$ 261,00 sem frete

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
