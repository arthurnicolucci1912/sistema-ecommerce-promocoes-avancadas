# 09 - Testes automatizados

- **Objetivo:** mapear os cenários de teste exigidos pelo enunciado (e os necessários para validar as regras de negócio) para a etapa do roadmap em que serão implementados e para o tipo de teste (unitário ou integração).
- **Etapas do roadmap relacionadas:** todas; cada cenário indica sua etapa (ver [08-roadmap.md](08-roadmap.md)).
- **Status:** implementado (E1–E9): os cenários da tabela foram cobertos pelas changes arquivadas; `./mvnw -o clean verify` em 2026-10-23: BUILD SUCCESS, 649 testes, zero falhas/erros/ignorados.

Documentos relacionados: [05-regras-negocio.md](05-regras-negocio.md) (regras testadas), [06-ciclo-vida-pedido.md](06-ciclo-vida-pedido.md) (transições), [07-casos-de-uso.md](07-casos-de-uso.md) (fluxos).

## 1. Diretrizes

- **Foco em comportamento, não em cobertura.** O enunciado pede testar comportamentos relevantes, e não atingir uma porcentagem elevada de cobertura. Nenhuma meta numérica de cobertura é adotada. Cada teste deve descrever uma regra de negócio observável.
- **JUnit 5** (com AssertJ e Mockito) como tecnologia de teste; testes de integração com Spring Boot Test.
- **Unitário:** testa uma classe ou pequeno grupo de classes de domínio sem Spring nem banco (promoção, condição, política de conflito, estado do pedido, cálculo de preço).
- **Integração:** sobe o contexto Spring e usa um banco PostgreSQL real para validar persistência, transações, restrições de unicidade, concorrência e a API HTTP com segurança. **Decidido na E1: Testcontainers com PostgreSQL 17** (contêiner descartável, mesma versão do `docker-compose.yml`, esquema criado pelas migrações Flyway; exige Docker em execução, ver [10-execucao.md](10-execucao.md)). Os testes usam a anotação `@TesteDeIntegracao` e o perfil `test`; não há banco em memória.
- **Integrações externas simuladas:** gateways de pagamento, provedores de frete e canais de notificação são substituídos por mocks, stubs ou implementações simuladas. Nenhum teste depende de serviço externo real nem de dados reais de cartão.
- **Relógio controlável:** validade de promoções e cupons usa um `Clock` injetável para testar vigência e expiração de forma determinística.
- **Testes de arquitetura (ArchUnit)**, `implementado` na E1: `arquitetura.RegrasDeArquiteturaTests` verifica que `dominio` não depende de `web`, `infra`, `aplicacao` nem de `org.springframework.web`, `stereotype` e `context`; que `web` não depende de `infra`; e que os contratos da onda 0 são interfaces, com implementações em `infra` e handlers de preço também em `preco.dominio` desde a E3. Na E4 acrescentou `compartilhadoNaoDependeDeModulosDeNegocio` e na E2 as regras entre `estoque`, `catalogo` e `pedido` (seção 7).
- **Nomes de teste** descrevem o cenário em português (por exemplo, `rejeitaCupomExpirado`), de modo que a lista de testes funcione como documentação.
- Cada change de etapa implementa os cenários marcados para sua etapa e mantém verdes os das anteriores (o sistema permanece executável).

## 2. Cenários

Legenda de tipo: U = unitário, I = integração.

### 2.1 Tipos de promoção (um conjunto de testes por tipo)

Os quatro primeiros são o mínimo exigido; os tipos exatos serão fixados na etapa 5 (os listados são a proposta, de naturezas distintas: quantidade, catálogo, valor do carrinho, perfil/histórico do cliente e frete).

| ID | Cenário | Resultado esperado | Etapa | Tipo |
|---|---|---|---|---|
| T-PR-01 | Leve 3 pague 2: 3 unidades do mesmo produto | Desconto igual ao preço de 1 unidade; com 6 unidades, de 2 unidades; com 2 unidades, sem desconto | 5 | U |
| T-PR-02 | Percentual por categoria (20%): carrinho com itens da categoria e de outra categoria | Desconto de 20% apenas sobre os itens da categoria (incluindo subcategorias, se definido) | 5 | U |
| T-PR-03 | Valor fixo acima de mínimo (R$ 50 acima de R$ 300): subtotal de R$ 300,00 e de R$ 299,99 | Desconto de R$ 50 no primeiro caso (limite inclusivo ou exclusivo conforme a regra documentada) e nenhum no segundo | 5 | U |
| T-PR-04 | 10% na primeira compra: cliente sem pedidos anteriores e cliente com pedido anterior | Desconto só para o cliente sem pedidos | 5 | U, I |
| T-PR-05 | Frete grátis acima de valor: subtotal acima e abaixo do limite | Frete zerado, listado como desconto da promoção, somente acima do limite | 5, 7 | U |
| T-PR-06 | Promoção fora da vigência (antes do início e depois do fim) | Não é aplicada; pedidos já criados não mudam | 5 | U |
| T-PR-07 | Condições compostas (E/OU/NÃO): primeira compra E valor mínimo; categoria OU produto; NÃO região | Elegibilidade conforme a combinação; condição inválida em qualquer ramo E reprova | 5 | U |
| T-PR-08 | Extensibilidade: registrar uma nova promoção de teste (quinta ou sexta) sem alterar classes existentes | Promoção nova é aplicada pelo mecanismo existente | 5 | U |
| T-PR-09 | Promoção criada pela API administrativa a partir da configuração (fábrica/registro) | Tipo correto instanciado; tipo desconhecido ou parâmetro inválido (percentual fora de 0 a 100, período invertido) é rejeitado | 5 | I |
| T-PR-10 | Promoção aplicada no carrinho e no checkout | O resumo discrimina subtotal, cada desconto (por nome), frete e total; o pedido registra os mesmos valores | 5 | I |

### 2.2 Múltiplas promoções e conflito

Política de conflito (D4): filtrar elegíveis; havendo exclusiva, aplica só a exclusiva de maior desconto; senão, todas as acumuláveis por prioridade mais a melhor não acumulável; empate por maior prioridade e depois a mais antiga. Ver [05-regras-negocio.md](05-regras-negocio.md).

| ID | Cenário | Resultado esperado | Etapa | Tipo |
|---|---|---|---|---|
| T-CF-01 | Duas promoções acumuláveis elegíveis | Ambas aplicadas, em ordem de prioridade; total de descontos é a soma | 5 | U |
| T-CF-02 | Uma exclusiva e várias acumuláveis elegíveis | Apenas a exclusiva é aplicada | 5 | U |
| T-CF-03 | Duas exclusivas elegíveis com descontos diferentes | Aplica só a de maior desconto | 5 | U |
| T-CF-04 | Duas não acumuláveis elegíveis, sem exclusiva | Aplica só a de maior desconto, mais as acumuláveis | 5 | U |
| T-CF-05 | Empate de desconto entre não acumuláveis | Vence a de maior prioridade; persistindo o empate, a mais antiga | 5 | U |
| T-CF-06 | Troca da política de conflito (outra implementação de Strategy) | O restante do pipeline funciona sem alteração e o resultado muda conforme a política | 5 | U |
| T-CF-07 | Várias promoções simultâneas no checkout completo | Pedido gravado com todos os descontos aplicados e total correto, sem desconto negativo (total nunca abaixo de zero) | 5 | I |
| T-CF-08 | Promoção de frete grátis combinada com promoção de produto | Ambas aplicadas conforme a política, com cada desconto discriminado | 5, 7 | U, I |

### 2.3 Estoque

