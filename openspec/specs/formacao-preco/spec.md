# formacao-preco Specification

## Purpose

Produzir um resumo monetário consistente da compra por meio de etapas ordenadas, permitindo acrescentar regras futuras sem modificar as regras já existentes.

## Requirements

### Requirement: Cálculo monetário inicial
O cálculo SHALL somar preço unitário vezes quantidade de cada linha e produzir subtotal, descontos, frete e total. Valores MUST usar duas casas decimais e arredondamento HALF_EVEN, conforme RN-14. Sem descontos e frete, o total MUST ser igual ao subtotal. Quando houver benefícios, o cálculo SHALL executar subtotal, promoções, cupom, composição do frete e total, preservando a origem e identidade de cada desconto. Promoções percentuais usam a base bruta elegível; cupom percentual usa o saldo de itens após promoções. Descontos efetivos MUST respeitar tetos separados de itens e frete, sem valores negativos nem dupla contagem. Quando houver entrega selecionada, o frete bruto SHALL vir da cotação corrente do servidor e a resposta SHALL discriminar frete líquido como bruto menos descontos efetivos sobre frete. O total SHALL ser subtotal menos todos os descontos mais frete bruto. Valores enviados pelo cliente MUST NOT definir a cotação.

#### Scenario: Múltiplas linhas
- **WHEN** o cálculo recebe 2 unidades a R$ 59,90 e 1 kit a R$ 79,90, sem benefícios elegíveis nem frete
- **THEN** o subtotal e o total são R$ 199,70, os descontos estão vazios e o frete é R$ 0,00

#### Scenario: Cálculo vazio
- **WHEN** não há linhas
- **THEN** subtotal, frete e total são zero e não há descontos

#### Scenario: Ordem de promoção e cupom
- **WHEN** subtotal R$ 400,00 recebe R$ 110,00 de promoções, cupom de 10% e frete bruto cotado R$ 25,00
- **THEN** o desconto do cupom é R$ 29,00 e o total é R$ 286,00

#### Scenario: Frete já descontado
- **WHEN** o cálculo recebe frete bruto cotado R$ 25,00, promoção de frete grátis e cupom acumulável que também reduz frete
- **THEN** o abatimento efetivo do frete não ultrapassa R$ 25,00 e não reduz o valor líquido dos itens

#### Scenario: Frete grátis integrado
- **WHEN** itens somam R$ 400,00, promoções de itens somam R$ 110,00, cupom concede R$ 29,00 e cotação de R$ 25,00 recebe frete grátis elegível
- **THEN** o total é R$ 261,00, o frete bruto é R$ 25,00, o desconto de frete é R$ 25,00 e o frete líquido é zero

#### Scenario: Trocar modalidade recalcula preço
- **WHEN** a entrega escolhida muda de econômica de R$ 25,00 para expressa de R$ 40,00
- **THEN** toda a composição de benefícios e frete é recalculada, respeitando tetos e conflitos por alvo

### Requirement: Extensão por novas etapas ordenadas
O cálculo SHALL executar etapas na ordem configurada. Registrar uma nova etapa MUST permitir modificar o resultado sem alterar as etapas existentes. Cálculos sucessivos MUST NOT compartilhar estado mutável de resultado.

#### Scenario: Acrescentar uma etapa
- **WHEN** é registrada uma etapa de teste entre o subtotal e a totalização que acrescenta R$ 5,00 de frete
- **THEN** um subtotal de R$ 100,00 resulta em total de R$ 105,00 sem modificar a regra de subtotal

#### Scenario: Ausência de vazamento entre cálculos
- **WHEN** um cálculo recebe frete adicional e um segundo cálculo independente não o recebe
- **THEN** o segundo resultado contém somente seus próprios valores
