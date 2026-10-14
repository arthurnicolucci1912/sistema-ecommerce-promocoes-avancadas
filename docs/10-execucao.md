# 10 - Execução

- **Objetivo:** reunir as instruções para compilar, configurar e executar o sistema, e para rodar os testes e a demonstração. O enunciado exige instruções de execução na entrega final.
- **Etapas do roadmap relacionadas:** etapa 1 (fundação) preenche a maior parte; as etapas 2 a 9 complementam (dados de demonstração, novos endpoints, relatórios). Ver [08-roadmap.md](08-roadmap.md).
- **Status:** rascunho (placeholder: ainda não existe código; o conteúdo abaixo é a previsão).

## 1. Pré-requisitos previstos

| Item | Versão prevista | Observação |
|---|---|---|
| JDK | 21 | Java conforme exigência do enunciado (Java + Spring Boot). |
| Maven | 3.9 ou superior | Ferramenta de build prevista (pode ser o wrapper `mvnw` do projeto). |
| PostgreSQL | 15 ou superior | Banco relacional previsto; o enunciado admite PostgreSQL, MySQL ou outro previamente aprovado. |
| Docker e Docker Compose | opcional | Para subir o PostgreSQL sem instalação local. |
| Git | qualquer versão recente | Repositório e histórico de commits fazem parte da entrega. |

As versões são previsões e serão confirmadas na etapa de fundação.

## 2. A preencher na etapa de fundação

Esta seção será preenchida na change `fundacao-projeto` (etapa 1 do roadmap).

### 2.1 Obtenção do código
- A preencher: URL do repositório e comando de clonagem.

### 2.2 Configuração do banco de dados
- A preencher: criação do banco e do usuário (ou `docker compose up`), variáveis de ambiente/propriedades de conexão e perfis (`dev`, `test`).

### 2.3 Build
- A preencher: comando de compilação e empacotamento (por exemplo, `./mvnw clean package`) e opção de pular testes, se houver.

### 2.4 Migrações Flyway
- A preencher: local dos scripts de migração, execução automática na subida da aplicação, como recriar o banco do zero e convenção de nomes das versões.

### 2.5 Execução da aplicação
- A preencher: comando para subir a aplicação, porta padrão e como verificar que está no ar.

### 2.6 Swagger / OpenAPI
- A preencher: URL da interface Swagger UI e do documento OpenAPI, e como autenticar-se nela para chamar endpoints protegidos.

### 2.7 Usuários de demonstração
- A preencher: lista de usuários por perfil (`CLIENTE`, `ADMIN`, `OPERADOR_COMERCIAL`, `ESTOQUE`, `PROMOCOES`), com e-mails e senhas **fictícios** de demonstração e a indicação de como são carregados (migração ou carga inicial). Nenhum dado real de pessoa ou de cartão de crédito deve constar; dados de pagamento de demonstração são fictícios.

### 2.8 Execução dos testes
- A preencher: comando para rodar os testes unitários e de integração (ver [09-testes.md](09-testes.md)), eventual separação entre eles e requisitos dos testes de integração (por exemplo, banco em contêiner ou banco em memória).

## 3. A preencher nas etapas seguintes

| Etapa | O que acrescentar aqui |
|---|---|
| 2 a 3 | Carga de dados de demonstração do catálogo e do estoque. |
| 5 | Promoções e cupons de demonstração. |
| 6 | Como simular a confirmação de pagamento (aprovada, recusada e repetida). |
| 7 | Modalidades de entrega simuladas disponíveis. |
| 8 | Como consultar notificações simuladas e a trilha de auditoria. |
| 9 | Roteiro da demonstração final, conforme o checklist de [08-roadmap.md](08-roadmap.md) (seção 5). |

## 4. Solução de problemas

A preencher durante o desenvolvimento (porta ocupada, falha de conexão com o banco, migração com conflito etc.).
