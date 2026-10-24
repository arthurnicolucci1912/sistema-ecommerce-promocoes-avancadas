# promocoes Specification

## Purpose
Aplicar campanhas comerciais configuráveis com regras combináveis, resolução determinística de conflitos e detalhamento dos benefícios, permitindo acrescentar novos tipos sem reescrever os existentes.

## Requirements

### Requirement: Administração de campanhas
ADMIN e PROMOCOES SHALL cadastrar, consultar, listar, alterar, ativar e desativar campanhas com nome, tipo, parâmetros, início, fim, prioridade, modo de acumulação e condições. Tipo desconhecido, parâmetros inválidos ou período invertido MUST retornar 400 sem gravação parcial; recurso inexistente MUST retornar 404 e edição concorrente MUST retornar 409. Sem autenticação MUST retornar 401 e outros perfis MUST receber 403. A vigência SHALL incluir o início e excluir o fim, usando instantes UTC. Desativar ou alterar uma campanha MUST NOT mudar snapshots de pedidos existentes.

#### Scenario: Campanha vigente
- **WHEN** uma campanha ativa tem início às 10h e fim às 11h
- **THEN** pode beneficiar cálculos a partir das 10h e deixa de beneficiar cálculos às 11h, sem exclusão manual

#### Scenario: Parâmetro inválido
- **WHEN** o responsável tenta cadastrar percentual acima de 100 ou leve 2/pague 3
- **THEN** recebe 400 e nenhuma configuração da tentativa é persistida

#### Scenario: Perfil de estoque
- **WHEN** um usuário ESTOQUE tenta criar ou consultar campanhas administrativas
- **THEN** recebe 403

### Requirement: Benefícios de naturezas distintas
O sistema SHALL oferecer leve X/pague Y por SKU, percentual por categoria incluindo descendentes, valor fixo com subtotal mínimo, percentual na primeira compra e frete grátis. Leve X/pague Y MUST descontar `(quantidade / X arredondada para baixo) * (X - Y)` unidades do SKU. Percentuais de promoção MUST incidir sobre o valor original elegível e arredondar cada desconto para duas casas com HALF_EVEN. Kits MUST usar seu preço e SKU comerciais, sem aplicar novamente descontos a componentes físicos. Benefícios de frete MUST abater somente o frete, limitados à cotação bruta; até a integração de entrega a cotação de produção MUST ser zero.

#### Scenario: Leve três pague dois
- **WHEN** o cliente seleciona 7 unidades do SKU elegível a R$ 10,00 sob leve 3/pague 2
- **THEN** recebe R$ 20,00 de desconto dessa campanha

#### Scenario: Percentual na categoria ancestral
- **WHEN** há 20% em Livros e o carrinho contém R$ 100,00 de uma subcategoria de Livros e R$ 200,00 de outra categoria
- **THEN** o desconto dessa campanha é R$ 20,00

#### Scenario: Frete grátis com cotação de entrada
- **WHEN** o cálculo recebe uma cotação bruta simulada de R$ 25,00 e uma campanha elegível de frete grátis
- **THEN** o desconto tem alvo frete e valor R$ 25,00, sem reduzir o subtotal dos itens

### Requirement: Condições combináveis
Campanhas SHALL aceitar condições de primeira compra, subtotal mínimo, categoria, SKU, região por conjunto de UFs, período e perfil autenticado, combinadas com E, OU e NÃO. E e OU MUST ter ao menos dois filhos e NÃO exatamente um; árvores inválidas ou acima de 16 níveis ou 128 nós MUST retornar 400. Ausência de endereço MUST tornar falsas as condições de região; atributos do cliente e perfis MUST vir de fontes autenticadas do servidor. Sem condição informada SHALL equivaler a elegibilidade irrestrita, sujeita à vigência e ativação da campanha.

#### Scenario: Combinação comercial
- **WHEN** a condição é valor mínimo de R$ 300,00 E (UF SP OU UF RJ) E NÃO primeira compra
- **THEN** apenas clientes que atendam à expressão inteira recebem o benefício

