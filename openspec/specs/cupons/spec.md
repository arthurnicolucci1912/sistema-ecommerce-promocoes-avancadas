# cupons Specification

## Purpose
Permitir benefícios por código promocional com validação clara, limites de uso confiáveis e vínculo por pedido, preservando a consistência entre seleção, desconto, consumo e devolução.

## Requirements

### Requirement: Cadastro e consulta de cupons
ADMIN e PROMOCOES SHALL cadastrar, consultar, listar, alterar e ativar/desativar cupons com código único, nome, tipo de benefício, parâmetros, início/fim, compatibilidade com promoções, condições, limite total e limite opcional por cliente. Códigos SHALL ser normalizados removendo espaços externos e convertendo para maiúsculas, com 3 a 40 caracteres de letras ASCII, dígitos, hífen ou sublinhado. Código duplicado MUST retornar 409, configuração inválida 400 e recurso inexistente 404; uso corrente e limites MUST ser consultáveis. Sem token MUST retornar 401; perfis sem ADMIN/PROMOCOES, 403. Alteração administrativa MUST NOT apagar usos nem reduzir limites abaixo do consumo ativo existente.

#### Scenario: Código equivalente
- **WHEN** existe BEMVINDO10 e alguém tenta cadastrar ` bemvindo10 `
- **THEN** recebe 409 sem criar cupom duplicado

### Requirement: Validação com motivo
Aplicar ou substituir cupom SHALL rejeitar código inexistente, inativo, fora da vigência, esgotado, acima do limite por cliente ou incompatível com condições/campanhas, com 409 e motivo estruturado. Código malformado MUST retornar 400. Rejeição MUST preservar a seleção e o cupom anterior. Promoção exclusiva aplicada ou cupom não acumulável com desconto de promoção aplicado MUST causar incompatibilidade. As condições combináveis SHALL ter os mesmos comportamentos disponíveis às campanhas.

#### Scenario: Substituição inválida
- **WHEN** o carrinho tem BEMVINDO10 e o cliente tenta substituir por cupom expirado
- **THEN** recebe motivo de validade e o carrinho continua com BEMVINDO10

#### Scenario: Cupom não acumulável
- **WHEN** há desconto efetivo de campanha e o cupom não aceita acumulação
- **THEN** o cupom é rejeitado como incompatível

### Requirement: Benefícios e cálculo
Cupons SHALL oferecer desconto percentual sobre os itens após promoções, valor fixo limitado a essa base e benefícios de frete grátis ou redução fixa de frete, limitada ao frete ainda não descontado. Percentual MUST ser maior que zero e no máximo 100; valor fixo MUST ser positivo. Cada desconto SHALL ser arredondado com HALF_EVEN e discriminado por nome, código, origem e alvo; totais nunca podem ficar negativos nem contar desconto de frete duas vezes. Novo tipo de benefício registrado MUST funcionar sem alterar benefícios existentes. Benefício efetivo zero MUST NOT consumir utilização.

#### Scenario: Percentual após promoções
- **WHEN** subtotal R$ 400,00 tem R$ 110,00 de promoções e o cupom concede 10%
- **THEN** o cupom concede R$ 29,00 e o total sem frete é R$ 261,00

#### Scenario: Fixo limitado
- **WHEN** há R$ 290,00 após promoções e cupom de R$ 400,00
- **THEN** o cupom concede R$ 290,00 e o total sem frete é zero

#### Scenario: Benefício de frete sem cotação
- **WHEN** o cupom é válido para frete e a cotação atual é zero
- **THEN** nenhum desconto positivo nem uso consumido é registrado por esse benefício

#### Scenario: Novo benefício
- **WHEN** um benefício adicional de cupom é registrado e configurado
- **THEN** ele é validado e calculado sem mudanças nos benefícios existentes

### Requirement: Consumo na compra com limites concorrentes
Somente a conclusão bem-sucedida do checkout com desconto efetivo de cupom SHALL consumir um uso, vinculado ao pedido. Consultas, prévias e aplicação no carrinho MUST NOT consumir usos. Limites total e por cliente MUST ser respeitados sob concorrência; um pedido MUST NOT consumir duas vezes. Criação de pedido, reserva, consumo e limpeza MUST ser atômicos. Falha de estoque, configuração desatualizada ou qualquer falha posterior MUST desfazer o consumo e as outras escritas da tentativa.

#### Scenario: Última utilização
- **WHEN** dois clientes concluem simultaneamente com a última utilização disponível do mesmo cupom
- **THEN** apenas um consome, o outro recebe 409 e mantém o carrinho, sem pedido ou reserva da tentativa perdedora

#### Scenario: Limite individual
- **WHEN** o cliente já tem um uso ativo de cupom cujo limite individual é um e tenta outra compra
- **THEN** a segunda compra é rejeitada sem consumir o limite global restante

#### Scenario: Falha após consumo
- **WHEN** o uso foi consumido e a persistência da limpeza do carrinho falha
- **THEN** uso, pedido e reserva da tentativa são desfeitos e a seleção permanece

### Requirement: Devolução idempotente
Cancelamento de pedido ou recusa de seu pagamento SHALL devolver o uso consumido do cupom na mesma transação da mudança do pedido, no máximo uma vez. A devolução SHALL restabelecer as vagas total e individual e preservar a identificação histórica do uso e o snapshot de descontos. Devolver novamente ou devolver para pedido sem cupom MUST ser operação sem efeito e sem erro. Cancelamento posterior a recusa MUST NOT devolver uma segunda vez.

#### Scenario: Recusa e cancelamento
- **WHEN** um pedido com cupom é recusado e depois cancelado
- **THEN** exatamente uma vaga global e uma vaga individual são devolvidas

#### Scenario: Cancelamento de pedido pago
- **WHEN** o administrador cancela um pedido pago que consumiu cupom
- **THEN** o uso é devolvido uma vez, sem alterar o desconto preservado no pedido
