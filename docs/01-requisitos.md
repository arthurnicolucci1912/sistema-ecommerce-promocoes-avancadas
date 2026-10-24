# 01. Requisitos

> **Objetivo:** listar os requisitos funcionais (RF) e não funcionais (RNF) extraídos do enunciado, com identificador, separados em obrigatórios e opcionais, para servir de base de rastreabilidade do roadmap e dos testes.
> **Etapas do roadmap relacionadas:** todas (cada requisito indica a etapa em que é entregue).
> **Status:** implementado (E1–E9): os requisitos foram atendidos pelas changes arquivadas até 2026-10-23 (ver [08-roadmap.md](08-roadmap.md)).

**Legenda de etapas:** E1 fundação, E2 catálogo e estoque, E3 carrinho, E4 checkout e pedido, E5 promoções e cupons, E6 pagamento e idempotência, E7 entrega e frete, E8 eventos, notificações e auditoria, E9 relatórios (ver [08-roadmap.md](08-roadmap.md)).

**Critério de classificação:** é *obrigatório* o que o enunciado expressa com "deverá". É *opcional* o que o enunciado trata como "poderá", "como funcionalidade adicional" ou "complementar".

## 1. Requisitos funcionais obrigatórios

### 1.1 Usuários e acesso

| ID | Requisito | Etapa |
|---|---|---|
| RF-01 | O sistema deve permitir que o cliente crie uma conta. | E1 |
| RF-02 | O sistema deve autenticar usuários (clientes e administradores). | E1 |
| RF-03 | O sistema deve possuir, no mínimo, os perfis **cliente** e **administrador**. | E1 |
| RF-04 | Cada usuário deve possuir permissões compatíveis com sua função (autorização por perfil). O administrador acessa catálogo, preços, estoques, campanhas, pedidos e relatórios. | E1 |
| RF-05 | O cliente deve acessar apenas os próprios dados e pedidos. | E1, E4 |

### 1.2 Clientes e endereços

| ID | Requisito | Etapa |
|---|---|---|
| RF-06 | O cliente deve possuir dados cadastrais suficientes para realizar compras. | E1 |
| RF-07 | O cliente pode possuir um ou mais endereços. | E1 |
| RF-08 | O cliente deve poder selecionar qual endereço será usado em cada entrega. | E4 |

### 1.3 Catálogo e categorias

| ID | Requisito | Etapa |
|---|---|---|
| RF-09 | O catálogo deve conter produtos com nome, descrição, preço e categoria. | E2 |
| RF-10 | Um produto deve poder ter variações (por exemplo tamanho e cor, ou capacidade de armazenamento). | E2 |
| RF-11 | O cliente deve poder consultar o catálogo de produtos. | E2 |
| RF-12 | O administrador deve poder gerenciar o catálogo e os preços. | E2 |
| RF-13 | Novos tipos de produto devem poder ser acrescentados sem mudanças excessivas no código existente. | E2 |
| RF-14 | Produtos devem ser organizados em categorias, e as categorias podem ter subcategorias. | E2 |
| RF-15 | A categoria deve poder ser usada como critério de promoções e de outras políticas comerciais, além da navegação. | E5 |

### 1.4 Estoque

| ID | Requisito | Etapa |
|---|---|---|
| RF-16 | O sistema deve controlar a disponibilidade dos produtos (por variação). | E2 |
| RF-17 | Não deve ser possível concluir normalmente uma compra quando a quantidade solicitada não está disponível. | E4 |
| RF-18 | O sistema deve definir o momento em que uma unidade é efetivamente reservada (proposta: conclusão do checkout, ver D5 em [05-regras-negocio.md](05-regras-negocio.md)). | E4 |
| RF-19 | A reserva deve ser liberada quando o pagamento for recusado, o pedido for cancelado ou a reserva expirar, e convertida em baixa quando o pagamento for aprovado. | E4, E6 |
| RF-20 | O sistema deve evitar que dois clientes adquiram simultaneamente a última unidade disponível. | E4 |
| RF-21 | O administrador deve poder gerenciar os estoques. | E2 |

### 1.5 Carrinho

| ID | Requisito | Etapa |
|---|---|---|
| RF-22 | Cada cliente pode possuir um carrinho com diferentes produtos e quantidades. | E3 |
| RF-23 | O carrinho deve permitir inclusão, alteração de quantidade e remoção de itens. | E3 |
| RF-24 | Os valores do carrinho devem ser recalculados sempre que seu conteúdo for modificado. | E3 |
| RF-25 | O carrinho deve considerar preços, descontos, promoções, cupons e eventuais custos adicionais. | E3, E5, E7 |

