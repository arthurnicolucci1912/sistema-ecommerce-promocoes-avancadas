# Spec Delta

## Purpose

Permite que um visitante crie uma conta de cliente e que o cliente mantenha seus dados cadastrais e endereços de entrega, acessando sempre apenas os próprios dados.

## ADDED Requirements

### Requirement: Criação de conta de cliente
O sistema SHALL permitir que um visitante não autenticado crie uma conta de cliente informando nome, e-mail, senha, CPF e, opcionalmente, telefone. A conta recebe o perfil `CLIENTE`. O sistema MUST validar o formato do e-mail e os dígitos verificadores do CPF, e MUST rejeitar e-mail ou CPF já cadastrados.

#### Scenario: Cadastro válido
- **WHEN** um visitante envia nome, e-mail novo, senha válida e CPF válido
- **THEN** o sistema responde 201 com os dados do cliente, sem a senha, e o cliente consegue fazer login

#### Scenario: E-mail já cadastrado
- **WHEN** um visitante usa um e-mail que já pertence a outro usuário
- **THEN** o sistema responde 409

#### Scenario: CPF já cadastrado
- **WHEN** um visitante usa um CPF que já pertence a outro cliente
- **THEN** o sistema responde 409

#### Scenario: Dados inválidos
- **WHEN** um visitante envia e-mail mal formado e CPF com dígitos verificadores incorretos
- **THEN** o sistema responde 400 listando o erro de cada campo

### Requirement: Consulta e alteração dos próprios dados
Um cliente autenticado SHALL poder consultar e alterar o próprio nome e telefone. O e-mail e o CPF MUST NOT ser alterados por esse recurso.

#### Scenario: Consultar os próprios dados
- **WHEN** um cliente autenticado consulta seus dados
- **THEN** o sistema responde 200 com nome, e-mail, CPF e telefone dele

#### Scenario: Alterar telefone
- **WHEN** um cliente autenticado altera o telefone
- **THEN** o sistema responde 200 e a consulta seguinte mostra o novo telefone

### Requirement: Gestão de endereços
Um cliente autenticado SHALL poder cadastrar, listar, consultar, alterar e remover endereços próprios. Cada endereço tem apelido, CEP, logradouro, número, complemento opcional, bairro, cidade e UF. O CEP MUST ter 8 dígitos, e a UF MUST ser uma sigla de estado brasileiro válida. A remoção MUST inativar o endereço, preservando-o para referências futuras: ele deixa de aparecer na listagem.

#### Scenario: Cadastrar endereço
- **WHEN** um cliente cadastra um endereço com todos os campos obrigatórios válidos
- **THEN** o sistema responde 201 e o endereço aparece na listagem do cliente

#### Scenario: Vários endereços
- **WHEN** um cliente cadastra dois endereços
- **THEN** a listagem devolve os dois

#### Scenario: Endereço inválido
- **WHEN** um cliente cadastra um endereço com CEP `123` e UF `XX`
- **THEN** o sistema responde 400 indicando os campos `cep` e `uf`

#### Scenario: Remover endereço
- **WHEN** um cliente remove um endereço próprio
- **THEN** o sistema responde 204 e o endereço deixa de aparecer na listagem

### Requirement: Endereço principal
Cada cliente SHALL ter no máximo um endereço principal. O primeiro endereço cadastrado é o principal. Marcar outro endereço como principal MUST desmarcar o anterior. Se o principal for removido, o endereço ativo mais antigo restante passa a ser o principal.

#### Scenario: Primeiro endereço
- **WHEN** um cliente sem endereços cadastra um endereço
- **THEN** esse endereço é marcado como principal

#### Scenario: Trocar endereço principal
- **WHEN** um cliente marca como principal um segundo endereço
- **THEN** apenas o segundo endereço aparece como principal

#### Scenario: Remover o principal
- **WHEN** um cliente com dois endereços remove o principal
- **THEN** o endereço restante passa a ser o principal

### Requirement: Isolamento entre clientes
Um cliente MUST acessar somente os próprios dados e endereços. Uma tentativa de acessar, alterar ou remover um endereço de outro cliente MUST responder 404, sem revelar que o recurso existe.

#### Scenario: Acessar endereço de outro cliente
- **WHEN** o cliente A consulta, altera ou remove um endereço pertencente ao cliente B
- **THEN** o sistema responde 404 e o endereço do cliente B permanece inalterado
