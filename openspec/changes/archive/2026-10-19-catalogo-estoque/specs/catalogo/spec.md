# Spec Delta

## Purpose

Organiza o que a loja vende: categorias hierárquicas, produtos com variações e combos, com consulta pública e administração restrita aos perfis comerciais.

## ADDED Requirements

### Requirement: Categorias hierárquicas
Um usuário `ADMIN` ou `OPERADOR_COMERCIAL` SHALL poder criar, renomear e mover categorias, com uma categoria pai opcional e profundidade livre.
- O nome MUST ser único entre categorias irmãs.
- Mover uma categoria para baixo de si mesma ou de uma descendente MUST ser rejeitado com 409.
- Remover uma categoria que tenha subcategorias ou produtos MUST ser rejeitado com 409.

#### Scenario: Criar subcategoria
- **WHEN** um `OPERADOR_COMERCIAL` cria "Camisetas" com pai "Roupas"
- **THEN** o sistema responde 201 e a árvore mostra "Camisetas" dentro de "Roupas"

#### Scenario: Ciclo na hierarquia
- **WHEN** alguém move "Roupas" para dentro de "Camisetas", sua descendente
- **THEN** o sistema responde 409 e a árvore não muda

#### Scenario: Remover categoria em uso
- **WHEN** alguém remove uma categoria que contém produtos
- **THEN** o sistema responde 409

### Requirement: Produtos e variações
Um usuário `ADMIN` ou `OPERADOR_COMERCIAL` SHALL poder cadastrar produtos com nome, descrição, marca opcional e exatamente uma categoria. Cada produto tem uma ou mais variações, e cada variação tem:
- SKU único em todo o catálogo;
- preço maior que zero;
- atributos nome/valor (por exemplo, tamanho M, cor azul);
- situação ativa ou inativa.

Produtos e variações MUST ser inativados em vez de excluídos. Novos tipos de característica MUST ser representáveis por novos atributos, sem mudança no cadastro.

#### Scenario: Produto com variações
- **WHEN** um operador cadastra "Camiseta Básica" em "Camisetas" com as variações M/azul a R$ 59,90 e G/azul a R$ 59,90
- **THEN** o sistema responde 201 e o produto tem duas variações com SKUs distintos

#### Scenario: Novo tipo de característica
- **WHEN** um operador cadastra um notebook com o atributo "Armazenamento: 512 GB", nunca usado antes
- **THEN** o produto é cadastrado sem nenhuma configuração prévia do atributo

#### Scenario: Dados inválidos
- **WHEN** alguém cadastra uma variação com preço zero ou negativo, ou um produto com categoria inexistente
- **THEN** o sistema responde 400 indicando o campo inválido

#### Scenario: SKU duplicado
- **WHEN** alguém cadastra uma variação com um SKU já usado por outra variação ou combo
- **THEN** o sistema responde 409

#### Scenario: Inativar produto
- **WHEN** um operador inativa um produto
- **THEN** o produto e suas variações deixam de aparecer na consulta pública, e os pedidos existentes não mudam

### Requirement: Combos
Um usuário `ADMIN` ou `OPERADOR_COMERCIAL` SHALL poder cadastrar combos com SKU próprio, nome, preço próprio maior que zero e uma lista de componentes com quantidade. Um componente pode ser uma variação ou outro combo. Um combo MUST NOT conter a si mesmo, direta ou indiretamente. Um combo com algum componente inativo MUST deixar de aparecer na consulta pública.

#### Scenario: Cadastrar combo
- **WHEN** um operador cadastra o "Kit Café" com 1 caneca e 2 pacotes de café por R$ 79,90
- **THEN** o sistema responde 201 e o combo lista os componentes e as quantidades

#### Scenario: Combo dentro de combo
- **WHEN** um operador cadastra um combo que contém o "Kit Café" e mais uma variação
- **THEN** o combo é aceito, e seus componentes de estoque incluem os itens do "Kit Café" multiplicados pela quantidade

#### Scenario: Combo circular
- **WHEN** alguém inclui um combo em um de seus próprios componentes
- **THEN** o sistema responde 409

### Requirement: Itens vendáveis tratados de forma uniforme
Para quem consome o catálogo (carrinho, checkout e promoções), o sistema SHALL tratar variações e combos de forma uniforme. Dado um SKU, devolve:
- nome;
- preço;
- categorias ancestrais;
- situação;
- componentes de estoque (variação e quantidade).

Um novo tipo de item vendável MUST poder ser incluído sem alterar quem consome itens vendáveis.

#### Scenario: Consultar variação por SKU
- **WHEN** um consumidor consulta o SKU de uma variação de "Camisetas", subcategoria de "Roupas"
- **THEN** recebe o preço da variação, as categorias "Camisetas" e "Roupas" e um único componente de estoque, a própria variação com quantidade 1

#### Scenario: Consultar combo por SKU
- **WHEN** um consumidor consulta o SKU do "Kit Café"
- **THEN** recebe o preço do combo e os componentes de estoque caneca ×1 e café ×2

### Requirement: Consulta pública do catálogo
Qualquer visitante, autenticado ou não, SHALL poder consultar:
- a árvore de categorias;
- a lista paginada de produtos ativos, filtrável por categoria (incluindo todas as subcategorias), por trecho do nome e por faixa de preço;
- o detalhe de um produto, com variações ativas, atributos, preço e disponibilidade de cada variação;
- os combos ativos com sua disponibilidade.

Produto inexistente ou inativo MUST responder 404.

#### Scenario: Filtro por categoria inclui subcategorias
- **WHEN** um visitante sem token filtra produtos pela categoria "Roupas"
- **THEN** a lista inclui os produtos de "Camisetas", subcategoria de "Roupas"

#### Scenario: Filtro sem resultado
- **WHEN** um visitante filtra pela faixa de preço R$ 10.000 a R$ 20.000 e nenhum produto se encaixa
- **THEN** o sistema responde 200 com lista vazia

#### Scenario: Disponibilidade no detalhe
- **WHEN** um visitante consulta um produto cuja variação M tem 3 unidades disponíveis
- **THEN** o detalhe mostra 3 unidades disponíveis para a variação M

#### Scenario: Disponibilidade do combo
- **WHEN** o "Kit Café" exige 2 cafés e há 5 cafés e 10 canecas disponíveis
- **THEN** a disponibilidade do combo é 2

#### Scenario: Produto inativo
- **WHEN** um visitante consulta o detalhe de um produto inativo
- **THEN** o sistema responde 404

### Requirement: Administração do catálogo restrita
As operações de escrita do catálogo SHALL exigir o perfil `ADMIN` ou `OPERADOR_COMERCIAL`. Qualquer outro perfil MUST receber 403, e uma requisição sem token MUST receber 401.

#### Scenario: Perfil de estoque altera catálogo
- **WHEN** um usuário `ESTOQUE` tenta cadastrar um produto
- **THEN** o sistema responde 403

#### Scenario: Visitante altera catálogo
- **WHEN** uma requisição sem token tenta cadastrar uma categoria
- **THEN** o sistema responde 401
