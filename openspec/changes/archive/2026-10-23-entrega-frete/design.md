# Design

## Context
Ver proposal.md. Entrega possui apenas ProvedorFrete, ContextoFrete e CotacaoFrete; o catálogo não informa peso. ContextoPreco já contém freteBruto, mas ContextoDeCompra o fornece como zero. A cadeia existente avalia promoções/cupom com esse contexto antes de transportar frete bruto para a totalização. Pedido guarda endereço/frete/descontos; não guarda modalidade nem prazo. Confirmação compara todos os dados atuais, incluindo identidade do cupom. E6 já protege pagamento/cancelamento e consulta o total preservado. Base validada: 562 testes.

## Goals / Non-Goals
Cotação simulada determinística, escolha explícita e snapshot verificável sem introduzir dependência de transportadoras no pedido. Não acrescentar peso fictício ao catálogo, rastreamento real, criação de remessa, estados novos, jobs de entrega ou cotação persistida no carrinho. O carrinho continua resumo antes da escolha de entrega; documentar essa distinção no OpenAPI e nos exemplos.

## Decisions

### 1. Contratos e duas APIs simuladas
Reutilizar ProvedorFrete e acrescentar fornecedor() como identidade estável. Cada bean representa fornecedor/modalidade; identificador público da opção é fornecedor + ':' + modalidade. RegistroProvedoresFrete valida códigos únicos no startup e enumera provedores sem switch de fornecedor. CotacaoFrete mantém modalidade/custo/prazo; OpcaoEntrega agrega código, fornecedor, modalidade e cotação. Validar IDs não vazios/limitados, modalidade consistente com provedor, dinheiro não negativo/cabível em NUMERIC(12,2), prazo inteiro positivo e UF brasileira válida. Não aceitar resposta incoerente do simulador.

AdaptadorEntregaEconomica traduz uma API por UF retornando preço em centavos e prazo; AdaptadorEntregaExpressa traduz uma API por região retornando documento de preço/prazo. Adaptees concretos ficam em entrega.infra, implementações do contrato também. ContextoFrete permanece compatível: pesoKg zero significa que estes simuladores não usam peso; nunca declarar esse número como peso físico conhecido. Custo deste incremento depende somente de região e modalidade, alternativa expressamente permitida pelo roadmap. Rejeitar Strategy adicional para tarifa: duplicaria o ponto de variação dos provedores sem necessidade.

### 2. Premissas da simulação
Tabela de demonstração (valores não representam serviço comercial real). Prazo estimado em dias úteis após aprovação do pagamento, sem calcular data-limite ou feriados:

| Região | Econômica (R$ / dias) | Expressa (R$ / dias) |
|---|---|---|
| Sudeste | 25 / 5 | 40 / 2 |
| Sul | 30 / 6 | 48 / 3 |
| Centro-Oeste | 35 / 7 | 56 / 3 |
| Nordeste | 40 / 9 | 64 / 4 |
| Norte | 45 / 12 | 72 / 6 |

Configuração de servidor permite alterar tarifas, prazos e UFs atendidas por cada simulador, validada no startup (custos não negativos, prazos positivos, UFs válidas). Por padrão os dois cobrem todas as 27 UFs. Configuração de cobertura vazia é válida para demonstrar ausência de atendimento. Valores SE/NE seguem exemplos de RN-15; os demais e prazos são premissas explícitas desta simulação. Sem administração HTTP de transportadoras neste incremento.

### 3. Consulta e escolha pelo comprador
POST /api/clientes/me/checkout/fretes com {enderecoId} retorna 200 {opcoes:[...]}, ordenadas por custo bruto, prazo e código, usando carrinho/catalogo atuais e endereço próprio. Cada opção é uma cotação bruta; descontos e total efetivo são calculados na prévia da modalidade selecionada. CLIENTE/JWT resolvido pela aplicação, com 401/403/404/400/409 conforme spec. Identidade do cliente, subtotal, UF e valores nunca vêm do corpo do chamador.

POST /checkout/previa passa a exigir {enderecoId,opcaoEntrega}; POST /checkout exige {enderecoId,opcaoEntrega,confirmacao}. opcaoEntrega é o código estável e não um preço nem objeto confiável. Campo ausente é 400 (BREAKING consciente); código inexistente/sem cobertura é 409 com opções atuais. Sem atendimento a consulta retorna lista vazia; não há fallback automático. Falha em parte dos provedores omite as opções afetadas, preserva as válidas e registra diagnóstico; se nenhum resultado válido existe e ao menos um provedor elegível falhou, responder 503. Resposta inválida do provedor conta como falha. Serviço de cotação não cria pedido nem modifica seleção/uso de cupom.

### 4. Revalidação e integração do preço
Extrair o carregamento/leitura da compra reutilizável em RevalidarCheckout, evitando copiar a lógica de SKUs/cliente/endereço em um controller de frete. Cotar com a seleção atual e alimentar ContextoDeCompra/ContextoPreco.freteBruto antes de PipelinePreco.calcular. Manter cadeia subtotal -> promoções -> cupom -> frete bruto -> total: regras anteriores já usam o frete do contexto para escolher benefícios, portanto não mover handlers nem aplicar frete grátis novamente no fim.