| ID | Cenário | Resultado esperado | Etapa | Tipo |
|---|---|---|---|---|
| T-ES-01 | Reservar quantidade menor ou igual ao disponível | Disponível diminui e reservado aumenta na mesma quantidade | 2 (`implementado`) | U, I |
| T-ES-02 | Estoque insuficiente na reserva | Reserva rejeitada, estoque inalterado | 2 (`implementado`) | U, I |
| T-ES-03 | Checkout com estoque insuficiente em um item de vários | Checkout rejeitado com indicação do item; nenhuma reserva parcial (tudo-ou-nada); nenhum pedido criado | 4 | I |
| T-ES-04 | Estoque some entre a inclusão no carrinho e o checkout | O carrinho aceita; o checkout rejeita (a verificação no carrinho não garante disponibilidade) | 4 | I |
| T-ES-05 | **Concorrência na última unidade:** N requisições simultâneas (por exemplo 10 threads) para reservar 1 unidade restante | Exatamente 1 sucesso e N-1 falhas por estoque insuficiente; disponível nunca negativo | 2 (mecanismo, `implementado`), 4 (via checkout) | I |
| T-ES-06 | Liberar reserva por cancelamento, pagamento recusado ou expiração | Disponível restaurado, reservado reduzido; liberar duas vezes não restaura duas vezes | 2 (ouvintes, `implementado`: recusa e cancelamento), 6 (expiração) | U, I |
| T-ES-07 | Pagamento aprovado converte reserva em baixa | Reservado reduzido e baixa registrada, sem alterar o disponível uma segunda vez | 2 (ouvinte de `PedidoPago`, `implementado`), 6 (pelo gateway) | I |
| T-ES-08 | Ajuste administrativo deixaria disponível negativo | Ajuste rejeitado (409) e saldo inalterado. O "abaixo do reservado" do rascunho não se aplica: `disponivel` já exclui o reservado | 2 (`implementado`) | U, I |

### 2.4 Cupons inválidos

| ID | Cenário | Resultado esperado | Etapa | Tipo |
|---|---|---|---|---|
| T-CU-01 | Cupom inexistente | Rejeitado com motivo "inexistente" | 5 | U, I |
| T-CU-02 | Cupom expirado (e ainda não vigente) | Rejeitado com motivo "fora da validade" (usa relógio controlável) | 5 | U |
| T-CU-03 | Cupom com limite de utilizações esgotado | Rejeitado; o último uso permitido é aceito e o seguinte rejeitado | 5 | U, I |
| T-CU-04 | Cupom incompatível com as regras da campanha (valor mínimo não atingido, categoria não presente, bloqueado por promoção exclusiva) | Rejeitado com o motivo da incompatibilidade | 5 | U |
| T-CU-05 | Cupom válido percentual e de valor fixo | Desconto correto, discriminado no resumo; valor fixo nunca deixa o total negativo | 5 | U |
| T-CU-06 | Cupom deixa de ser válido entre o carrinho e o checkout | Checkout rejeita ou remove o cupom com aviso, conforme a decisão da etapa 4/5 | 5 | I |
| T-CU-07 | Consumo do cupom no checkout concluído, sob concorrência no último uso | O contador de usos não ultrapassa o limite | 5 | I |

### 2.5 Pagamento

| ID | Cenário | Resultado esperado | Etapa | Tipo |
|---|---|---|---|---|
| T-PG-01 | Pagamento recusado (gateway simulado retorna recusa) | Pedido vai para `PAGAMENTO_RECUSADO`, reserva de estoque liberada, nenhuma baixa | 6 | U, I |
| T-PG-02 | Pagamento aprovado | Pedido vai para `PAGO`, reserva convertida em baixa | 6 | I |
| T-PG-03 | Adaptadores de gateway diferentes (cartão, PIX, boleto) traduzem mensagens distintas para o mesmo modelo interno | Mesmo resultado interno para entradas equivalentes | 6 | U |
| T-PG-04 | Troca de fornecedor: substituir o adaptador por outro (stub) | Regras centrais passam inalteradas | 6 | I |
| T-PG-05 | Cancelamento de pedido já pago | Estorno simulado acionado, reserva/estoque tratados, pedido `CANCELADO` com histórico | 6 | I |
| T-PG-06 | Pagamento ou confirmação para pedido inexistente ou cancelado | Erro registrado ou sinalizado, sem alterar o pedido | 6 | I |

### 2.6 Ciclo de vida do pedido

Matriz de transições em [06-ciclo-vida-pedido.md](06-ciclo-vida-pedido.md).

| ID | Cenário | Resultado esperado | Etapa | Tipo |
|---|---|---|---|---|
| T-PD-01 | Percorrer o caminho feliz: `CRIADO`, `AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE` | Cada transição aceita e registrada no histórico | 4 | U |
| T-PD-02 | **Transição inválida:** exemplos `CRIADO` para `ENVIADO`, `AGUARDANDO_PAGAMENTO` para `ENTREGUE`, `ENTREGUE` para qualquer estado | Lança erro de transição inválida; estado e histórico inalterados | 4 | U, I |
| T-PD-03 | Cancelamento a partir dos estados permitidos (`CRIADO`, `AGUARDANDO_PAGAMENTO`, `PAGAMENTO_RECUSADO`, `PAGO`, `EM_PREPARACAO`) | Aceito, registrado com motivo e responsável | 4 | U |
| T-PD-04 | Cancelamento a partir de `ENVIADO`, `ENTREGUE` ou `CANCELADO` | Rejeitado como transição inválida | 4 | U |
| T-PD-05 | Pedido preserva os dados da compra após alteração de preço do produto | Itens, preços, descontos e frete do pedido permanecem os da data da compra | 4 | I |
| T-PD-06 | Transição inválida via API (por exemplo, enviar pedido não pago) | Resposta de erro apropriada (conflito de estado) e nenhum efeito colateral | 4, 7 | I |

Os cenários T-PD-01 a T-PD-06 estão `implementado` na E4 (classes na seção 6); o cancelamento por perfil (cliente x administrativo) e a disputa concorrente pelo mesmo pedido estão cobertos além da lista base.

### 2.7 Idempotência da confirmação de pagamento

| ID | Cenário | Resultado esperado | Etapa | Tipo |
|---|---|---|---|---|
| T-ID-01 | **Mesma confirmação enviada duas (ou mais) vezes** com o mesmo identificador de transação | Segunda chamada responde sucesso sem efeito: uma única baixa de estoque, uma única transição para `PAGO`, um único registro de transação | 6 | I |
| T-ID-02 | Confirmações repetidas concorrentes (threads simultâneas) | Um único efeito, garantido pela restrição de unicidade do identificador | 6 | I |
| T-ID-03 | Confirmação duplicada não republica evento nem notifica novamente | Um único `PagamentoConfirmado` publicado e uma única notificação | 8 | I |
| T-ID-04 | Falha no meio do processamento (erro após registrar identificador) | A transação é revertida de modo que o identificador não fique registrado sem efeito; nova tentativa processa normalmente | 6 | I |
| T-ID-05 | Confirmação com identificadores diferentes para o mesmo pedido | Segunda confirmação não gera segunda baixa; tratada como conflito conforme a regra da etapa 6 | 6 | I |

### 2.8 Entrega, frete, eventos e segurança (complementares)

| ID | Cenário | Resultado esperado | Etapa | Tipo |
|---|---|---|---|---|
| T-EN-01 | Custo de frete por modalidade, região, peso ou valor | Valor conforme a regra de cada provedor simulado | 7 | U |
| T-EN-02 | Novo provedor de frete registrado (stub) | Aparece nas opções sem alterar o checkout | 7 | I |
| T-EV-01 | `PagamentoConfirmado` entregue a vários listeners (estoque e notificação) | Cada listener reage de forma independente | 8 | I |
| T-EV-02 | Falha no canal de notificação | Pedido, pagamento e baixa de estoque não são afetados; falha registrada | 8 | I |
| T-EV-03 | Operação administrativa (alteração de preço, criação de campanha, cancelamento) | Registro de auditoria com usuário, momento e operação | 8 | I |
| T-SG-01 | Cliente tenta acessar pedido, endereço ou carrinho de outro cliente | Acesso negado ou não encontrado | 1, 4 | I |
| T-SG-02 | Perfil sem permissão tenta operação administrativa (por exemplo, `ESTOQUE` criando promoção) | 403 Proibido | 1, 5 | I |
| T-SG-03 | Senha armazenada | Apenas hash no banco; login funciona com a senha correta e falha com a incorreta | 1 | U, I |
| T-RL-01 | Relatórios (total de vendas, ticket médio, desconto por campanha) sobre pedidos conhecidos | Valores batem com os dados semeados (`implementado`, E9) | 9 | I |

## 3. Resumo de cobertura dos cenários do enunciado

| Cenário exigido pelo enunciado | IDs | Etapa(s) |
|---|---|---|
| Cada tipo de promoção (pelo menos 4) | T-PR-01 a T-PR-05 (e T-PR-06 a T-PR-10 de apoio) | 5 |
| Múltiplas promoções simultâneas e conflito | T-CF-01 a T-CF-08 | 5 (e 7) |
| Estoque insuficiente | T-ES-02 (`implementado`, seção 7), T-ES-03, T-ES-04 | 2, 4 |
| Concorrência na última unidade | T-ES-05 (mecanismo `implementado`, seção 7) | 2, 4 |
| Cupom inválido (inexistente, expirado, limite, incompatível) | T-CU-01 a T-CU-04 | 5 |
| Pagamento recusado | T-PG-01 | 6 |
| Transição inválida de pedido | T-PD-02, T-PD-04, T-PD-06 (`implementado`, seção 6) | 4 |
| Confirmação de pagamento repetida | T-ID-01 a T-ID-05 | 6 (e 8) |

