# Design

## Context

Ver `proposal.md` para motivação e `specs/` para os contratos de comportamento. E3/E4b estão implementadas localmente; suas specs principais foram sincronizadas, mantendo as changes ativas. A regressão da entrega anterior tem 507 testes aprovados. Não existe persistência de campanhas/cupons: `promocao.dominio` contém apenas contratos.

Pontos observados no código: `ContextoPreco` ainda recebe `primeiraCompra=false` e UF nula no carrinho; `DescontoCandidato` não tem criação para o desempate D4; `DescontoAplicado` não discrimina origem; `RevalidarCheckout` monta o snapshot com descontos vazios. A confirmação existente compara nome/alvo/valor, sem identidade de campanha/cupom. O pedido já tem origem PROMOCAO/CUPOM e eventos síncronos; os ouvintes de estoque já reagem a recusa/cancelamento. ArchUnit permite estratégias da onda 0 somente em infraestrutura e precisa liberar especificamente as implementações de negócio da E5 no domínio de promoções.

## Goals / Non-Goals

**Goals:** conectar configuração persistente a regras extensíveis, manter cálculo determinístico e tornar o consumo de cupom consistente com a transação já existente. Origem e identidade devem sobreviver ao snapshot para rastreabilidade e relatórios futuros.

**Non-Goals:** gateway de pagamento, seleção de entrega, cotação externa, auditoria administrativa completa e relatórios. Benefícios de frete são implementados com cotação explícita de entrada e produção em zero até a E7; não criar transportadoras fictícias na E5. Perfis comerciais VIP não existem no cadastro atual; a condição de perfil usa os perfis autenticados existentes, sem inventar segmentos.

## Decisions

### 1. Configuração, persistência e administração

`PromocaoConfig` e `Cupom` representam dados persistidos com id, versão, nome, ativo, início/fim UTC e parâmetros. Campanha tem tipo, prioridade, modo e árvore de condições. Cupom tem código normalizado e imutável após criação, tipo de benefício, acumulável, limites e condições. JSONB guarda mapas de parâmetros e a árvore declarativa (`tipo`, `parametros`, `filhos`); o domínio usa tipos imutáveis sem depender de Jackson. Não persistir classes Java serializadas nem executar expressões fornecidas pelo administrador.

Migração V5 aditiva: `promocao_config`, `cupom`, `saldo_cupom` (usos ativos), `uso_cupom` (pedidoId, clienteId, cupomId, estado CONSUMIDO/DEVOLVIDO e datas), coluna de código selecionado em `carrinho` e referências opcionais em `pedido_desconto` (`referencia_id`, `codigo`). Restrições: código único normalizado, período válido, contadores não negativos, limites positivos e um uso por pedido; índices para vigência e consulta de usos ativos por cupom/cliente. IDs entre módulos são valores, sem associações JPA a agregados externos. Dados históricos anteriores continuam válidos com referências nulas.

`GerenciarPromocoes` e `GerenciarCupons` expõem GET listado/detalhe, POST criação, PUT alteração com versão e POST ativação/inativação em `/api/admin/promocoes` e `/api/admin/cupons`. Exigir ADMIN/PROMOCOES na aplicação e nas regras HTTP antes do matcher genérico `/api/admin/**`. Paginação conforme o padrão do projeto (20, máximo 100). Expor catálogos de tipos/condições registradas via GET `/tipos` e `/condicoes`, antes de `/{id}`, para descrever parâmetros aceitos sem uma lista paralela na web. Tipo inexistente, percentual inválido, quantidade inválida, referência de categoria/SKU inexistente e árvore inválida respondem 400. Alvo administrativo inexistente responde 404; código duplicado, limite abaixo do uso ativo e versão antiga respondem 409. Não excluir fisicamente campanha/cupom.

