# Tasks

> Não fazer commits. Ao final, as alterações ficam no working tree para revisão e commit do autor.

## 1. Persistência

- [x] 1.1 Criar a migração Flyway `V3` com as tabelas de D1 a D4: unicidade global de SKU em `item_vendavel`, nome de categoria único por pai, `CHECK` de preço maior que zero, `CHECK` de disponível e reservado não negativos, situação da reserva e unicidade `(pedido_id, variacao_id)`. Verificar: `MigracaoV3Tests` confirma as tabelas e as restrições, e `ddl-auto=validate` passa.

## 2. Catálogo: domínio e Composite

- [x] 2.1 Implementar `Categoria`, com a regra de ciclo e de nome único por pai, e um serviço de árvore que calcula ancestrais e descendentes (D2). Verificar: testes unitários de ciclo (mover para si e para uma descendente), de ancestrais e de descendentes.
- [x] 2.2 Implementar o Composite (D1): `ItemVendavel` (componente), `VariacaoProduto` (folha, com atributos nome/valor), `Combo` e `ItemCombo` (composto), mais `Produto`, com Javadoc de pattern e papel e link para `docs/04-design-patterns.md` §3.8. Verificar com testes unitários:
  - os componentes de estoque da variação e do combo, inclusive combo dentro de combo, com a multiplicação e a agregação de variações repetidas;
  - a disponibilidade para venda com componente inativo;
  - a rejeição de combo circular;
  - **extensibilidade:** uma subclasse de teste nova de `ItemVendavel` é tratada por um combo e pelo cálculo de componentes sem nenhuma alteração nas classes existentes.
- [x] 2.3 Publicar `VariacaoCadastrada` ao cadastrar uma variação (Observer, publicador). Verificar: teste de que o evento é publicado com o id da variação.

## 3. Estoque: domínio, concorrência e Observer

- [x] 3.1 Implementar o saldo, `ReservarEstoque` (UPDATE condicional e tudo-ou-nada, D3), `EstoqueInsuficienteException` com `variacaoId` e `disponivel`, a liberação, a baixa e a reposição idempotentes (D4) e o ajuste administrativo. Verificar: testes de integração de "Reserva bem-sucedida", "Estoque insuficiente" (nenhum saldo alterado), "Liberar duas vezes", "Baixa", "Pedido sem reserva", reservar duas vezes para o mesmo pedido (rejeitado), "Ajuste que deixaria negativo" e "Entrada de mercadoria".
- [x] 3.2 Escrever o teste de concorrência "Dez pedidos pela última unidade", com 10 threads e uma barreira de início. Verificar: exatamente 1 sucesso e 9 `EstoqueInsuficienteException`, com saldo final de 0 disponível e 1 reservado. Repetir 3 vezes sem instabilidade.
- [x] 3.3 Implementar `OuvinteDeCatalogoDoEstoque` (`VariacaoCadastrada` cria saldo zero) e `OuvinteDePedidosDoEstoque` (D5, mapa de origem para ação, sem `switch`), com Javadoc de Observer. Verificar com testes de integração usando os serviços reais do `pedido`:
  - "Variação nova";
  - "Cancelamento antes do pagamento";
  - "Pagamento aprovado";
  - "Cancelamento depois do pagamento";
  - pagamento recusado libera;
  - transição rejeitada não altera o estoque;
  - **extensibilidade:** nenhum arquivo do módulo `pedido` foi alterado nesta change (comparado com `git diff`, no relatório).

## 4. Catálogo: aplicação e consulta

- [x] 4.1 Implementar os casos de uso de administração do catálogo (categorias, produtos e variações, combos e inativação) e `ConsultarItensVendaveis` (D7). Verificar: testes de integração de "Criar subcategoria", "Ciclo na hierarquia", "Remover categoria em uso", "Produto com variações", "Novo tipo de característica", "Dados inválidos", "SKU duplicado" (inclusive SKU de variação contra SKU de combo), "Inativar produto", "Cadastrar combo", "Combo dentro de combo", "Combo circular", "Consultar variação por SKU" e "Consultar combo por SKU".
- [x] 4.2 Implementar a consulta pública: árvore, produtos com filtros e paginação, detalhe com disponibilidade (via `ConsultarSaldo`) e combos com disponibilidade. Verificar: testes de "Filtro por categoria inclui subcategorias" (sem token), "Filtro sem resultado", "Disponibilidade no detalhe", "Disponibilidade do combo" e "Produto inativo".