## 4. Mapa dos testes por etapa

| Etapa | Cenários |
|---|---|
| 1 Fundação | T-SG-01 (base), T-SG-02 (base), T-SG-03 (`implementado`, ver a seção 5) |
| 2 Catálogo e estoque | T-ES-01, T-ES-02, T-ES-05 (mecanismo), T-ES-08, T-ES-06 e T-ES-07 (pelos ouvintes), T-CT-01 a T-CT-12 (`implementado` na change `catalogo-estoque`, seção 7) |
| 3 Carrinho | T-CA-01 a T-CA-14: domínio, persistência, recálculo, disponibilidade e API |
| 4 Checkout e pedido | T-ES-03, T-ES-04, T-ES-05 (via checkout), T-ES-06, T-PD-01 a T-PD-06 (`implementado` na change `pedido-ciclo-vida`, seção 6), T-SG-01 (pedido `implementado`) |
| 5 Promoções e cupons | T-PR-01 a T-PR-10, T-CF-01 a T-CF-08, T-CU-01 a T-CU-07, T-SG-02 |
| 6 Pagamento e idempotência | T-PG-01 a T-PG-06, T-ES-06, T-ES-07, T-ID-01, T-ID-02, T-ID-04, T-ID-05 |
| 7 Entrega e frete | T-EN-01, T-EN-02, T-PR-05, T-CF-08, T-PD-06 |
| 8 Eventos, notificações e auditoria | T-ID-03, T-EV-01 a T-EV-03 |
| 9 Relatórios | T-RL-01 (`implementado`) |

Os testes de carrinho (etapa 3) estão detalhados na seção E3 deste documento (T-CA-01 a T-CA-14).

## 5. Cobertura da E1 (`implementado`)

Todos os testes de integração usam Testcontainers (PostgreSQL 17). Pacotes abaixo de `br.com.fatec.ecommerce`.

| ID | Cobertura na E1 | Classes de teste |
|---|---|---|
| T-SG-01 | Cliente não acessa endereço de outro cliente (resposta 404, endereço do outro intacto). Pedido e carrinho só na E3 e E4. | `cliente.web.EnderecosTests` (`acessarAlterarOuRemoverEnderecoDeOutroClienteRetorna404...`); também `cliente.web.CadastroClienteTests` (`/api/clientes/me` usa a identidade do token) |
| T-SG-02 | Perfil sem permissão recebe 403 em Problem Details: `CLIENTE` e `ESTOQUE` em `/api/admin/usuarios`; administrativo sem cliente em `/api/clientes/me`. Promoções (`ESTOQUE` criando promoção) só na E5. | `seguranca.web.UsuariosAdministrativosTests`, `seguranca.web.AutenticacaoTests` (`perfilSemPermissaoRetorna403EmProblemDetail`), `cliente.web.CadastroClienteTests`, `cliente.web.EnderecosTests` |
| T-SG-03 | Apenas hash no banco, login correto funciona e senha incorreta falha. | `seguranca.dominio.UsuarioPersistenciaTests` (`senhaPersistidaEhHashVerificavel`), `seguranca.web.AutenticacaoTests` (`loginBemSucedido`, `senhaIncorretaRetorna401Generico`), `seguranca.dominio.SenhaForteTests` (política de senha) |

Demais testes da E1, para referência:

| Assunto | Classes |
|---|---|
| Esquema e migração `V1` | `infra.MigracaoV1Tests` |
| Erros em Problem Details, OpenAPI e saúde sem token | `compartilhado.web.TratadorGlobalDeErrosTests`, `compartilhado.web.PlataformaApiTests` |
| `Dinheiro` | `compartilhado.dominio.DinheiroTest` |
| Publicação de eventos | `compartilhado.infra.PublicadorEventosSpringTests` |
| Usuários administrativos e regra do último `ADMIN` | `seguranca.web.UsuariosAdministrativosTests`, `seguranca.dominio.RegraUltimoAdminTests` |
| Cadastro e dados do cliente, CPF | `cliente.web.CadastroClienteTests`, `cliente.dominio.CpfTests` |
| Endereços e regra do endereço principal | `cliente.web.EnderecosTests`, `cliente.dominio.RegraEnderecoPrincipalTests` |
| Usuários de demonstração (perfil `dev`) | `infra.demo.UsuariosDeDemonstracaoTests`, `infra.demo.UsuariosDeDemonstracaoForaDoDevTests` |
| Regras de arquitetura | `arquitetura.RegrasDeArquiteturaTests` |

## 6. Cobertura do pedido na E4 (`implementado`)

Change `pedido-ciclo-vida`. Pacotes abaixo de `br.com.fatec.ecommerce`; os testes de integração usam Testcontainers (PostgreSQL 17).

| ID | Cobertura na E4 | Classes de teste |
|---|---|---|
| T-PD-01 | Fluxo completo até `ENTREGUE`, com uma entrada de histórico por transição (mais a de criação), em ordem cronológica e com responsável e motivo. | `pedido.aplicacao.TransicionarPedidoTests` (`fluxoCompletoTerminaEmEntregue...`, `historicoVemEmOrdemCronologica...`); `pedido.dominio.PedidoTests` (`cadaTransicaoRegistraUmaEntradaDeHistorico...`) |
| T-PD-02 | **Transição inválida:** os 56 pares estado x operação (11 válidas levam ao estado certo, 45 lançam `TransicaoInvalidaException` com estado e operação); a rejeição não altera situação, histórico nem eventos; via API responde 409 com `situacaoAtual` e `operacao`. | `pedido.dominio.estado.EstadosDoPedidoTests`; `pedido.dominio.PedidoTests` (`transicaoRejeitadaNaoAlteraSituacaoNemGeraHistoricoOuEvento`); `pedido.aplicacao.TransicionarPedidoTests` (`transicaoInvalidaNaoAlteraHistoricoNemPublicaEvento`); `pedido.web.PedidosAdministrativosTests` (`pularEtapaResponde409...`, `estadosFinaisRejeitamQualquerTransicao`) |
| T-PD-03 | Cancelamento aceito nos 5 estados que admitem, com motivo e responsável; cliente cancela `CRIADO`, `AGUARDANDO_PAGAMENTO` e `PAGAMENTO_RECUSADO`, e o administrativo também `PAGO` e `EM_PREPARACAO`. | `pedido.dominio.estado.EstadosDoPedidoTests` (`clienteCancelaPedidoAindaNaoPago`); `pedido.dominio.PedidoTests` (`administradorCancelaPedidoPago...`); `pedido.web.PedidosAdministrativosTests` (`administradorCancelaPedidoPago...`, `operadorComercialCancelaPedidoEmPreparacao...`); `pedido.web.PedidosDoClienteTests` (`clienteCancelaPedidoAguardandoPagamento...`) |
| T-PD-04 | Cancelamento a partir de `ENVIADO`, `ENTREGUE` ou `CANCELADO` rejeitado como transição inválida (409); cliente em `PAGO` ou `EM_PREPARACAO` também (política de RN-26). | `pedido.dominio.estado.EstadosDoPedidoTests` (`estadosFinaisRejeitamTodasAsOperacoes`, `clienteNaoCancelaPedidoPagoOuEmPreparacao`); `pedido.web.PedidosAdministrativosTests` (`cancelarPedidoEnviadoResponde409...`); `pedido.web.PedidosDoClienteTests` (`clienteNaoCancelaPedidoPagoEORecebe409...`) |
| T-PD-05 | Pedido preserva preço e endereço depois de alterados ou removidos no cadastro; snapshot sem setters e totais inconsistentes rejeitados. | `pedido.aplicacao.CriarPedidoTests` (`precoAlteradoDepoisDaCompra...`, `enderecoAlteradoOuRemovidoDepoisDaCompra...`, `pedidoCriadoEmAguardandoPagamentoPreservaOSnapshot...`); `pedido.dominio.PedidoTests` (`totalInconsistenteRejeitaOSnapshot` e relacionados) |
| T-PD-06 | Transição inválida via API: 409 em Problem Details com `situacaoAtual` e `operacao`, sem alterar o pedido. | `pedido.web.PedidosAdministrativosTests`, `pedido.web.PedidosDoClienteTests` |
| T-SG-01 (pedido) | Cliente não vê nem cancela pedido de outro cliente (404, pedido intacto); sem token 401; cliente na área administrativa 403; perfil sem permissão 403. | `pedido.web.PedidosDoClienteTests` (`pedidoDeOutroClienteResponde404...`), `pedido.web.PedidosAdministrativosTests` (`perfilSemPermissaoRecebe403...`, `clienteNaAreaAdministrativaRecebe403...`) |

