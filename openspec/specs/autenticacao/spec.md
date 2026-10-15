# autenticacao Specification

## Purpose
Garante que só usuários identificados acessem a API e que cada um execute apenas as operações compatíveis com seus perfis, protegendo as credenciais armazenadas.

## Requirements

### Requirement: Login com e-mail e senha
O sistema SHALL autenticar um usuário ativo a partir de e-mail e senha e devolver um token de acesso do tipo Bearer com tempo de expiração informado na resposta. Credenciais incorretas MUST resultar em 401 com a mesma mensagem, exista ou não o e-mail.

#### Scenario: Login bem-sucedido
- **WHEN** um usuário ativo envia e-mail e senha corretos para o login
- **THEN** o sistema responde 200 com um token de acesso, o tipo `Bearer` e o tempo de expiração em segundos

#### Scenario: Senha incorreta
- **WHEN** um usuário envia um e-mail cadastrado com senha incorreta
- **THEN** o sistema responde 401 com mensagem genérica de credenciais inválidas

#### Scenario: E-mail inexistente
- **WHEN** alguém envia um e-mail não cadastrado
- **THEN** o sistema responde 401 com a mesma mensagem genérica usada para senha incorreta

#### Scenario: Usuário inativo
- **WHEN** um usuário inativado envia credenciais corretas
- **THEN** o sistema responde 401 e não emite token

### Requirement: Acesso exige token válido
Todo endpoint, exceto cadastro de cliente, login, documentação da API e verificação de saúde, MUST exigir um token válido e não expirado.

#### Scenario: Requisição sem token
- **WHEN** uma requisição a um endpoint protegido não contém token
- **THEN** o sistema responde 401

#### Scenario: Token expirado ou adulterado
- **WHEN** uma requisição usa um token expirado ou com assinatura inválida
- **THEN** o sistema responde 401

#### Scenario: Endpoints públicos
- **WHEN** uma requisição sem token chama o cadastro de cliente, o login, a documentação da API ou a verificação de saúde
- **THEN** o sistema processa a requisição normalmente

### Requirement: Senha armazenada apenas como hash
O sistema MUST armazenar senhas somente na forma de hash com algoritmo adaptativo e sal, e MUST NOT devolver a senha nem o hash em nenhuma resposta.

#### Scenario: Persistência da senha
- **WHEN** um usuário é criado com uma senha
- **THEN** o valor persistido difere da senha original e é um hash verificável

#### Scenario: Respostas sem credenciais
- **WHEN** qualquer endpoint devolve dados de um usuário
- **THEN** a resposta não contém senha nem hash de senha

### Requirement: Política mínima de senha
O sistema SHALL rejeitar senhas com menos de 8 caracteres ou sem ao menos uma letra e um dígito, respondendo 400 com o erro associado ao campo.

#### Scenario: Senha fraca
- **WHEN** um cadastro informa a senha `abcdefgh`
- **THEN** o sistema responde 400 indicando o campo `senha` como inválido

### Requirement: Autorização por perfil
O sistema SHALL suportar os perfis `CLIENTE`, `ADMIN`, `OPERADOR_COMERCIAL`, `ESTOQUE` e `PROMOCOES`. Um usuário pode ter mais de um perfil administrativo. Uma operação negada por falta de perfil MUST resultar em 403.

#### Scenario: Cliente em área administrativa
- **WHEN** um usuário com perfil `CLIENTE` chama a gestão de usuários administrativos
- **THEN** o sistema responde 403

#### Scenario: Perfil administrativo sem permissão
- **WHEN** um usuário apenas com perfil `ESTOQUE` chama a gestão de usuários administrativos
- **THEN** o sistema responde 403

### Requirement: Gestão de usuários administrativos
Um usuário `ADMIN` SHALL poder criar usuários administrativos com um ou mais perfis administrativos, listá-los, alterar seus perfis e inativá-los. O sistema MUST rejeitar perfis inexistentes, e-mail duplicado e qualquer operação que deixe o sistema sem nenhum `ADMIN` ativo.

#### Scenario: Criar usuário administrativo
- **WHEN** um `ADMIN` cria um usuário com e-mail novo, senha válida e perfil `PROMOCOES`
- **THEN** o sistema responde 201 e o novo usuário consegue fazer login com esse perfil

#### Scenario: Perfil inexistente
- **WHEN** um `ADMIN` atribui o perfil `SUPERVISOR`
- **THEN** o sistema responde 400 indicando perfil inválido

#### Scenario: Atribuir CLIENTE a usuário administrativo
- **WHEN** um `ADMIN` tenta atribuir o perfil `CLIENTE` pela gestão de usuários administrativos
- **THEN** o sistema responde 400

#### Scenario: Remover o último ADMIN
- **WHEN** um `ADMIN` retira o perfil `ADMIN` do único usuário ativo com esse perfil, ou o inativa
- **THEN** o sistema responde 409 e mantém o usuário inalterado

### Requirement: Usuários de demonstração
Ao executar no perfil de desenvolvimento, o sistema SHALL garantir a existência de usuários de demonstração com dados fictícios: um usuário por perfil administrativo e ao menos um cliente. Fora do perfil de desenvolvimento, esses usuários MUST NOT ser criados.

#### Scenario: Execução em desenvolvimento
- **WHEN** a aplicação inicia no perfil de desenvolvimento com o banco vazio
- **THEN** os usuários de demonstração existem e conseguem fazer login com as credenciais documentadas

#### Scenario: Reinício em desenvolvimento
- **WHEN** a aplicação reinicia no perfil de desenvolvimento com os usuários de demonstração já existentes
- **THEN** nenhum usuário é duplicado

#### Scenario: Execução fora de desenvolvimento
- **WHEN** a aplicação inicia em outro perfil
- **THEN** nenhum usuário de demonstração é criado