Alternativa descartada: uma subclasse JPA por tipo de promoção, que faria cada tipo novo exigir esquema e alteração dos serviços administrativos. JSONB é limitado a dados validados por cada fábrica, não um atalho para configuração sem contrato. Usar configuração vigente para novas avaliações; o pedido guarda uma cópia imutável.

### 2. Estratégias e criação por registro

Manter `Promocao` e `CondicaoElegibilidade` no domínio. Estratégias concretas: `PromocaoLeveXPagueY` (parâmetros SKU/X/Y), `PromocaoPercentualPorCategoria`, `PromocaoValorFixoAcimaDe`, `PromocaoPercentualPrimeiraCompra` e `PromocaoFreteGratis`. Cada uma recebe metadados e condição raiz; estruturas comuns de metadados/validação podem ser compartilhadas sem centralizar o cálculo de todos os tipos.

`PromocaoFactory` define tipo, descrição de parâmetros e criação/validação. `PromocaoFactoryRegistry` recebe as fábricas registradas, rejeita chaves duplicadas e materializa a estratégia para cada configuração carregada. Fábricas concretas correspondem aos cinco tipos; novas fábricas entram pela composição de beans, sem `switch` no motor, controller ou persistência. `BeneficioCupom` e seu registro usam a mesma separação de criação e cálculo para percentual, fixo, frete grátis e redução fixa de frete; o cupom não simula uma campanha adicional sujeita à D4.

`FonteDePromocoes` e `FonteDeCupons` são portas de domínio; adaptadores em infra consultam a persistência e os registros. `MotorDePromocoes` recebe as estratégias e `PoliticaConflito`. Handlers de preço podem consumir essas portas e serviços de domínio sem depender de classes de aplicação. Infra configura os objetos puros como beans.

### 3. Condições e contexto obtido no servidor

Folhas: `PrimeiraCompra`, `ValorMinimo`, `CategoriaDoProduto`, `ItemPorSku`, `RegiaoDeEntrega`, `PeriodoDeVigencia` e `PerfilDoCliente`. Compostos: `CondicaoE`, `CondicaoOu`, `CondicaoNao`. Um registro de criadores de condição compila a árvore recursiva, com limites de 16 níveis/128 nós, E/OU com dois ou mais filhos e NÃO com um. Categoria usa ancestrais do contrato já existente; SKU identifica o item comercial, inclusive kit. A composição é puramente booleana: região desconhecida é falsa e NÃO mantém sua semântica normal.

Evoluir `ContextoPreco` de forma aditiva com código do cupom, perfis autenticados e frete bruto de entrada, preservando o construtor atual com valores padrão. Adicionar um serviço de montagem de contexto que obtenha dados do servidor, com Clock injetado para o instante único da avaliação. O carrinho usa o endereço principal ativo para UF; sem ele, região é desconhecida. O checkout sempre usa o endereço escolhido e pode apresentar descontos diferentes, exigindo confirmação. Nenhum cliente envia sinal de primeira compra ou perfis para determinar desconto.

**Decisão do autor:** favorecimento ao desconto, interpretado como primeira compra baseada em pagamento aprovado, não mera criação de pedido. Acrescentar consulta à porta de pedido para existência de histórico com destino PAGO até o instante avaliado; isso cobre pedidos hoje EM_PREPARACAO/ENVIADO/ENTREGUE ou CANCELADO depois de pagar. Não usar apenas `existeDoCliente` nem apenas a situação atual. Recusa/cancelamento sem pagamento anterior não impede o benefício. Vários pedidos ainda não pagos podem ser elegíveis enquanto não existir pagamento aprovado; não criar limite oculto de primeira compra diferente dessa regra. Se um pagamento for aprovado antes da próxima revalidação, os descontos são recalculados.

### 4. Política D4, tetos e identidade dos descontos