### 1.6 Promoções, condições, conflitos e administração

| ID | Requisito | Etapa |
|---|---|---|
| RF-26 | O sistema deve possuir um mecanismo flexível de promoções. | E5 |
| RF-27 | Devem ser implementados pelo menos **quatro tipos** de promoção, preferencialmente de naturezas distintas (por exemplo: leve 3 pague 2, percentual na primeira compra, valor fixo acima de um mínimo, percentual por categoria, frete grátis). | E5 |
| RF-28 | A arquitetura deve permitir incorporar uma quinta ou sexta promoção com impacto reduzido sobre as classes existentes. | E5 |
| RF-29 | Uma promoção deve poder ter condições de elegibilidade (primeira compra, valor mínimo, região, categoria, produto, período, perfil de cliente). | E5 |
| RF-30 | Uma promoção deve poder combinar várias condições simultâneas, sem métodos grandes e difíceis de manter. | E5 |
| RF-31 | O sistema deve tratar promoções válidas ao mesmo tempo, com estratégia clara e demonstrável: acumulável, exclusiva, maior desconto ou prioridade. | E5 |
| RF-32 | Parte das informações das campanhas deve ser configurável: nome, período de validade, prioridade e demais parâmetros. | E5 |

### 1.7 Cupons

| ID | Requisito | Etapa |
|---|---|---|
| RF-33 | O sistema deve permitir cupons promocionais com código, período de validade, limite de utilizações e condições específicas. | E5 |
| RF-34 | Os cupons podem ser de desconto percentual, de valor fixo ou de outros benefícios. | E5 |
| RF-35 | O sistema deve impedir o uso de cupons inexistentes, expirados ou incompatíveis com as regras da campanha. | E5 |

### 1.8 Formação do preço

| ID | Requisito | Etapa |
|---|---|---|
| RF-36 | O valor final deve ser formado em etapas (preços dos produtos, promoções, cupons, descontos e custos de entrega), sem concentrar o cálculo em um único método condicional. | E5 |
| RF-37 | O cliente deve conseguir compreender a composição do valor final, distinguindo subtotal, descontos aplicados, frete e total. | E3, E5 |

### 1.9 Checkout

| ID | Requisito | Etapa |
|---|---|---|
| RF-38 | O checkout deve transformar um carrinho válido em um pedido. | E4 |
| RF-39 | Antes de concluir, o checkout deve verificar preços, estoque, promoções, endereço, entrega e pagamento. | E4 |
| RF-40 | O checkout deve detectar mudanças ocorridas desde a inclusão no carrinho (promoção encerrada, preço alterado) e trabalhar com informações consistentes. | E4 |

### 1.10 Pedidos

| ID | Requisito | Etapa |
|---|---|---|
| RF-41 | Após o checkout deve ser criado um pedido. | E4 |
| RF-42 | O pedido deve preservar as informações comerciais do momento da compra; alterações futuras de preço não alteram pedidos antigos. | E4 |
| RF-43 | Cada pedido deve possuir um ciclo de vida (criado, aguardando pagamento, pago, em preparação, enviado, entregue, cancelado, pagamento recusado). | E4 |
| RF-44 | O sistema deve definir as transições válidas entre situações e rejeitar as inválidas. | E4 |
| RF-45 | O cliente deve poder consultar seus pedidos e o andamento deles. | E4 |

### 1.11 Pagamentos e idempotência

| ID | Requisito | Etapa |
|---|---|---|
| RF-46 | O sistema deve permitir diferentes formas de pagamento (cartão de crédito, PIX, boleto ou outras simuladas). | E6 |
| RF-47 | Os pagamentos são simulados: não há transações financeiras reais. | E6 |
| RF-48 | Interfaces diferentes de provedores externos não devem exigir mudanças significativas nas regras centrais; trocar de fornecedor deve ter baixo impacto. | E6 |
| RF-49 | O cliente deve poder acompanhar o pagamento. | E6 |
| RF-50 | O recebimento repetido da mesma confirmação de pagamento não deve gerar operações duplicadas (segunda baixa de estoque ou segunda alteração do pedido). O tratamento deve ser explícito. | E6 |

### 1.12 Entrega e frete grátis