A confirmação acrescenta entrega {codigo,fornecedor,modalidade,prazoDias}, mantém frete como bruto e acrescenta freteLiquido derivado. Snapshot do pedido guarda a escolha comercial; total continua subtotal - todosDescontos + freteBruto. Não persistir valor líquido duplicado: derivar bruto menos descontos FRETE no preço/response. Frete grátis de 25 sobre bruto 25 aparece como frete=25, desconto FRETE=25, freteLiquido=0. Cupom de frete limitado ao residual, e uso só quando benefício efetivo for positivo.

ConferirConfirmacao compara escolha inteira e frete líquido, além de campos existentes. Alteração de prazo/fornecedor ou troca de modalidade com mesmo preço exige 409. Frete grátis não oculta mudança da tarifa bruta: frete e descontos mudam e exigem reconfirmação. Não comparar instante efêmero da cotação, que causaria conflito a cada requisição. Opção ainda válida com mudança de dados retorna divergencias e previaAtualizada; se deixa de existir/atender, retornar 409 com opcoesEntrega atualizadas, sem escolher outra nem produzir prévia falsa. Ao reenviar, o cliente seleciona opção válida e pede nova prévia.

### 5. Snapshot e migração
Adicionar DadosEntregaSelecionada no domínio pedido (código até 128, fornecedor até 80, modalidade até 40 e prazoDias positivo), sem associação ao módulo entrega. DadosNovoPedido ganha campo opcional para compatibilidade dos construtores internos e dados legados; novas compras HTTP exigem seleção. V7 acrescenta colunas opcionais ao pedido com restrição de preenchimento conjunto, prazo positivo e imutabilidade no mapeamento JPA. Não preencher artificialmente registros anteriores. PedidoResponse expõe opcaoEntrega opcional e freteLiquido derivado, preservando campos existentes.

A transação do checkout continua incluindo pedido, reserva, cupom e limpeza versionada da seleção. Falhas posteriores à cotação não deixam snapshot parcial. Pagamento usa o novo total com frete sem recotar; cancelamento e estorno devolvem o total pago original. Reutilizar estados, permissões, eventos e endpoints existentes de preparação/envio/entrega; testar consulta do cliente em cada etapa sem nova infraestrutura de andamento.

### 6. Design Patterns
| Pattern | Papel | Problema do enunciado | Classes participantes | O que fica mais fácil de modificar |
|---|---|---|---|---|
| Adapter | Target/adapters/adaptees | Transportadoras com APIs diferentes | ProvedorFrete, AdaptadorEntregaEconomica, AdaptadorEntregaExpressa e simuladores de transporte | Registrar fornecedor/modalidade sem alterar checkout |
| Chain of Responsibility | handlers existentes | Composição extensível do preço | PipelinePreco, EtapaPromocoes, EtapaCupom, EtapaFreteBruto, EtapaTotal | Consumir cotação com benefícios sem duplicar regras |
| Strategy | estratégias existentes | Benefícios e conflitos variáveis | PromocaoFreteGratis, benefícios de cupom, PoliticaConflito | Trocar condição/benefício mantendo base de frete |
| Facade | coordenador existente | Compra atômica atravessa módulos | CheckoutFacade, RevalidarCheckout | Conectar entrega sem regras no controller |
| State | contexto/estados existentes | Andamento depende do estado | Pedido e EstadoPedido existentes | Preservar transições e rejeição de saltos |

Adapter substitui switch de APIs concretas no núcleo. Chain substitui cálculo duplicado no checkout; Strategy reutilizada substitui if de frete grátis fora do motor. Facade substitui orquestração transacional no HTTP; State substitui nova lógica de status na entrega. Não criar patterns próprios para registro, DTOs, migração ou cálculo aritmético do frete líquido. Documentar papéis/classes reais no Javadoc e docs/04, inclusive teste de novo provedor registrado sem alterar consumidores.

## Risks / Trade-offs
- Contrato HTTP passa a exigir modalidade → atualizar OpenAPI, exemplos e fixtures de todas as compras E4b–E6 na mesma entrega; não mascarar regressão zerando tarifas nos testes de integração.
- Benefício de frete pode afetar conflitos com promoções de itens → recalcular o motor inteiro com a cotação corrente e verificar cenários de exclusividade/residual/teto.
- Cotação muda sem mudar total → comparar identidade/prazo e parcelas brutas, nunca só total.
- Falha externa não equivale a falta de cobertura → distinguir resposta vazia de 503 e nunca substituir por zero.
- Main specs ainda precedem deltas E5/E6 → deltas de checkout/preço desta change incorporam integralmente os requisitos/cenários E5; em futura sincronização/arquivamento aplicar alterações em ordem E3/E4b/E5/E6/E7 para não sobrescrever comportamento recente.

## Migration Plan
V7 aditiva sobre V6, testada com pedido anterior e cobrança/confirmação existentes preservados. Legados expõem opcaoEntrega=null e mantêm frete original; novos pedidos HTTP preservam entrega obrigatória. Construtores de apoio internos podem continuar formando snapshots sem cotação em testes de domínio/ciclo de vida. Migrar helpers de checkout e valores esperados da suíte, incluindo pagamento de 20+25=45 no exemplo SP, sem reduzir as garantias de estoque/cupom/concorrência. Atualizar demonstração dev para usar opção explícita quando criar pedido novo, preservando idempotência e pedidos já existentes. Rollback operacional com versão compatível e colunas adicionais preservadas; não apagar snapshots nem alterar migrações antigas. Sem commit/PR/arquivamento automático.