Demais cenários do pedido cobertos na E4:

| Assunto | Classes |
|---|---|
| Esquema e restrições da migração `V2` (situação, quantidade > 0, origem e alvo do desconto, histórico com origem nula, versão) | `infra.MigracaoV2Tests` |
| Eventos: cada uma das 11 transições válidas produz o evento esperado; `PedidoCancelado` leva responsável e motivo | `pedido.dominio.evento.EventosDoPedidoTests` |
| Observer: um `@EventListener` registrado só no teste recebe `PedidoCancelado` com `de = PAGO` e nada é publicado em transição rejeitada | `pedido.aplicacao.OuvinteDeCancelamentoTests` |
| Extensibilidade do State: um estado novo implementando `EstadoPedido` funciona sem alterar `Pedido` | `pedido.dominio.PedidoTests` (`estadoNovoImplementandoEstadoPedidoFuncionaSemAlterarOPedido`) |
| Concorrência: lock otimista, envio e cancelamento simultâneos aplicam um e respondem 409 ao outro | `pedido.aplicacao.TransicionarPedidoTests` (`gravacaoComVersaoDesatualizadaViraConflito`, `envioECancelamentoSimultaneos...`) |
| Criação, reserva confirmada e evento `PedidoCriado` | `pedido.aplicacao.CriarPedidoTests` |
| Consultas: listagem própria paginada, do mais recente ao mais antigo; filtro por situação; detalhe com histórico | `pedido.web.PedidosDoClienteTests`, `pedido.web.PedidosAdministrativosTests` |
| Andamento pelo `ESTOQUE` até a entrega e regras de rota por perfil | `pedido.web.PedidosAdministrativosTests` |
| Pagamento simulado (temporário): aprovação, recusa e recusado não reabre o pedido; só `ADMIN` | `pedido.web.PagamentoSimuladoTests` |
| Pedido de demonstração do perfil `dev` criado uma única vez; fora do `dev` nenhum pedido | `infra.demo.UsuariosDeDemonstracaoTests`, `infra.demo.UsuariosDeDemonstracaoForaDoDevTests` |
| Regra de arquitetura nova: `compartilhado` não depende de módulos de negócio | `arquitetura.RegrasDeArquiteturaTests` (`compartilhadoNaoDependeDeModulosDeNegocio`) |

Apoio de teste (não são testes): `pedido.PedidosParaTeste`, `pedido.PedidosPersistidosParaTeste` e `pedido.CapturaDeEventosDoPedido`.

## 7. Cobertura do catálogo e do estoque na E2 (`implementado`)

Change `catalogo-estoque`. Pacotes abaixo de `br.com.fatec.ecommerce`; os testes de integração usam Testcontainers (PostgreSQL 17), e os de domínio do Composite e da árvore de categorias rodam sem Spring nem banco.

### 7.1 Estoque

| ID | Cobertura na E2 | Classes de teste |
|---|---|---|
| T-ES-01 | Reserva bem-sucedida move do disponível para o reservado; componentes repetidos da mesma variação são somados; reservar duas vezes para o mesmo pedido é rejeitado e sem efeito. | `estoque.aplicacao.ReservarEstoqueTests` (`reservaBemSucedidaMoveDoDisponivelParaOReservado`, `componentesRepetidosDaMesmaVariacaoSaoSomados`, `reservarDuasVezesParaOMesmoPedidoEhRejeitadoESemEfeito`) |
| T-ES-02 | Estoque insuficiente em um componente de vários: a exceção informa a variação e o disponível, e **nenhum** saldo é alterado (tudo-ou-nada); variação sem saldo conta como disponível zero; componentes inválidos são rejeitados. | `estoque.aplicacao.ReservarEstoqueTests` (`estoqueInsuficienteInformaOComponenteENaoAlteraNenhumSaldo`, `variacaoSemSaldoEhInsuficienteComDisponivelZero`, `somaDosComponentesRepetidosAcimaDoDisponivelEhInsuficiente`, `componentesInvalidosSaoRejeitados`) |
| T-ES-05 | **Dez pedidos pela última unidade:** 10 threads com barreira de largada; exatamente 1 sucesso e 9 `EstoqueInsuficienteException`; saldo final 0 disponível e 1 reservado; repetido 3 vezes. A via checkout fica para a etapa 4. | `estoque.aplicacao.ReservaConcorrenteTests` (`@RepeatedTest(3)`) |
| T-ES-06 | Liberar duas vezes devolve ao disponível uma única vez; pagamento recusado e cancelamento antes do pagamento liberam a reserva (pelo ouvinte, com os serviços reais do pedido). A expiração é da etapa 6. | `estoque.aplicacao.MovimentarReservasTests` (`liberarDuasVezesDevolveAoDisponivelUmaUnicaVez`), `estoque.aplicacao.OuvintesDoEstoqueTests` (`cancelamentoAntesDoPagamentoLiberaAReserva`, `pagamentoRecusadoLiberaAReserva`) |
| T-ES-07 | Pagamento aprovado baixa a reserva: o reservado diminui, o disponível não muda, e a baixa repetida não tem efeito. Aqui a aprovação vem do pedido (`TransicionarPedido`); o gateway é da etapa 6. | `estoque.aplicacao.MovimentarReservasTests` (`baixaDiminuiOReservadoSemMudarODisponivelEEhIdempotente`), `estoque.aplicacao.OuvintesDoEstoqueTests` (`pagamentoAprovadoBaixaAReserva`) |
| T-ES-08 | Ajuste que deixaria o disponível negativo responde 409 e o saldo não muda; ajuste zero ou sem motivo é inválido (400); saída até zero é permitida; ajuste de variação sem saldo é 404. | `estoque.aplicacao.AjusteEConsultaDeSaldoTests`, `estoque.web.EstoqueWebTests` (`ajusteQueDeixariaNegativoRespondeConflitoESemMotivoOuZeroRespondem400`) |

Demais cenários do estoque cobertos na E2:

| Assunto | Classes |
|---|---|
| Reposição desfaz uma baixa uma única vez; operações sem reserva aplicável e pedido sem reserva não mudam nada nem lançam erro | `estoque.aplicacao.MovimentarReservasTests` |
| Observer do estoque: variação nova ganha saldo zero; ouvinte de catálogo não altera saldo existente; cancelamento depois do pagamento e em preparação repõe; transição rejeitada não altera o estoque; pedido sem reserva não quebra a transição | `estoque.aplicacao.OuvintesDoEstoqueTests` |
| Consulta de saldo por variação e por várias; estoque baixo lista só os abaixo do limite (estritamente) | `estoque.aplicacao.AjusteEConsultaDeSaldoTests`, `estoque.web.EstoqueWebTests` |
| Rotas e perfis do estoque: `ESTOQUE` e `ADMIN` operam; `OPERADOR_COMERCIAL` recebe 403; visitante, 401; `abaixoDe` ausente, 400 | `estoque.web.EstoqueWebTests` |
| Esquema e restrições da migração `V3` (SKU único, preço maior que zero, tipo, nome de categoria único por pai, saldo não negativo, reserva única por pedido e variação, situação da reserva) | `infra.MigracaoV3Tests` |

### 7.2 Catálogo

