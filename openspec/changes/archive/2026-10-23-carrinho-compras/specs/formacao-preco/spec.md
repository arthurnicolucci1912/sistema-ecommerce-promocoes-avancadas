## Purpose

Produzir um resumo monetário consistente da compra por meio de etapas ordenadas, permitindo acrescentar regras futuras sem modificar as regras já existentes.

## ADDED Requirements

### Requirement: Cálculo monetário inicial
O cálculo SHALL somar preço unitário vezes quantidade de cada linha e produzir subtotal, descontos, frete e total. Valores MUST usar duas casas decimais e arredondamento HALF_EVEN, conforme RN-14. Sem descontos e frete, o total MUST ser igual ao subtotal.

#### Scenario: Múltiplas linhas
- **WHEN** o cálculo recebe 2 unidades a R$ 59,90 e 1 kit a R$ 79,90
- **THEN** o subtotal e o total são R$ 199,70, os descontos estão vazios e o frete é R$ 0,00

#### Scenario: Cálculo vazio
- **WHEN** não há linhas
- **THEN** subtotal, frete e total são zero e não há descontos

### Requirement: Extensão por novas etapas ordenadas
O cálculo SHALL executar etapas na ordem configurada. Registrar uma nova etapa MUST permitir modificar o resultado sem alterar as etapas existentes. Cálculos sucessivos MUST NOT compartilhar estado mutável de resultado.

#### Scenario: Acrescentar uma etapa
- **WHEN** é registrada uma etapa de teste entre o subtotal e a totalização que acrescenta R$ 5,00 de frete
- **THEN** um subtotal de R$ 100,00 resulta em total de R$ 105,00 sem modificar a regra de subtotal

#### Scenario: Ausência de vazamento entre cálculos
- **WHEN** um cálculo recebe frete adicional e um segundo cálculo independente não o recebe
- **THEN** o segundo resultado contém somente seus próprios valores