Acrescentar `criadoEm` a `DescontoCandidato`, mantendo construtor anterior para consumidores atuais. `PoliticaPadrao` seleciona exclusiva de maior desconto; na ausência, acumuláveis por prioridade mais a melhor não acumulável. Empate: desconto, prioridade, criação mais antiga e menor id. A ordenação é explícita e estável, sem depender da ordem devolvida pelo banco. `PoliticaSomenteMaiorDesconto` é uma segunda implementação demonstrável por configuração/teste.

Aplicar teto de ITENS separadamente de FRETE: depois da seleção, distribuir o limite preservando prioridade maior, reduzindo descontos de menor prioridade e removendo valores efetivos zero. Promocionais percentuais são calculados sobre os valores brutos elegíveis; cupom usa o saldo líquido de itens. Frete grátis e redução fixa nunca excedem a cotação ainda não descontada. Guardar informação de exclusividade selecionada para rejeitar cupom incompatível; não tentar inferir modo pelo nome da campanha.

Evoluir `DescontoAplicado` com origem própria do módulo de promoção (PROMOCAO/CUPOM), id de referência, código quando cupom e metadado de acumulação. Preservar construtor de quatro argumentos e o acesso legado `promocaoId()` para descontos de promoção. Não importar enum do pedido para o cálculo. `DadosNovoPedido.Desconto` e `DescontoPedido` recebem referência/código opcionais de forma aditiva; o checkout faz mapeamento explícito de origem para `OrigemDesconto`, preserva alvo e soma os descontos efetivamente aplicados. Atualizar o DTO público do pedido com campos opcionais, sem exigir valores inexistentes nos registros antigos.

### 5. Cadeia de preço e benefícios de frete

Ordem explícita em `ConfiguracaoPreco`: subtotal → `EtapaPromocoes` → `EtapaCupom` → `EtapaFreteBruto` → total. O frete bruto já faz parte do contexto e pode ser usado para avaliar benefícios antes de sua materialização no detalhamento. `EtapaFreteBruto` apenas transporta a cotação de entrada ao cálculo; em produção é zero até a E7. Não introduzir cotação nem modalidades nesta change.

Handlers ficam em `preco.dominio`, dependendo somente das portas e regras de `promocao.dominio`. `CalculoPreco` passa a guardar situação do cupom e metadados necessários de campanhas, além de descontos. O subtotal e a etapa final existentes continuam responsáveis por seus cálculos, sem duplicá-los no carrinho/checkout. Verificar HALF_EVEN por benefício e total `subtotal - descontos ITENS + freteBruto - descontos FRETE`, sem descontar frete duas vezes. Um novo handler, estratégia ou condição de teste deve funcionar com a mesma cadeia e os mesmos consumidores.

### 6. Seleção de cupom e recuperação do carrinho

`Carrinho` guarda código normalizado opcional e incrementa revisão ao aplicar/substituir/remover, inclusive quando não houver alteração de itens. `PUT /api/clientes/me/carrinho/cupom` recebe código; `DELETE` remove e é idempotente. Aplicar exige carrinho não vazio e validação corrente antes de persistir. Substituição rejeitada lança `CupomRejeitadoException` derivada de `ConflitoException` com motivo INEXISTENTE, INATIVO, FORA_DA_VIGENCIA, ESGOTADO, LIMITE_CLIENTE ou INCOMPATIVEL, sem perder a seleção anterior. Corpo malformado responde 400.

Leituras e manutenção de itens não são bloqueadas por um cupom que perdeu validade: `ResumoCarrinho` acrescenta código e problema do cupom, totalizando sem seu benefício inválido e sem desassociá-lo silenciosamente. A mesma situação aparece na prévia do checkout; conclusão fica bloqueada com motivo e prévia atualizada até remoção/substituição. Ausência de benefício monetário (por exemplo frete zero ou itens já totalmente descontados) não é falha de validade, mas não gera desconto positivo nem consome uso. A limpeza após sucesso remove itens e código. Campos opcionais mantêm compatibilidade com consumidores sem cupom.

### 7. Consumo, rollback e devolução