| ID | Cenário | Resultado esperado | Classes de teste |
|---|---|---|---|
| T-CT-01 | Composite: componentes de estoque da variação e do combo, inclusive combo dentro de combo | Multiplicação em cadeia e agregação de variações repetidas | `catalogo.dominio.ItemVendavelCompositeTests` |
| T-CT-02 | Composite: disponibilidade para venda | Combo com componente inativo, combo inativo, combo vazio e variação de produto inativo não estão disponíveis | `catalogo.dominio.ItemVendavelCompositeTests` |
| T-CT-03 | Combo circular | Rejeitado (409): combo que contém a si mesmo, direta ou indiretamente; componente repetido | `catalogo.dominio.ItemVendavelCompositeTests`, `catalogo.aplicacao.GerenciarCombosTests` (`comboCircularEhConflitoAoAlterarOsComponentes`) |
| T-CT-04 | **Extensibilidade do Composite** | Uma subclasse nova de `ItemVendavel` (`AssinaturaDeTeste`, só em `src/test`) é tratada por um combo e pelo cálculo de componentes sem nenhuma alteração nas classes existentes | `catalogo.dominio.ItemVendavelCompositeTests` (`novoTipoDeItemVendavelEhTratadoPeloComboSemAlterarClassesExistentes`, `novoTipoTambemParticipaDaAgregacaoComOutrasVariacoes`) |
| T-CT-05 | Categorias em árvore | Ancestrais e descendentes; mover para si mesma ou para uma descendente é rejeitado; nome repetido entre irmãs rejeitado (sem diferenciar caixa) e permitido sob pais diferentes; remover categoria em uso é conflito | `catalogo.dominio.CategoriaTests`, `catalogo.aplicacao.GerenciarCategoriasTests` |
| T-CT-06 | Produto com variações e combos | SKUs distintos e um evento por variação; novo tipo de característica sem configuração prévia; dados inválidos não gravam nada; SKU duplicado (inclusive variação contra combo, e violação no banco) é conflito; inativar produto inativa as variações; combo com preço e componentes alteráveis, inativação do combo não inativa os componentes | `catalogo.aplicacao.GerenciarProdutosTests`, `catalogo.aplicacao.GerenciarCombosTests` |
| T-CT-07 | Consulta de item por SKU (contrato do Composite) | Variação e combo devolvem a mesma visão (preço, categorias ancestrais, componentes de uma unidade); combo dentro de combo agrega; item inativo; `porSkus` preserva a ordem e SKU inexistente é 404 | `catalogo.aplicacao.ConsultarItensVendaveisTests` |
| T-CT-08 | Consulta pública | Filtro por categoria inclui subcategorias (sem token); filtro sem resultado devolve página vazia; filtros por nome e preço; paginação; disponibilidade no detalhe e do combo (mínimo dos componentes); produto ou combo inativo é 404 | `catalogo.aplicacao.ConsultarCatalogoTests`, `catalogo.web.CatalogoWebTests` (`consultaPublicaNaoExigeToken`, `produtoEComboInexistentesRespondem404`) |
| T-CT-09 | Autorização do catálogo | `ESTOQUE` e cliente alteram o catálogo: 403; visitante: 401; a consulta pública segue aberta; `/api/admin/usuarios` continua só do `ADMIN` | `catalogo.aplicacao.AutorizacaoDoCatalogoTests`, `catalogo.web.CatalogoWebTests` |
| T-CT-10 | Observer (publicador): `VariacaoCadastrada` | Publicado com o id da variação gravada, um por variação, e não publicado se o SKU for duplicado | `catalogo.dominio.VariacaoCadastradaTests`, `catalogo.aplicacao.CadastrarVariacaoTests` |
| T-CT-11 | Persistência do Composite | Combo com variações e combo interno gravado e recarregado com os componentes certos; inativar o produto torna o combo indisponível | `catalogo.dominio.CompositePersistenciaTests` |
| T-CT-12 | Demonstração do perfil `dev` | Categorias, produtos, variações, combo e estoque semeados; Teclado com 1 unidade; pedido de exemplo com reserva ativa; executar o runner duas vezes não duplica nem soma estoque; fora do `dev`, nada é semeado | `infra.demo.CatalogoDeDemonstracaoTests`, `infra.demo.UsuariosDeDemonstracaoTests`, `infra.demo.UsuariosDeDemonstracaoForaDoDevTests` |
| Fluxo ponta a ponta | Categoria, produto com variações, combo e consulta pública com disponibilidade, pela API | `catalogo.web.CatalogoWebTests` (`fluxoCategoriaProdutoComVariacoesComboEConsultaPublicaComDisponibilidade`) |

### 7.3 Regras de arquitetura da E2

`arquitetura.RegrasDeArquiteturaTests`: `estoqueNaoDependeDoCatalogoExcetoOEventoDeVariacaoCadastrada` (o `estoque` só pode depender do pacote `catalogo.dominio.evento`), `catalogoNaoDependeDePedido` e `pedidoNaoDependeDeEstoqueNemDeCatalogo`. As regras passam (a task 5.2 da change registra também a confirmação de que falham com uma violação temporária, removida em seguida). Detalhes em [03-arquitetura.md](03-arquitetura.md), seção 7.

Apoio de teste (não são testes): `catalogo.CatalogoParaTeste`, `catalogo.CatalogoPersistidoParaTeste`, `catalogo.CapturaDeEventosDoCatalogo` e `estoque.EstoqueParaTeste`.

## E3 — Carrinho: domínio e persistência implementados

| ID | Comportamento verificado | Teste |
|---|---|---|
| T-CA-01 | Inclusão acumulativa, alteração, remoção, ausência e proteção da coleção | `CarrinhoTests` |
| T-CA-02 | Quantidade inválida, SKU vazio e estouro sem alteração parcial | `CarrinhoTests` |
| T-CA-03 | Migração V4, unicidade, FK, quantidade positiva e leitura entre sessões | `CarrinhoPersistenciaTests` |
| T-CA-04 | Duas gravações concorrentes de quantidades: uma vence, outra recebe conflito | `CarrinhoPersistenciaTests` |

| T-CA-05 | Subtotal/total, precisão, vazio e independência entre cálculos | `PipelinePrecoTests` |
| T-CA-06 | Handler novo de frete entre subtotal e total, sem alteração dos existentes | `PipelinePrecoTests` |

A regra ArchUnit da E3 permite implementações de `EtapaPreco` em `preco.dominio`, preservando as restrições dos demais contratos da onda 0.

### E3 — Aplicação, disponibilidade e API implementadas

| ID | Comportamento verificado | Teste |
|---|---|---|
| T-CA-07 | Novo tipo de item vendável passa por catálogo, carrinho, preço e disponibilidade sem mudar consumidores | `CarrinhoAplicacaoTests` |
| T-CA-08 | Referência ausente permanece visível, identificada e removível | `CarrinhoAplicacaoTests` |
| T-CA-09 | Fluxo HTTP: vazio, inclusão acumulativa, PUT, DELETE e recálculo com preço atualizado | `CarrinhoWebTests` |
| T-CA-10 | Kit aninhado com preço próprio; componentes compartilhados somados; saldos/reservas intactos | `CarrinhoWebTests` |
| T-CA-11 | Falta de estoque e saldo ausente geram aviso sem mudar seleção | `CarrinhoWebTests` |
| T-CA-12 | Inteiro positivo, fracionário rejeitado, int excedido, SKU ausente/inativo e rollback após falha no recálculo | `CarrinhoWebTests` |
| T-CA-13 | 401/403, dois clientes isolados e proteção também na aplicação (T-SG-01, E3) | `CarrinhoWebTests` |
| T-CA-14 | OpenAPI expõe as rotas e quantidade inteira | `CarrinhoWebTests` |

Os mesmos verbos, corpos e respostas dos exemplos de docs/10-execucao.md são exercitados pelos testes web, com catálogo de teste determinístico e tokens reais.

### E4b — Checkout

