# Documentação do Sistema de E-commerce com Promoções Avançadas

> **Objetivo:** apresentar a visão geral do sistema, servir de índice para a documentação e relacionar cada documento às etapas de produção (roadmap).
> **Etapas do roadmap relacionadas:** todas (1 a 9).
> **Status:** rascunho

## Visão geral

Sistema de comércio eletrônico de médio porte, desenvolvido como projeto acadêmico (FATEC) em **Java e Spring Boot**. Ele gerencia clientes, produtos, estoque, carrinho, pedidos, pagamentos, entregas e, principalmente, um conjunto flexível de **promoções comerciais**.

O desafio central não é o CRUD de uma loja virtual, e sim uma arquitetura que continue simples de evoluir quando surgirem novas campanhas, formas de pagamento, transportadoras e canais de comunicação. Por isso o projeto aplica Design Patterns como ferramenta para organizar a variabilidade, e cada padrão é justificado por um requisito concreto do enunciado (documento 04).

### Usuários

- **Cliente:** cria conta, consulta produtos, monta o carrinho, informa endereços, faz pedidos, acompanha pagamento e entrega.
- **Administrador:** gerencia catálogo, preços, estoques, campanhas, pedidos e relatórios. Perfis adicionais (operador comercial, responsável pelo estoque, responsável pelas promoções) são opcionais.

### Stack prevista

| Camada | Tecnologia |
|---|---|
| Linguagem e framework | Java + Spring Boot |
| Persistência | Spring Data JPA + PostgreSQL |
| Segurança | Spring Security (autenticação e autorização por perfil) |
| Interface | API REST documentada com OpenAPI/Swagger (interface gráfica é opcional) |
| Testes | JUnit (com mocks/stubs para integrações externas) |

### Convenções da documentação

- Idioma: português. Diagramas em **Mermaid**, renderizados nativamente pelo GitHub.
- Todo documento começa com um cabeçalho com **objetivo**, **etapas do roadmap relacionadas** e **status** (`rascunho`, `proposto` ou `implementado`).
- `proposto` indica decisão que ainda pode mudar durante a implementação da etapa correspondente. Quando mudar, o motivo é registrado no próprio documento.
- Os diagramas atuais são **preliminares**: os nomes definitivos de classes serão refinados em cada etapa de implementação.
- Cada etapa do roadmap deve atualizar os documentos indicados na tabela abaixo.

## Índice

| # | Documento | Conteúdo |
|---|---|---|
| 01 | [Requisitos](01-requisitos.md) | Requisitos funcionais e não funcionais, obrigatórios e opcionais |
| 02 | [Modelo de domínio](02-modelo-dominio.md) | Entidades, relacionamentos, variações de produto, categorias, combos e diagrama de classes |
| 03 | [Arquitetura](03-arquitetura.md) | Camadas, módulos e pacotes, pontos de variação e contratos centrais |
| 04 | [Design Patterns](04-design-patterns.md) | Padrões candidatos, problema, justificativa, classes participantes e benefício |
| 05 | [Regras de negócio](05-regras-negocio.md) | Conflito entre promoções, cupons, formação do preço, estoque, idempotência, cancelamento e frete grátis |
| 06 | [Ciclo de vida do pedido](06-ciclo-vida-pedido.md) | Estados e transições do pedido |
| 07 | [Casos de uso](07-casos-de-uso.md) | Casos de uso e diagramas de sequência |
| 08 | [Roadmap](08-roadmap.md) | As 9 etapas incrementais e as changes do OpenSpec |
| 09 | [Testes](09-testes.md) | Cenários de teste obrigatórios e a etapa que cobre cada um |
| 10 | [Execução](10-execucao.md) | Como executar o sistema (placeholder até existir código) |
| 11 | [Divisão da equipe](11-divisao-equipe.md) | Responsabilidades por integrante, ordem de trabalho e fluxo de revisão |

Ordem sugerida de leitura: 01, 02, 03, 04, 05, 06, 07. Os documentos 08, 09, 10 e 11 são de planejamento e operação.

## Documentos x etapas do roadmap

As 9 etapas incrementais (detalhadas em [08-roadmap.md](08-roadmap.md)) são: **E1** fundação, **E2** catálogo e estoque, **E3** carrinho, **E4** checkout e pedido, **E5** promoções e cupons, **E6** pagamento e idempotência, **E7** entrega e frete, **E8** eventos, notificações e auditoria, **E9** relatórios.

A marca `●` indica que a etapa cria ou atualiza o documento.

| Documento | E1 | E2 | E3 | E4 | E5 | E6 | E7 | E8 | E9 |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| [01-requisitos](01-requisitos.md) | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| [02-modelo-dominio](02-modelo-dominio.md) | ● | ● | ● | ● | ● | ● | ● | ● | |
| [03-arquitetura](03-arquitetura.md) | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| [04-design-patterns](04-design-patterns.md) | | ● | ● | ● | ● | ● | ● | ● | |
| [05-regras-negocio](05-regras-negocio.md) | | ● | | ● | ● | ● | ● | | |
| [06-ciclo-vida-pedido](06-ciclo-vida-pedido.md) | | | | ● | | ● | ● | ● | |
| [07-casos-de-uso](07-casos-de-uso.md) | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| [08-roadmap](08-roadmap.md) | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| [09-testes](09-testes.md) | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| [10-execucao](10-execucao.md) | ● | | | | | | | | |

## Relação com o enunciado

O enunciado exige, como documentação: descrição do sistema, principais requisitos, modelo de domínio, arquitetura, casos de uso, ao menos um diagrama de classes, um diagrama do ciclo de vida do pedido, diagramas de sequência (checkout, aplicação de promoções, confirmação de pagamento) e a justificativa de cada Design Pattern.

| Exigência do enunciado | Documento |
|---|---|
| Descrição do sistema | Este README |
| Principais requisitos | [01](01-requisitos.md) |
| Modelo de domínio e diagrama de classes | [02](02-modelo-dominio.md) |
| Arquitetura adotada | [03](03-arquitetura.md) |
| Patterns: classes participantes e justificativa | [04](04-design-patterns.md) |
| Ciclo de vida do pedido | [06](06-ciclo-vida-pedido.md) |
| Casos de uso e diagramas de sequência | [07](07-casos-de-uso.md) |
| Instruções para execução | [10](10-execucao.md) |
