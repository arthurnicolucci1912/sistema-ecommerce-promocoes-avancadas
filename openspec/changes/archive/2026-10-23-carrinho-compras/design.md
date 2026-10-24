# Design

## Context

Ver proposal.md para motivação. Os contratos `ItemVendavelInfo` e `ConsultarSaldo.porVariacoes` já permitem consulta em lote. `EtapaPreco`, `CalculoPreco`, `ItemPreco` e `ContextoPreco` existem, sem pipeline concreto. O teste `contratosDaOnda0SoTemImplementacaoNaInfra` ainda impede implementações de negócio fora da infraestrutura e precisa evoluir de maneira delimitada para E3. Não há pacote de carrinho nem tabela correspondente.

## Goals / Non-Goals

**Goals:** preservar fronteiras entre módulos, usar os contratos existentes e devolver um resumo consistente sem escrever no estoque.

**Non-Goals:** criar pedidos, reservar estoque, aplicar promoções, selecionar entrega ou cobrar pagamento. A revisão conjunta dos contratos é atividade humana ainda pendente, não será declarada concluída por testes locais.

## Decisions

### Persistência e concorrência

`Carrinho` é o agregado por cliente; `ItemCarrinho` guarda SKU, quantidade e identificação/nome de referência para manter visível uma linha indisponível. O preço apresentado vem sempre do catálogo. `CarrinhoRepository` é implementado via JPA, com tabela `carrinho` (cliente único, FK e versão) e `item_carrinho` (SKU único no carrinho, quantidade positiva). A consulta vazia não cria registro. A primeira escrita cria o carrinho; uma corrida pela unicidade deve gerar 409 recuperável. Escritas concorrentes usam versão otimista no agregado, inclusive quando só filhos mudam, para evitar perda de alterações. Alternativa em memória rejeitada por perder seleção no reinício; tabela apenas de linhas rejeitada por dificultar controle de concorrência do agregado.

### Serviço de aplicação e API

`GerenciarCarrinho` resolve o cliente pela identidade autenticada, coordena persistência e recálculo, e exige CLIENTE na fronteira de aplicação. `CarrinhoDoClienteController` expõe GET `/api/clientes/me/carrinho`, POST `/itens` com SKU/quantidade, PUT `/itens/{sku}` com quantidade e DELETE `/itens/{sku}`. Todas retornam 200 com a representação atual, inclusive remoção ausente. O cliente nunca fornece um clienteId. DTOs validam formato e domínio valida invariantes; estouro é convertido em 400. Falhas de escrita fazem rollback completo.

O recálculo usa a consulta aditiva `ConsultarItensVendaveis.existentesPorSkus` (omite ausentes sem alterar o 404 dos métodos estritos) e consulta SKUs existentes em lote; referências ausentes/inativas usam a identificação guardada e aviso, sem inviabilizar remoção. Uma referência sem preço corrente usa o último preço conhecido, explicitamente marcado como referência indisponível, nunca apto a checkout. Aumento/inclusão de inativo retorna 409; redução/remoção permanece possível. Alteração de linha inexistente retorna 404.

### Disponibilidade

`ConsultarDisponibilidadeDoCarrinho` expande `ItemVendavelInfo.componentes`, multiplica pela quantidade com aritmética verificada e agrega por variação antes de chamar `ConsultarSaldo.porVariacoes`. Saldo ausente equivale a indisponível. A resposta identifica componentes insuficientes com demanda e saldo. A falta de saldo é informativa, conforme RN-17; não se injeta serviço de reserva. O preço próprio do combo continua sendo uma única linha, sem somar preços de componentes. Alternativa de verificar cada linha isolada rejeitada por ignorar componentes compartilhados.

### Design Patterns

| Pattern | Papel | Problema do enunciado | Classes participantes | Modificação facilitada |
|---|---|---|---|---|
| Chain of Responsibility | Coordenador, handlers, contexto | Formação do preço evolui com promoções, cupons e frete | `PipelinePreco`, `EtapaPreco`, `EtapaSubtotal`, `EtapaTotal`, `CalculoPreco` | Registrar etapas ordenadas sem alterar handlers existentes |
| Composite (existente) | Consumidor do contrato uniforme | Kits e variações têm preços e componentes diferentes | `ConsultarItensVendaveis`, `ItemVendavelInfo`, `ConsultarDisponibilidadeDoCarrinho` | Consumir novos itens vendáveis sem condicionais por tipo |

`PipelinePreco` recebe uma lista ordenada explicitamente na configuração Spring e cria novo `CalculoPreco` por chamada. `EtapaSubtotal` soma `ItemPreco.subtotal`; `EtapaTotal` finaliza subtotal menos descontos de itens, mais frete líquido, observando `AlvoDesconto` para não descontar frete duas vezes. As duas implementações ficam no domínio; o coordenador e a composição de beans ficam em aplicação/infra. A regra ArchUnit passa a permitir especificamente implementações de `EtapaPreco` em `preco.dominio`, preservando restrições dos demais contratos da onda 0. Não se usa varredura dependente de ordem incidental de beans.

Alternativa de cálculo único com condicionais rejeitada por concentrar mudanças futuras. Para Composite, alternativa de testar tipo concreto de combo rejeitada por acoplar carrinho aos tipos do catálogo. CRUD, DTOs, persistência e cálculo de disponibilidade não introduzem pattern próprio. Participantes novos recebem Javadoc com papel e motivação, e docs/04 registra classes reais.

O campo legado `ItemPreco.varianteId` recebe o identificador do item vendável; para combos ele não representa um componente de estoque. O SKU é a identidade comercial e apenas a composição do catálogo determina demanda física. Isso será registrado em docs/03; não haverá dependência de entidades de catálogo no domínio de carrinho.

## Risks / Trade-offs

- Estoque/preço podem mudar após resposta → disponibilidade é informativa; revalidação definitiva pertence ao checkout.
- Atualizar apenas filhos pode não incrementar versão do pai → teste de concorrência e incremento explícito de versão no agregado.
- Contrato provisório de contexto de preço inclui primeira compra e UF → nesta etapa nenhum handler usa esses campos; não inventar elegibilidade nem chamar integrações futuras.
- Documentação do roadmap ainda diz que catálogo está em revisão → corrigir a situação com base no arquivo arquivado, sem atribuir revisão humana inexistente.

## Migration Plan

Adicionar V4 sem editar V1–V3; verificar aplicação do zero em PostgreSQL com Testcontainers e reinício com carrinho persistido. Em rollback do aplicativo, tabelas aditivas podem permanecer sem uso; remoção manual de tabelas exige decisão posterior, sem apagar dados automaticamente. Verificar a suíte com `./mvnw verify` antes de considerar implementação pronta.