| ID | Comportamento verificado | Teste |
|---|---|---|
| T-CH-01 | Descrição polimórfica ordenada, combo sem descrição artificial, SKU ausente/inativo bloqueado | `RevalidarCheckoutTests` |
| T-CH-02 | Prévia com versão, endereço próprio e valores atuais; carrinho vazio rejeitado; leitura sem escrita | `RevalidarCheckoutTests`, `CheckoutIntegracaoTests` |
| T-CH-03 | Kit aninhado com preço próprio, componentes compartilhados e saldo ausente | `RevalidarCheckoutTests`, `CheckoutIntegracaoTests.kitAninhadoESaldoAusenteNaPreviaSemEscrita` |
| T-CH-04 | Multiplicação/soma acima da capacidade de reserva rejeitadas sem efeitos | `RevalidarCheckoutTests` |
| T-CH-05 | Novo item vendável e nova etapa do pipeline usados sem modificar checkout | `RevalidarCheckoutTests.novoTipoDoCompositeENovaEtapaDoPipelineFuncionamSemMudarCheckout` |
| T-CH-06 | Ordem/escala equivalentes aceitas; preços compensados, versão/seleção e destino alterados exigem confirmação | `ConfirmacaoCheckoutTests` |
| T-CH-07 | 409 com divergências e prévia atualizada; preço recebido nunca determina o pedido; reconfirmação | `CheckoutIntegracaoTests.fluxoKitPrecoAlteradoReconfirmacaoSnapshotECancelamento`, `precoManipuladoQuantidadeEEnderecoAlteradosNaoConcluem` |
| T-CH-08 | 201 com Location, AGUARDANDO_PAGAMENTO, snapshot imutável, reserva agregada e carrinho vazio | `CheckoutIntegracaoTests.fluxoKitPrecoAlteradoReconfirmacaoSnapshotECancelamento` |
| T-CH-09 | Falha por insuficiência parcial desfaz pedido/histórico/reservas/saldos e preserva carrinho | `CheckoutIntegracaoTests.insuficienciaPosteriorDesfazReservaAnteriorEPedidoInteiro` |
| T-CH-10 | Falha após confirmação persistida e após flush da limpeza faz rollback completo | `CheckoutIntegracaoTests.falhaDepoisDaConfirmacaoPersistidaDesfazTudo`, `falhaDepoisDoFlushDaLimpezaDesfazPedidoReservaESelecao` |
| T-CH-11 | Dois clientes pela última unidade: um 201 e um 409; carrinho perdedor preservado | `CheckoutIntegracaoTests.doisClientesPelaUltimaUnidadeSoConfirmamUmPedido` |
| T-CH-12 | Mesmo carrinho simultâneo com estoque suficiente gera um único pedido/reserva | `CheckoutIntegracaoTests.duasConclusoesDaMesmaVersaoNaoDuplicamCompraMesmoComEstoqueSuficiente` |
| T-CH-13 | Inclusão concorrente vencedora não é apagada; checkout perdedor reverte sua reserva e pedido | `CheckoutIntegracaoTests.inclusaoConcorrenteVencedoraNaoEhApagadaPeloCheckout`, `checkoutVencedorFazInclusaoDaVersaoAntigaResponder409SemPerderNovaCompra` |
| T-CH-14 | Cancelamento/recusa liberam e aprovação baixa a reserva uma vez; novo ouvinte recebe PedidoCriado | `CheckoutIntegracaoTests.fluxoKitPrecoAlteradoReconfirmacaoSnapshotECancelamento`, `recusaEAprovacaoSimuladasReagemSobreReservaReal` |
| T-CH-15 | 401/403 em ambas as rotas, proteção na fachada, isolamento entre clientes e endereço inativo/alheio 404 | `CheckoutIntegracaoTests.autenticaPerfisEIsolaEnderecoECarrinho`, `fachadaProtegeAplicacaoTambem` |
| T-CH-16 | Confirmação ausente, quantidade fracionária/inválida, ids inválidos: 400 em Problem Details | `CheckoutIntegracaoTests.corposInvalidosECarrinhoVazioSaoErrosPadronizados` |
| T-CH-17 | OpenAPI documenta prévia, confirmação, pedido, 201 e conflitos | `CheckoutIntegracaoTests.documentaContratoDePreviaConfirmacaoPedidoEErros` |

`CheckoutIntegracaoTests` usa PostgreSQL/Testcontainers, tokens reais, threads e transações independentes nas disputas, e falhas controladas depois de escritas reais com flush. As regressões incluem catálogo, carrinho e ArchUnit; frete, promoções e gateway foram cobertos depois, nas E5 a E7. Comando: `./mvnw -o -Dtest=RevalidarCheckoutTests,ConfirmacaoCheckoutTests,CheckoutIntegracaoTests test`.

Evidência E4b em 2026-10-22: `./mvnw -o verify` — 507 testes (27 novos de checkout), zero falhas, erros ou ignorados, BUILD SUCCESS e JAR gerado; `openspec validate checkout --strict` aprovado.

### E5 — Promoções e cupons

| ID | Comportamento verificado | Teste |
|---|---|---|
| T-PR-01 | Contratos legados de contexto/candidato/desconto e coleções copiadas | `PromocoesDominioTests.contratosLegadosSaoCompativeisEContextoCopiaColecoes` |
| T-PR-02 | E/OU/NÃO, folhas, limites de árvore e tipos/parâmetros inválidos | `PromocoesDominioTests.tabelaVerdadeDosCompostos`, `folhasEArvoreDeclarativaUsamDadosDoContexto`, `invalidaTiposFilhosProfundidadeTamanhoEParametros` |
| T-PR-03 | Leve X/pague Y, SKU comercial de kit, vigência [início,fim) e HALF_EVEN | `PromocoesDominioTests.leveXPagueYArredondamentoVigenciaEKitsPeloSkuComercial` |
| T-PR-04 | D4 A–D, prioridade/criação, teto por alvo e política alternativa | `PromocoesDominioTests.politicaD4CenariosADeterminismoETetos` |
| T-PR-05 | 400−110−29=261, ordem de cálculo, frete simulado e handler novo sem estado compartilhado | `PromocoesDominioTests.cadeiaDesconta400Menos110Menos29ESeparaFreteSemEstadoCompartilhado` |
| T-PR-06 | Sexto tipo e nova folha registrados e cadastrados por HTTP sem alterar consumidores | `PromocoesCuponsIntegracaoTests.cadastroNormalizacaoAutorizacaoTiposEExtensaoPorRegistro` |
| T-PR-07 | Histórico real de primeira compra: cancelado sem pagar elegível, pago depois cancelado inelegível; flags no corpo ignoradas | `PromocoesCuponsIntegracaoTests.primeiraCompraFavoreceTentativasNaoPagasMasHistoricoPagoCanceladoConta`, `PromocoesDominioTests.primeiraCompraDoServidorNaoConfiaEmCriacaoDePedidoEUsaInstanteUnico` |
| T-PR-08 | Região do endereço principal no carrinho e escolhido na prévia | `PromocoesCuponsIntegracaoTests.descontoPorRegiaoUsaPrincipalNoCarrinhoEEscolhidoNaPrevia` |
| T-CP-01 | Percentual após promoções, teto de fixo/frete, benefício zero e novo benefício registrado | `PromocoesDominioTests.beneficiosDeCupomTetoFreteZeroEExtensao`, `cadeiaDesconta400Menos110Menos29ESeparaFreteSemEstadoCompartilhado` |
| T-CP-02 | Cupom normalizado e persistido, substituição rejeitada preserva anterior, leitura sem consumo e isolamento | `PromocoesCuponsIntegracaoTests.cupomPersistidoSubstituicaoRejeitadaEExpiracaoRecuperavel` |
| T-CP-03 | Motivos de inexistência/inatividade/validade/limite/incompatibilidade; resumo recuperável e checkout bloqueado | `PromocoesCuponsIntegracaoTests.cupomPersistidoSubstituicaoRejeitadaEExpiracaoRecuperavel`, `cupomInativoLimiteIndividualIncompatibilidadeEQuantidadeRecalculada` |
| T-CP-04 | Pedido com origens/referências, limpeza de código, recusa/cancelamento devolvem uma vez e snapshot não muda | `PromocoesCuponsIntegracaoTests.checkoutDiscriminaOrigensPreservaSnapshotEDevolveNaRecusaECancelamento` |
| T-CP-05 | Cupom distinto com mesmo valor e campanha alterada exigem reconfirmação | `PromocoesCuponsIntegracaoTests.alteracaoDeCampanhaECupomComMesmoValorExigeNovaConfirmacao` |
| T-CP-06 | Rollback depois do consumo e depois do flush da limpeza, incluindo saldo/uso/pedido/reserva/seleção | `PromocoesCuponsIntegracaoTests.falhaDepoisDoConsumoFazRollbackInclusiveDasReservasESelecao`, `falhaDepoisDoFlushDaLimpezaReverteTambemUsoDoCupom` |
| T-CP-07 | Última utilização global entre clientes, com preservação de recursos da perdedora | `PromocoesCuponsIntegracaoTests.ultimaUtilizacaoSobConcorrenciaNaoCriaPedidoNemReservaNaPerdedora` |
| T-CP-08 | Limite individual concorrente, consumo repetido e versão alterada | `PromocoesCuponsIntegracaoTests.limiteIndividualConcorrenteConsumoDuplicadoEDesativacaoConcorrente` |
| T-CP-09 | Duas conclusões da mesma seleção com cupom consomem uma vez | `PromocoesCuponsIntegracaoTests.duasConclusoesComCupomDaMesmaSelecaoConsomemUmaVez` |
| T-CP-10 | Desativação administrativa durante compra é detectada com rollback | `PromocoesCuponsIntegracaoTests.configuracaoAlteradaDuranteCompraNaoEhConsumidaComVersaoAntiga` |
| T-CP-11 | Devolução e novo consumo concorrentes respeitam contadores sem espera circular | `PromocoesCuponsIntegracaoTests.devolucaoENovoConsumoConcorrentesNaoGeramSaldoNegativoNemEsperaCircular` |
| T-CP-12 | Edição versionada não apaga usos nem reduz limites abaixo do uso ativo | `PromocoesCuponsIntegracaoTests.edicaoVersionadaEContagemAtivaNaoPodemSerApagadas` |
| T-CP-13 | ADMIN/PROMOCOES, 401/403 e proteção na aplicação, tipos/OpenAPI, código duplicado e entradas inválidas | `PromocoesCuponsIntegracaoTests.cadastroNormalizacaoAutorizacaoTiposEExtensaoPorRegistro`, `aplicacaoTambemExigeResponsavelPorPromocoes` |
| T-CP-14 | Migração V4→V5 preserva seleção e desconto histórico com novas referências nulas | `MigracaoPromocionalTests.upgradeDaV4PreservaCarrinhoPedidoEDescontosAnteriores` |
| T-CP-15 | Carga dev repetida não duplica nem restaura contadores; campanha automática inicia inativa | `PromocoesDemonstracaoTests.repeticaoDaCargaNaoDuplicaNemRestauraUtilizacoes` |

