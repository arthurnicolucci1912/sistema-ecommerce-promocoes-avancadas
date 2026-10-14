# 09 - Testes automatizados

- **Objetivo:** mapear os cenários de teste exigidos pelo enunciado (e os necessários para validar as regras de negócio) para a etapa do roadmap em que serão implementados e para o tipo de teste (unitário ou integração).
- **Etapas do roadmap relacionadas:** todas; cada cenário indica sua etapa (ver [08-roadmap.md](08-roadmap.md)).
- **Status:** proposto (a lista de cenários é a base mínima; cada change de etapa pode acrescentar casos e deve atualizar esta tabela).

Documentos relacionados: [05-regras-negocio.md](05-regras-negocio.md) (regras testadas), [06-ciclo-vida-pedido.md](06-ciclo-vida-pedido.md) (transições), [07-casos-de-uso.md](07-casos-de-uso.md) (fluxos).

## 1. Diretrizes

- **Foco em comportamento, não em cobertura.** O enunciado pede testar comportamentos relevantes, e não atingir uma porcentagem elevada de cobertura. Nenhuma meta numérica de cobertura é adotada. Cada teste deve descrever uma regra de negócio observável.
- **JUnit 5** (com AssertJ e Mockito) como tecnologia de teste; testes de integração com Spring Boot Test.
- **Unitário:** testa uma classe ou pequeno grupo de classes de domínio sem Spring nem banco (promoção, condição, política de conflito, estado do pedido, cálculo de preço).
- **Integração:** sobe o contexto Spring e usa o banco (PostgreSQL de teste ou contêiner descartável, a decidir na etapa 1) para validar persistência, transações, restrições de unicidade, concorrência e a API HTTP com segurança.
- **Integrações externas simuladas:** gateways de pagamento, provedores de frete e canais de notificação são substituídos por mocks, stubs ou implementações simuladas. Nenhum teste depende de serviço externo real nem de dados reais de cartão.
- **Relógio controlável:** validade de promoções e cupons usa um `Clock` injetável para testar vigência e expiração de forma determinística.
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
| T-ES-01 | Reservar quantidade menor ou igual ao disponível | Disponível diminui e reservado aumenta na mesma quantidade | 2 | U, I |
| T-ES-02 | Estoque insuficiente na reserva | Reserva rejeitada, estoque inalterado | 2 | U |
| T-ES-03 | Checkout com estoque insuficiente em um item de vários | Checkout rejeitado com indicação do item; nenhuma reserva parcial (tudo-ou-nada); nenhum pedido criado | 4 | I |
| T-ES-04 | Estoque some entre a inclusão no carrinho e o checkout | O carrinho aceita; o checkout rejeita (a verificação no carrinho não garante disponibilidade) | 4 | I |
| T-ES-05 | **Concorrência na última unidade:** N requisições simultâneas (por exemplo 10 threads) para reservar 1 unidade restante | Exatamente 1 sucesso e N-1 falhas por estoque insuficiente; disponível nunca negativo | 2 (mecanismo), 4 (via checkout) | I |
| T-ES-06 | Liberar reserva por cancelamento, pagamento recusado ou expiração | Disponível restaurado, reservado reduzido; liberar duas vezes não restaura duas vezes | 4, 6 | U, I |
| T-ES-07 | Pagamento aprovado converte reserva em baixa | Reservado reduzido e baixa registrada, sem alterar o disponível uma segunda vez | 6 | I |
| T-ES-08 | Ajuste administrativo deixaria disponível negativo ou abaixo do reservado | Ajuste rejeitado | 2 | U |

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
| T-RL-01 | Relatórios (total de vendas, ticket médio, desconto por campanha) sobre pedidos conhecidos | Valores batem com os dados semeados | 9 | I |

## 3. Resumo de cobertura dos cenários do enunciado

| Cenário exigido pelo enunciado | IDs | Etapa(s) |
|---|---|---|
| Cada tipo de promoção (pelo menos 4) | T-PR-01 a T-PR-05 (e T-PR-06 a T-PR-10 de apoio) | 5 |
| Múltiplas promoções simultâneas e conflito | T-CF-01 a T-CF-08 | 5 (e 7) |
| Estoque insuficiente | T-ES-02, T-ES-03, T-ES-04 | 2, 4 |
| Concorrência na última unidade | T-ES-05 | 2, 4 |
| Cupom inválido (inexistente, expirado, limite, incompatível) | T-CU-01 a T-CU-04 | 5 |
| Pagamento recusado | T-PG-01 | 6 |
| Transição inválida de pedido | T-PD-02, T-PD-04, T-PD-06 | 4 |
| Confirmação de pagamento repetida | T-ID-01 a T-ID-05 | 6 (e 8) |

## 4. Mapa dos testes por etapa

| Etapa | Cenários |
|---|---|
| 1 Fundação | T-SG-01 (base), T-SG-02 (base), T-SG-03 |
| 2 Catálogo e estoque | T-ES-01, T-ES-02, T-ES-05 (mecanismo), T-ES-08 |
| 3 Carrinho | recálculo do carrinho, itens inválidos e composição do preço (acrescentar IDs `T-CA-xx` ao implementar) |
| 4 Checkout e pedido | T-ES-03, T-ES-04, T-ES-05 (via checkout), T-ES-06, T-PD-01 a T-PD-06, T-SG-01 |
| 5 Promoções e cupons | T-PR-01 a T-PR-10, T-CF-01 a T-CF-08, T-CU-01 a T-CU-07, T-SG-02 |
| 6 Pagamento e idempotência | T-PG-01 a T-PG-06, T-ES-06, T-ES-07, T-ID-01, T-ID-02, T-ID-04, T-ID-05 |
| 7 Entrega e frete | T-EN-01, T-EN-02, T-PR-05, T-CF-08, T-PD-06 |
| 8 Eventos, notificações e auditoria | T-ID-03, T-EV-01 a T-EV-03 |
| 9 Relatórios | T-RL-01 |

Os testes de carrinho (etapa 3) ainda não têm IDs detalhados: serão listados na change `carrinho-compras`, atualizando este documento.