`ConfirmarUsoCupom` recebe cupom/id de configuração revalidada, pedidoId e clienteId obtidos no servidor; não é exposto como rota. Participa da mesma transação do checkout: revalidar/comparar → criar pedido → reservar estoque em ordem existente → consumir cupom com benefício efetivo → confirmar reserva do pedido → limpar carrinho. Uso é gravado antes de PedidoCriado, para ouvintes transacionais verem a associação. Sem benefício efetivo não existe uso consumido.

O adaptador de uso bloqueia a linha de configuração do cupom (`FOR UPDATE`), relê a versão/configuração corrente e confere validade, ativo, saldo global e contagem de usos ativos daquele cliente. Alteração de configuração desde a revalidação causa 409 e rollback; não consumir usando configuração antiga. A mesma trava é usada por alterações administrativas que afetam limites. Atualizar `saldo_cupom` e inserir `uso_cupom` com unicidade de pedido na mesma transação. Separar saldo da entidade versionada evita perder contadores em uma edição administrativa e evita tratar qualquer uso concorrente como edição de campanha. O bloqueio global do cupom protege também a verificação do limite individual.

`OuvinteDePedidosDoCupom` reage sincronicamente a `PedidoPagamentoRecusado` e `PedidoCancelado`. Troca CONSUMIDO→DEVOLVIDO condicionalmente, diminuindo o saldo somente quando a troca ocorre. O registro histórico permanece; contar apenas usos CONSUMIDO para o limite individual. Não criar dependência de pedido para cupom. A ordem de aquisição é estoque antes de cupom em checkout e em ouvintes de recusa/cancelamento: declarar ordem explícita nos ouvintes existentes/novos e testar, evitando ciclo de locks. Novos usos, devoluções e edições do cupom usam a mesma trava de cupom.

Alternativas descartadas: contador em memória não resiste ao reinício; leitura `usos < limite` sem trava/atualização protegida permite exceder limite; consumir no carrinho prende vagas sem compra; decrementar sem registro de uso duplicaria devoluções. Qualquer falha depois do consumo, inclusive versão do carrinho, reverte saldo, uso, pedido e reserva. Repetição de evento e cancelamento após recusa não devolvem duas vezes.

### 8. Confirmação e compatibilidade

Acrescentar código/situação do cupom e identidade/origem de cada desconto em `ConfirmacaoCheckout`. Ordenação canônica considera origem, id, código, nome, alvo e valor; `ConferirConfirmacao` compara esses dados além dos já existentes. Trocar cupom/campanha pelo mesmo valor não pode passar despercebido. Campos opcionais novos em solicitações antigas sem desconto devem continuar aceitos; confirmação antiga sem identidades não autoriza desconto real recém-adicionado e deve provocar 409 com prévia atualizada.

Configuração expirada/inativada após prévia gera recálculo e reconfirmação; um cupom inválido bloqueia conclusão mesmo se o comprador reenviar o resumo sem seu benefício. O snapshot é sempre montado do resultado revalidado. É proibido manter `List.of()` de descontos e `Dinheiro.zero()` de total de descontos no novo caminho de compra com benefícios.

### 9. Design Patterns