#### Scenario: Região ainda desconhecida
- **WHEN** o carrinho não tem endereço principal ativo e existe campanha exclusiva para SP
- **THEN** a campanha não é aplicada, e o checkout a reavalia com o endereço escolhido

### Requirement: Primeira compra por pagamento aprovado
Um cliente SHALL ser considerado em primeira compra se nenhum pedido seu tiver registrado pagamento aprovado até o instante da avaliação. Pedidos recusados ou cancelados antes de pagar MUST NOT retirar a elegibilidade. Um pagamento aprovado anterior MUST retirar a elegibilidade mesmo que o pedido depois tenha sido cancelado. A condição SHALL ser reavaliada no checkout, sem confiar em sinalização recebida do cliente.

#### Scenario: Tentativa anterior não paga
- **WHEN** o cliente tem pedidos recusados ou cancelados sem aprovação de pagamento
- **THEN** continua elegível para a promoção de primeira compra

#### Scenario: Compra paga e depois cancelada
- **WHEN** há no histórico de qualquer pedido do cliente uma aprovação de pagamento anterior
- **THEN** ele não recebe a promoção de primeira compra, mesmo que esse pedido esteja cancelado

#### Scenario: Primeira compra deixa de valer entre prévia e conclusão
- **WHEN** após a prévia outro pedido do cliente tem pagamento aprovado e o desconto confirmado dependia de primeira compra
- **THEN** o checkout recalcula e exige nova confirmação do detalhamento sem esse desconto

### Requirement: Política determinística de conflitos
Se houver campanhas exclusivas elegíveis, o sistema SHALL aplicar somente a exclusiva de maior desconto. Sem exclusivas SHALL aplicar todas as acumuláveis por prioridade decrescente e a não acumulável de maior desconto. Empate no desconto MUST ser resolvido por maior prioridade, depois por criação mais antiga e finalmente por menor identificador. Promoção exclusiva aplicada MUST impedir cupom. O desconto efetivo total de itens MUST ser limitado ao subtotal, reduzindo primeiro os benefícios de menor prioridade; descontos de frete têm teto separado. Valor zero MUST NOT aparecer como desconto concedido.

#### Scenario: Acumuláveis e melhor não acumulável
- **WHEN** subtotal R$ 400,00 tem descontos acumuláveis de R$ 20,00 e R$ 50,00 e não acumuláveis de R$ 40,00 e R$ 60,00
- **THEN** o desconto de promoções é R$ 130,00 e a campanha de R$ 40,00 não aparece como aplicada

#### Scenario: Exclusiva vence a soma
- **WHEN** uma exclusiva concede R$ 100,00 e outras campanhas somariam R$ 130,00
- **THEN** somente R$ 100,00 da exclusiva é aplicado e nenhum cupom é aceito

#### Scenario: Teto dos itens
- **WHEN** subtotal R$ 60,00 recebe R$ 50,00 com prioridade 20 e R$ 20,00 com prioridade 10
- **THEN** os valores efetivos são R$ 50,00 e R$ 10,00 e o subtotal líquido é zero

### Requirement: Extensibilidade observável
Um novo tipo de promoção registrado SHALL participar da criação por configuração e do cálculo sem alterar os tipos existentes nem o motor. Uma nova condição registrada SHALL funcionar dentro das composições existentes sem alterá-las. A política de conflito SHALL poder ser substituída sem alterar os benefícios ou os consumidores do cálculo.

#### Scenario: Sexto tipo registrado
- **WHEN** um sexto tipo de promoção é disponibilizado com seu criador e parâmetros válidos
- **THEN** uma campanha desse tipo pode ser cadastrada e aplicada sem alterações nas cinco promoções existentes

#### Scenario: Nova folha e política alternativa
- **WHEN** uma nova condição é registrada e a política é trocada por outra que concede apenas o maior desconto
- **THEN** as composições avaliam a condição e o cálculo aplica a política alternativa sem reescrever os benefícios