| ID | Requisito | Etapa |
|---|---|---|
| RF-51 | O sistema deve permitir diferentes modalidades de entrega. | E7 |
| RF-52 | O custo de entrega pode variar por região, peso, quantidade, valor da compra ou modalidade. | E7 |
| RF-53 | Transportadoras ou serviços de entrega podem ser simulados, e novos serviços devem poder ser incorporados futuramente. | E7 |
| RF-54 | O cliente deve poder consultar o andamento da entrega. | E7 |
| RF-55 | O frete grátis deve poder depender de condições comerciais (valor mínimo, região, produto, categoria, perfil de cliente) e ser integrado ao mecanismo geral de promoções. | E5, E7 |

### 1.13 Cancelamento

| ID | Requisito | Etapa |
|---|---|---|
| RF-56 | O sistema deve permitir cancelar pedidos nos estados em que isso for definido como permitido. | E4 |
| RF-57 | Havendo estoque reservado, ele deve ser liberado no cancelamento, quando apropriado. | E4 |
| RF-58 | Havendo pagamento aprovado, deve ser possível simular um estorno. | E6 |
| RF-59 | O cancelamento deve permanecer registrado no histórico. | E4, E8 |

### 1.14 Notificações e eventos

| ID | Requisito | Etapa |
|---|---|---|
| RF-60 | O cliente deve poder receber notificações de acontecimentos importantes (criação do pedido, confirmação do pagamento, envio, cancelamento). | E8 |
| RF-61 | Os serviços de comunicação podem ser simulados, mas novos canais devem poder ser incluídos sem forte dependência entre o módulo de pedidos e a tecnologia de notificação. | E8 |
| RF-62 | Acontecimentos de interesse de vários componentes (por exemplo pagamento confirmado) devem ser tratados sem que uma única classe conheça todos os interessados. | E8 |

### 1.15 Segurança, auditoria e relatórios

| ID | Requisito | Etapa |
|---|---|---|
| RF-63 | Não devem ser usados dados reais de cartão de crédito; os dados de pagamento da demonstração são fictícios. | E6 |
| RF-64 | Operações administrativas importantes (alteração de preço, criação de campanha, cancelamento de pedido e outras relevantes) devem ser registradas. | E8 |
| RF-65 | O registro de auditoria deve identificar o usuário responsável e o momento da operação. | E8 |
| RF-66 | O sistema deve fornecer informações gerenciais, entre elas: valor total de vendas, quantidade de pedidos, ticket médio, produtos mais vendidos, campanhas mais utilizadas, total concedido em descontos e quantidade de cancelamentos. | E9 |

### 1.16 API

| ID | Requisito | Etapa |
|---|---|---|
| RF-67 | A API REST deve permitir consultar o catálogo, manipular o carrinho, aplicar cupons, realizar checkout, consultar pedidos e executar operações administrativas sobre catálogo e promoções. | E2 a E5 |

## 2. Requisitos funcionais opcionais

| ID | Requisito | Origem no enunciado | Etapa sugerida |
|---|---|---|---|
| RF-68 | Combos ou kits: conjuntos de produtos comercializados juntos, tratados de forma uniforme com produtos simples (Composite). | Carrinho de compras | E2, E3 |
| RF-69 | Perfis administrativos adicionais (operador comercial, responsável pelo estoque, responsável pelas promoções). | Usuários | E1 |
| RF-70 | Atributos adicionais de produto: marca, imagens, peso, dimensões e características específicas. O peso passa a ser necessário se o frete variar por peso (RF-52). | Catálogo de produtos | E2, E7 |
| RF-71 | Devolução parcial ou total de produtos, definindo tratamento de quantidade devolvida, estoque e valores correspondentes. | Devolução | Após E9 |
| RF-72 | Relatório do desconto concedido por cada campanha em um período. | Relatórios | E9 |
| RF-73 | Interface gráfica, considerada complementar à avaliação do backend. | API | Após E9 |

## 3. Requisitos não funcionais obrigatórios