| Pattern | Papel | Problema do enunciado | Classes participantes propostas | Modificação facilitada |
|---|---|---|---|---|
| Strategy | Estratégias, contexto e política | RF-27/28/31: tipos distintos e conflitos variáveis | `Promocao`, cinco estratégias, `MotorDePromocoes`, `PoliticaPadrao`, `PoliticaSomenteMaiorDesconto`, `BeneficioCupom` e benefícios concretos | Registrar um novo benefício ou trocar a política sem editar estratégias existentes |
| Composite/Specification | Componente, folhas e compostos | RF-29/30: condições simultâneas sem método gigante | `CondicaoElegibilidade`, folhas, `CondicaoE`, `CondicaoOu`, `CondicaoNao` e registro de criadores | Combinar/reutilizar condições e acrescentar folha |
| Factory Method com registro | Criadores e produto | RF-32: materializar tipo configurado | `PromocaoFactory`, fábricas concretas, `PromocaoFactoryRegistry`, `PromocaoConfig`, `Promocao` | Novo tipo por nova estratégia e fábrica registrada |
| Chain of Responsibility | Handlers na cadeia existente | RF-36/37: formação de preço em etapas | `EtapaPromocoes`, `EtapaCupom`, `EtapaFreteBruto`, `PipelinePreco`, `CalculoPreco` | Adicionar etapas sem reescrever subtotal/total |
| Observer | Ouvinte de eventos existentes | RN-07/26: devolver uso por cancelamento/recusa | `OuvinteDePedidosDoCupom`, `ConfirmarUsoCupom`, serviço de devolução e eventos existentes | Acrescentar reação sem modificar o domínio do pedido |
| Facade (existente) | Coordenação transacional | Checkout consistente entre módulos | `CheckoutFacade`, consumo de cupom e subsistemas existentes | Integrar mais um recurso mantendo atomicidade e controller simples |

Alternativas descartadas: switch por tipo no motor/CRUD para Strategy/Factory recriaria o ponto central de alteração; flags opcionais numa condição gigante não modelam E/OU/NÃO; método único de preço substituiria indevidamente a cadeia existente; liberar cupom dentro de `Pedido` violaria a direção de dependências do Observer; consumo separado do checkout quebraria a Facade e o rollback. CRUD, normalização, JPA, paginação, contadores e DTOs não recebem patterns próprios.

ArchUnit deve liberar somente implementações de `Promocao`, `PoliticaConflito` e `CondicaoElegibilidade` em `promocao.dominio`, mantendo restrições dos contratos de pagamento/frete/notificação e das camadas. As novas portas têm adaptadores em infra. Atualizar Javadoc e docs/04 com classes realmente entregues, sem contar registros genéricos como novos patterns.

## Risks / Trade-offs

- Mais parâmetros no contexto provisório → construtores compatíveis, dados obtidos no servidor e documentação de cada consumidor em docs/03.
- Alteração concorrente de campanha/cupom → versões, revalidação comercial, bloqueio/releitura no consumo e rollback da tentativa perdedora.
- Uma trava por cupom limita paralelismo → adequado ao projeto acadêmico e mais simples de verificar; testes com várias transações independentes comprovam ambos os limites.
- Frete zero pode dar benefício zero → não fabricar desconto/uso; testar frete positivo somente como entrada simulada e manter a integração de entrega na E7.
- Limites devolvidos podem reabrir vaga após cancelamento pago → comportamento da RN-07/26, com registro histórico preservado e devolução idempotente.
- Primeira compra pode ser pré-avaliada em mais de um pedido ainda não pago → consequência explícita do critério por pagamento aprovado; reavaliar a cada conclusão, sem supor desconto vitalício ou bloqueio adicional não solicitado.

## Migration Plan

Adicionar V5 sem editar V1–V4; conferir esquema do zero e atualização de uma base V4 com carrinho/pedidos existentes. Novos campos são opcionais e usos históricos não são inventados. Adaptar a limpeza das tabelas de teste à nova ordem de dependências. Demonstração `dev` deve ser idempotente, com campanhas/cupons identificáveis e Clock/validades controlados nos testes, sem semear fora de dev.

Validar cenário D4 A–D, arredondamento e tetos, primeiro pagamento histórico, recuperação de cupom inválido, consumo/devolução e falhas após flush; executar `./mvnw -o verify`, ArchUnit e OpenSpec estrito. Rollback de aplicativo preserva tabelas/dados: não prometer que a versão antiga, sem entender descontos/cupom, continua apta a vender contra a configuração nova. Não apagar dados automaticamente. Entregar para revisão mantendo atribuições, revisão conjunta da onda 0 pendente e ausência de commit/arquivamento automático.