## 5. API e segurança

- [ ] 5.1 Implementar os controllers e as regras de rota de D6, com `GET /api/catalogo/**` público, catálogo administrativo para `ADMIN` e `OPERADOR_COMERCIAL`, e estoque para `ADMIN` e `ESTOQUE`, sem abrir `/api/admin/usuarios` nem as rotas de pedidos. Verificar: testes de "Perfil de estoque altera catálogo", "Visitante altera catálogo", "Perfil comercial ajusta estoque" e "Estoque baixo", e de que as regras anteriores continuam valendo (todos os testes existentes passam).
- [ ] 5.2 Adicionar regras ArchUnit: `estoque` não depende de `catalogo`, `catalogo` não depende de `pedido`, e `pedido` não depende de `estoque` nem de `catalogo`. Verificar: as regras passam e falham com uma violação temporária, que depois é removida.

## 6. Demonstração

- [ ] 6.1 Estender `UsuariosDeDemonstracao` (D8) com o catálogo, os combos e o estoque inicial, idempotentes por SKU, com o Teclado em 1 unidade. O pedido de exemplo passa a usar os SKUs reais e a reservar estoque pelo `ReservarEstoque`. Verificar: o teste no perfil `dev` executa o runner duas vezes sem duplicar e confirma a reserva do pedido demo; fora do `dev`, nada é semeado.

## 7. Documentação

- [ ] 7.1 Atualizar com os nomes reais do código:
  - `docs/04-design-patterns.md`: §3.8 Composite com as classes reais, status implementado e o teste de extensibilidade; §3.6 Observer com os ouvintes do estoque e `VariacaoCadastrada`; remover a menção a "Composite de categorias", se houver;
  - `docs/02-modelo-dominio.md`: nomes padronizados `ItemVendavel`, `VariacaoProduto`, `Combo`, `ReservaEstoque` e as tabelas da `V3`;
  - `docs/03-arquitetura.md`: módulos `catalogo` e `estoque`, contratos `ConsultarItensVendaveis`, `ReservarEstoque` e `ConsultarSaldo`, ouvintes e regras ArchUnit novas;
  - `docs/05-regras-negocio.md`: RN-17 a RN-21 marcadas como implementadas, com a decisão de concorrência;
  - `docs/07-casos-de-uso.md`: UC-03 público e UC-A01 a A04 com as rotas reais;
  - `docs/09-testes.md`: T-ES-01, 02, 05, 06, 07 e 08, mais os novos cenários de catálogo, com as classes de teste reais;
  - `docs/10-execucao.md`: dados de demonstração do catálogo e a demo da última unidade.

  Verificar: os nomes citados existem e os links resolvem.
- [ ] 7.2 Corrigir `docs/08-roadmap.md`, `docs/11-divisao-equipe.md` e `docs/12-board.md`: o "Composite (categorias e combos)" vira "Composite (combos)" e "Observer (ouvintes do estoque)". No board, registrar `catalogo-estoque` como **implementado por Arthur**, com a nota de que a atribuição original era do Lucas, e manter a mesma informação no `11`, na tabela de responsabilidades. Verificar: os três documentos dizem o mesmo.

## 8. Verificação final

- [ ] 8.1 Rodar `./mvnw verify` (todos os testes, incluindo os de arquitetura) e o roteiro manual no perfil `dev`:
  - consultar `/api/catalogo/produtos?categoriaId=<Roupas>` sem token;
  - como `estoque@demo.local`, ajustar estoque e listar o estoque baixo;
  - simular a aprovação do pedido demo e conferir a baixa;
  - cancelar um pedido e conferir a reposição.

  Verificar: nenhum commit criado.