Os testes E5 usam regras puras e PostgreSQL/Testcontainers. Concorrência usa threads e transações independentes; falhas controladas ocorrem depois de escritas reais. A regressão mantém os testes do checkout/carrinho sem campanhas, a confirmação antiga sem benefícios e ArchUnit, incluindo a limitação das implementações de promoções ao domínio próprio/infra. A ausência da carga fora de dev é garantida pelo perfil e pela carga inexistente nos contextos de integração sem dev; nenhuma campanha é criada automaticamente nos testes normais.

T-CP-16: `PromocoesCuponsIntegracaoTests.beneficioZeroNaoConsomeEFaltaDeEstoqueNaoGastaCupom` verifica que frete zero não consome utilização e que falta de estoque não gasta cupom. O teste de rollback após consumo também é parametrizado para falha depois da confirmação persistida do pedido. A carga fora de dev é verificada pela ausência do bean no contexto normal. O código com 40 caracteres e espaços externos é normalizado antes da validação de comprimento.

Evidência E5 em 2026-10-22: `./mvnw -o verify` — BUILD SUCCESS, 540 testes, zero falhas, erros ou ignorados; JAR gerado. São 32 casos novos nas classes E5 (`PromocoesDominioTests`: 12, `PromocoesCuponsIntegracaoTests`: 18, `MigracaoPromocionalTests`: 1, `PromocoesDemonstracaoTests`: 1), além da regra ArchUnit adicional. `openspec validate promocoes-cupons --strict` aprovado.


## Pagamento e idempotência — E6

| Cobertura | Evidência |
|---|---|
| Invariantes, confirmação terminal, valor zero e recuperação de estorno | `PagamentoDominioTests` |
| Tradução de três APIs, resultado síncrono/pendente e extensão por novo fornecedor | `PagamentoDominioTests.adaptersTraduzemTresApisEUsamChavesEstaveis`, `novoFornecedorRegistradoSubstituiCartaoEPreservaOriginal` |
| Unicidade por pedido/transação, inserção idempotente sem rollback-only | `PagamentoPersistenciaTests` |
| Upgrade V5→V6 preserva pedido/desconto antigo, sem fabricar cobranças | `MigracaoPagamentoTests` |
| JWT, propriedade, respostas inválidas, cartão/PIX/boleto e histórico SISTEMA | `PagamentoIntegracaoTests` |
| Inícios, confirmações, cancelamentos e retentativas concorrentes | `PagamentoIntegracaoTests` (threads e transações reais com timeout) |
| Rollback após falha do estoque ou observador e reenvio | `PagamentoIntegracaoTests.falhaNoEstoqueTambemPermiteReenvio`, `falhaDeOuvinteDesfazConfirmacaoHistoricoEEstoqueParaReenvio`, `falhaNaDevolucaoDoCupomDesfazRecusaEPermiteReenvio` |
| Cancelamento pago/em preparação, estorno único, exceção/falha recuperável e legado | `PagamentoIntegracaoTests` |
| Rota temporária ausente da API e OpenAPI | `PagamentoIntegracaoTests.atalhoRemovidoEInexistenteNoOpenApi` |
| Regressão de reserva real e cupons usando nova integração | `CheckoutIntegracaoTests`, `PromocoesCuponsIntegracaoTests` |

Os testes de contrato/estado do atalho temporário foram substituídos por fluxos reais de cobrança e confirmação; não há aprovação direta por cliente. Testcontainers usa PostgreSQL 17 e Flyway V6. O novo ouvinte de teste demonstra Observer sem alteração no domínio pedido. A troca de fornecedor é demonstrada registrando outra implementação no contrato; não há alteração nos consumidores centrais.

Evidência E6 em 2026-10-22: `./mvnw clean verify` — BUILD SUCCESS, **562 testes**, zero falhas, erros ou ignorados; JAR gerado. As quatro classes de pagamento somam 26 casos (5 de domínio/adapters, 2 de persistência, 1 de migração e 18 de integração), substituindo os quatro casos exclusivos do atalho removido e mantendo toda a regressão anterior. `openspec validate pagamento-idempotencia --strict` e `git diff --check` aprovados.


## Entrega e frete — E7

| Cobertura | Evidência |
|---|---|
| Todas as UFs/regiões, duas APIs distintas, tarifas e cobertura configuráveis, invariantes e duplicidade | `EntregaDominioTests` |
| JWT/CLIENTE, propriedade de endereço, 400/401/403/404/409 e seleção obrigatória | `EntregaIntegracaoTests.segurancaValidacaoESemSelecaoNaoHaFreteImplicito` |
| Consulta sem escrita, frete bruto/líquido e preço composto | `opcoesRegionaisSemEscritaEPrecoComposto` |
| Novo bean de transportadora selecionado sem mudar checkout | `novaTransportadoraESelecionadaSemMudarCheckout` |
| Prazo/modalidade com mesmo preço e tarifa alterada com frete grátis | `prazoOuModalidadeDeMesmoPrecoExigemConfirmacao`, `freteGratisNaoOcultaTarifaAlteradaETetoNaoDuplicaAbatimento` |
| Total 261 com benefícios e cotação, política exclusiva e redução residual por modalidade | `total261ComFreteGratisEPromocoesECupom`, `promocaoExclusivaBloqueiaFreteGratisEReducaoFicaNoAlvoCorreto` |
| Perda de cobertura sem fallback e falha parcial/total (503) | `perdaDeCoberturaRetornaOpcoesSemTrocarAutomaticamente`, `falhasDeProvedorDiferemDeCoberturaENaoGeramZero` |
| Snapshot persistido após mudança de cotação, pago até ENTREGUE | `snapshotPagoAteEntregaPermaneceMesmoDepoisDeMudarCotacao` |
| Cupom de frete consumido/devolvido uma vez e estorno original; recusa repetida sem estorno | `cupomDeFreteConsumidoEDevolvidoUmaVezNoEstornoOriginal`, `recusaDePagamentoDevolveCupomDeFreteSemGerarEstorno` |
| Upgrade V6→V7 preserva pedido pago/cobrança/confirmação sem fabricar modalidade | `MigracaoEntregaTests` |
| Regressão de rollback/concorrência com entrega cotada | `CheckoutIntegracaoTests`, `PromocoesCuponsIntegracaoTests`, `PagamentoIntegracaoTests` |
| Demonstração dev idempotente com modalidade e ausência fora de dev | `UsuariosDeDemonstracaoTests`, `UsuariosDeDemonstracaoForaDoDevTests` |

Os exemplos de total de compra nos testes E4b–E6 foram atualizados com a tarifa padrão de R$ 25 em SP; o carrinho anterior à escolha de entrega mantém seu resumo sem cotação. A regressão de benefício zero agora cria campanha de frete grátis para zerar o residual do cupom, em vez de desativar artificialmente o frete da produção. Dados legados e testes internos de estado continuam sem obrigação de inventar modalidade histórica.

Evidência E7 em 2026-10-22: `./mvnw -o clean verify` — BUILD SUCCESS, **584 testes**, zero falhas, erros ou ignorados; JAR gerado. As três classes E7 somam 22 casos: EntregaDominioTests (9, incluindo cinco regiões parametrizadas), EntregaIntegracaoTests (12) e MigracaoEntregaTests (1). Regressão E3–E6, ArchUnit e demonstração dev aprovadas. `openspec validate entrega-frete --strict` e `git diff --check` passaram.