| ID | Requisito | Etapa |
|---|---|---|
| RNF-01 | A aplicação deve ser desenvolvida em **Java e Spring Boot**, com Spring Data JPA para persistência e Spring Security para autenticação e autorização (recomendações do enunciado). | E1 |
| RNF-02 | O banco deve ser relacional (previsto: PostgreSQL), com scripts ou migrações versionados. | E1 |
| RNF-03 | Deve haver separação adequada entre domínio, lógica de aplicação, persistência, interface HTTP e integrações externas. | E1 |
| RNF-04 | Nenhuma classe (ou módulo) deve concentrar catálogo, carrinho, promoções, checkout, pagamento e entrega. | E1 a E8 |
| RNF-05 | **Extensibilidade:** novas promoções, formas de pagamento, transportadoras e canais de notificação devem entrar com impacto reduzido sobre o código existente. | E5 a E8 |
| RNF-06 | **Manutenibilidade:** regras combináveis e o cálculo do preço não devem virar métodos longos com grande quantidade de condicionais. | E5 |
| RNF-07 | Devem ser usados pelo menos **6 Design Patterns**, de mais de uma categoria, com no mínimo 2 comportamentais e 1 de criação. Cada um deve ter problema, motivo da escolha, classes participantes e benefícios documentados. Não são aceitas classes criadas só para casar nomes com patterns. | E2 a E8 |
| RNF-08 | Senhas não devem ser armazenadas em texto puro (hash com algoritmo adequado). | E1 |
| RNF-09 | Dados cadastrais devem ser tratados segundo boas práticas de segurança e privacidade. | E1 |
| RNF-10 | Devem existir testes automatizados (JUnit ou equivalente) que cubram os cenários obrigatórios listados em [09-testes.md](09-testes.md): cada tipo de promoção, múltiplas promoções simultâneas, estoque insuficiente, cupom inválido, pagamento recusado, transição inválida de pedido e confirmação de pagamento repetida. O foco é comportamento relevante, não percentual de cobertura. | E1 a E9 |
| RNF-11 | Integrações externas (pagamento, transportadoras, notificações) podem usar mocks, stubs ou implementações simuladas. | E6 a E8 |
| RNF-12 | A documentação deve conter descrição do sistema, requisitos, modelo de domínio, arquitetura, casos de uso, ao menos um diagrama de classes, um diagrama do ciclo de vida do pedido e diagramas de sequência (checkout, aplicação de promoções ou confirmação de pagamento). | E1 a E9 |
| RNF-13 | O desenvolvimento deve ser incremental, com o sistema executável a cada etapa, e o código deve ficar em repositório Git, cujo histórico é evidência de participação da equipe. | E1 a E9 |
| RNF-14 | A entrega final inclui código-fonte, scripts ou migrações do banco, documentação, diagramas UML, testes e instruções de execução. A apresentação deve mostrar uma compra completa, um cenário com múltiplas promoções e um cenário de erro (pagamento recusado, estoque insuficiente ou cupom inválido). Todos os integrantes devem conhecer a arquitetura e os patterns. | E9 |

## 4. Requisitos não funcionais opcionais (recomendados)

| ID | Requisito | Etapa |
|---|---|---|
| RNF-15 | Documentar a API com OpenAPI/Swagger (recomendado pelo enunciado). | E2 em diante |

## 5. Cobertura das seções do enunciado

Todas as seções do enunciado relevantes têm ao menos um requisito:

| Seção do enunciado | Requisitos |
|---|---|
| Usuários | RF-03, RF-04, RF-69 |
| Clientes e endereços | RF-01, RF-06, RF-07, RF-08 |
| Catálogo de produtos | RF-09 a RF-13, RF-70 |
| Categorias | RF-14, RF-15 |
| Estoque | RF-16 a RF-21 |
| Carrinho de compras | RF-22 a RF-25, RF-68 |
| Promoções | RF-26 a RF-28 |
| Condições de elegibilidade | RF-29, RF-30 |
| Conflitos entre promoções | RF-31 |
| Cupons | RF-33 a RF-35 |
| Formação do preço | RF-36, RF-37 |
| Checkout | RF-38 a RF-40 |
| Pedidos | RF-41 a RF-45 |
| Pagamentos | RF-46 a RF-49 |
| Idempotência | RF-50 |
| Entrega e frete | RF-51 a RF-54 |
| Frete grátis | RF-55 |
| Cancelamento | RF-56 a RF-59 |
| Devolução (opcional) | RF-71 |
| Notificações | RF-60, RF-61 |
| Eventos do sistema | RF-62 |
| Segurança | RF-02, RF-04, RF-05, RF-63, RNF-08, RNF-09 |
| Auditoria | RF-64, RF-65 |
| Relatórios | RF-66, RF-72 |
| Administração das promoções | RF-32 |
| Aplicação de Design Patterns | RNF-07 |
| Arquitetura e implementação | RNF-01 a RNF-06 |
| API | RF-67, RF-73, RNF-15 |
| Testes automatizados | RNF-10, RNF-11 |
| Documentação | RNF-12 |
| Desenvolvimento incremental e entrega final | RNF-13, RNF-14 |

## 6. Resumo quantitativo

| Tipo | Obrigatórios | Opcionais |
|---|:-:|:-:|
| Requisitos funcionais | 67 (RF-01 a RF-67) | 6 (RF-68 a RF-73) |
| Requisitos não funcionais | 14 (RNF-01 a RNF-14) | 1 (RNF-15) |
