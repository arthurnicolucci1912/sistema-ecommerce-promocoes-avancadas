## MODIFIED Requirements

### Requirement: Cálculo monetário inicial
O cálculo SHALL somar preço unitário vezes quantidade de cada linha e produzir subtotal, descontos, frete e total. Valores MUST usar duas casas decimais e arredondamento HALF_EVEN, conforme RN-14. Sem descontos e frete, o total MUST ser igual ao subtotal. Quando houver benefícios, o cálculo SHALL executar subtotal, promoções, cupom, composição do frete e total, preservando a origem e identidade de cada desconto. Promoções percentuais usam a base bruta elegível; cupom percentual usa o saldo de itens após promoções. Descontos efetivos MUST respeitar tetos separados de itens e frete, sem valores negativos nem dupla contagem.

#### Scenario: Múltiplas linhas
- **WHEN** o cálculo recebe 2 unidades a R$ 59,90 e 1 kit a R$ 79,90, sem benefícios elegíveis nem frete
- **THEN** o subtotal e o total são R$ 199,70, os descontos estão vazios e o frete é R$ 0,00

#### Scenario: Cálculo vazio
- **WHEN** não há linhas
- **THEN** subtotal, frete e total são zero e não há descontos

#### Scenario: Ordem de promoção e cupom
- **WHEN** subtotal R$ 400,00 recebe R$ 110,00 de promoções, cupom de 10% e frete bruto simulado R$ 25,00
- **THEN** o desconto do cupom é R$ 29,00 e o total é R$ 286,00

#### Scenario: Frete já descontado
- **WHEN** o cálculo recebe frete bruto simulado R$ 25,00, promoção de frete grátis e cupom acumulável que também reduz frete
- **THEN** o abatimento efetivo do frete não ultrapassa R$ 25,00 e não reduz o valor líquido dos itens