### E8 — grupo 1: notificações implementadas

| Cenário | Evidência |
|---|---|
| T-ID-03: replay sequencial e concorrente não duplica comunicação | `PagamentoIntegracaoTests`: cartão repetido, boleto recusado repetido, duas confirmações simultâneas e replay após cancelamento |
| T-EV-01: efeitos comerciais e comunicação independentes | `NotificacoesIntegracaoTests` registra canal/interessado adicionais; `PagamentoIntegracaoTests` verifica mensagem única e baixa real |
| T-EV-02: falha de comunicação não desfaz pagamento | `falhaDeCanalNaoDesfazPagamentoNemMudaRespostaOuReplay`; `EnviarNotificacoesTests` confirma continuidade dos demais canais |
| Cinco acontecimentos, destinatário correto e envio somente após commit | `NotificacoesIntegracaoTests.todosOsAcontecimentosChegamAoNovoCanalApenasDepoisDoCommit` |
| Rollback e evento sem transação não enviam | `NotificacoesIntegracaoTests.rollbackNaoEnviaENaoHaFallbackForaDaTransacao`; falha de ouvinte/cupom na integração de pagamento |
| Resolução ausente/falha, configuração inválida e logs sem dados sensíveis | `EnviarNotificacoesTests` |
| Cancelamento notificado devolve cupom e estoque uma vez | `PagamentoIntegracaoTests.cupomDevolvidoUmaVezNoCancelamentoNotificado` |
| Pedido/pagamento não dependem da comunicação | ArchUnit `pedidoEPagamentoNaoDependemDeNotificacao` |

Em 2026-10-22: seleção de testes `PagamentoIntegracaoTests,NotificacoesIntegracaoTests,EnviarNotificacoesTests,RegrasDeArquiteturaTests` aprovada (36 testes). `./mvnw -o clean verify` aprovado: **592 testes, zero falhas/erros/ignorados**, JAR gerado em `target/ecommerce-0.0.1-SNAPSHOT.jar`. São sete casos novos de notificações/pagamento, uma regra ArchUnit nova e novas asserções nos cenários comerciais existentes. Evidência detalhada em `/tmp/bdr-notificacoes-verify.log`. Auditoria (T-EV-03) e sua consulta continuam pendentes; esta execução valida o grupo 1 e as regressões anteriores.

### E8 — grupo 2: auditoria transacional

`OperacaoAdministrativaTests` verifica campos obrigatórios, snapshot profundamente imutável, rejeição de credenciais aninhadas e objetos arbitrários. `AuditoriaIntegracaoTests` verifica ator por JWT real e perfis administrativos, extensão por novo interessado, categorias, produtos/variações, preços e componentes de combos, ajuste de estoque e rejeição negativa, campanhas/cupons e conflito de versão, usuários sem credenciais, estados e cancelamento administrativo pago. Confere contagens exatas em operações compostas, ausência de trilha para cliente/sistema, rollback posterior e falha de persistência da auditoria revertendo a mutação.

`MigracaoAuditoriaTests` aplica V1–V7 com pedido pago/cobrança/confirmação existentes e migra para V8: valores e resultado são preservados, auditoria começa vazia e permanece após remover o recurso. ArchUnit inclui auditoria na proteção de dependências do compartilhado. Esses testes cobrem o registro T-EV-03; consulta/filtros/permissões da trilha permanecem no grupo 3.

Asserções adicionais em `PagamentoIntegracaoTests` confirmam ausência de trilha administrativa no pagamento/replay sequencial e concorrente, mantendo somente o cancelamento administrativo. `UsuariosDeDemonstracaoTests` confere auditoria vazia após carga dev repetida sem ator.

Em 2026-10-23, validação final do grupo 2: `./mvnw -o clean verify`, **BUILD SUCCESS, 607 testes, zero falhas/erros/ignorados** e JAR gerado. Inclui migração V7→V8, auditoria, carga dev, asserções de replay e regressões E3–E7. Log: `/tmp/bdr-auditoria-verify-final.log`. OpenSpec strict e git diff --check aprovados. A consulta administrativa foi implementada no grupo 3 e a validação final (649 testes) está registrada no fechamento de 2026-10-23.

### E8 — grupo 3: consulta e demonstração

`ConsultarAuditoriaTests` contém dez testes de integração com PostgreSQL e JWT real: ordenação por momento/id (incluindo empates), detalhes anterior/novo, filtros isolados/combinados, intervalo inclusivo e equivalência de fuso, páginas de tamanho 1 e 100, total completo e página vazia, deslocamento grande sem overflow, operação desconhecida, filtros malformados rejeitados antes do banco, 401/403 para todos os perfis sem ADMIN, proteção na aplicação e ausência de rotas de escrita. OpenAPI é conferido contra os seis parâmetros e respostas 200/400/401/403.

`PagamentoIntegracaoTests.roteiroDeCompraNotificadaEConsultaDaTrilha` executa por HTTP MockMvc o roteiro preço→campanha→checkout→PIX aprovado→cancelamento pago→consulta da trilha, com banco real. Verifica comunicação de criação/aprovação/cancelamento, total e estorno, reposição e exatamente três registros administrativos, incluindo consulta com usuário/operação e intervalo inclusivo. `falhaDeCanalNaoDesfazPagamentoNemMudaRespostaOuReplay` injeta falha no canal por spy de teste e confirma sucesso HTTP, pagamento/estoque preservados e replay sem duplicação. Os comandos para reprodução estão em [10-execucao.md](10-execucao.md).

Evidência do grupo 3 em 2026-10-23: comando de reprodução de docs/10 aprovado com **62 testes, zero falhas/erros/ignorados (BUILD SUCCESS)**, incluindo consulta, roteiro de compra notificada/falha de canal, auditoria transacional, migração e ArchUnit. Log `/tmp/bdr-auditoria-grupo3-tests.log`. OpenSpec strict e git diff --check aprovados. `clean verify` completo e geração do JAR atualizado permanecem no grupo 4; progresso 15/17 tarefas.

## Relatórios gerenciais — E9 (`implementado`)

`RelatoriosGerenciaisTests` (T-RL-01, integração com PostgreSQL e JWT real) semeia por SQL dez pedidos de datas e descontos controlados no período de março de 2026: quatro vendas (415,00, 200,00 pago exatamente no início do período, 100,00 pago exatamente no fim e 84,99) e seis que não são venda (pago e depois cancelado, pago em fevereiro e cancelado em março, aguardando pagamento, pago em abril, cancelado sem pagar e cancelado em abril), com itens e descontos grandes para denunciar inclusão indevida. Confere valor total 799,99, 4 pedidos, ticket médio 200,00 (199,9975 pelo arredondamento bancário), descontos 80,00 e 3 cancelamentos no período; bordas inclusivas, fuso e `inicio` igual a `fim`; período sem dados com zeros e listas vazias; produtos somados entre pedidos, ordenação por quantidade e SKU e limite (padrão 10); campanhas por referência, promoção e cupom no mesmo pedido, desconto de frete, legado sem referência, ordenação e coerência da soma com o total de descontos do resumo. Um teste usa pedidos reais criados pelos serviços do pedido (`PedidosPersistidosParaTeste`) com o histórico reposicionado no tempo.

Também cobre validação (período ausente, vazio, malformado, sem fuso, invertido e `limite` inválido retornam 400 sem consultar a porta), segurança (401; `ADMIN` e `OPERADOR_COMERCIAL` com 200; `PROMOCOES` só em campanhas; `ESTOQUE` e `CLIENTE` com 403; o serviço de aplicação nega fora da API), ausência de rotas de escrita (405, dados intactos) e OpenAPI (rotas, parâmetros e respostas 200/400/401/403). `RelatoriosDominioTests` cobre período, limite e ticket médio. ArchUnit ganhou duas regras para o módulo. Não há teste de extensibilidade porque a change não tem ponto de variação nem pattern.

Evidência em 2026-10-23: `./mvnw -o clean verify` — **BUILD SUCCESS, 649 testes, zero falhas/erros/ignorados** (29 novos: 13 de `RelatoriosDominioTests` e 16 de `RelatoriosGerenciaisTests`, mais duas regras ArchUnit) e JAR gerado. `openspec validate relatorios-gerenciais --strict` e `git diff --check` aprovados.
