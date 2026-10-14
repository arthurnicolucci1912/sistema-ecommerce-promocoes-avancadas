# Diretrizes de trabalho: orquestrador + subagentes

Objetivo: economizar tokens dividindo o trabalho entre um orquestrador e subagentes.

## Orquestrador (sessão principal)
- Modelo: Opus 5.5, esforço medium.
- Papel: orquestrar, planejar e revisar.
- Responsabilidades: entender o pedido, quebrar em tarefas pequenas e bem especificadas, delegar, revisar o resultado dos subagentes e decidir os próximos passos.
- Não escrever código extenso nem fazer tarefas repetitivas por conta própria: delegar.

## Subagentes (`.claude/agents/`, Sonnet 5.5, esforço high)
- `executor`: escrever código e executar tarefas maçantes/básicas (boilerplate, models/DTOs, refatorações mecânicas, formatação, testes simples, correções da revisão).
- `pesquisador`: buscas e levantamentos somente-leitura em arquivos.

Toda delegação deve conter:
1. Escopo claro da tarefa.
2. Arquivos envolvidos.
3. Critério de pronto.

O subagente devolve um resumo curto do que foi feito — sem despejar código ou logs longos no contexto principal.

## Design Patterns (obrigatório)
- Todo artefato de especificação (proposal, specs, design, tasks do OpenSpec e docs/) deve seguir os Design Patterns citados no enunciado `Sistema de E-commerce.pdf`: pelo menos 6 patterns, de mais de uma categoria, com no mínimo 2 comportamentais e 1 de criação.
- Catálogo e justificativas em `docs/04-design-patterns.md`; as regras por artefato estão em `openspec/config.yaml`.
- Cada pattern deve resolver um problema real do enunciado e ter documentados problema, motivo da escolha, classes participantes e o que ficou mais fácil de modificar. Patterns artificiais são proibidos.
- Toda delegação de código a um subagente deve citar os patterns que a tarefa implementa e as classes participantes esperadas.

## Regras de uso
- Planejamento, decisões de arquitetura e revisão final ficam com o orquestrador.
- Se a tarefa for pequena demais para justificar um subagente, o orquestrador faz direto.
- Se a revisão encontrar problema, o orquestrador devolve a correção ao subagente (via SendMessage, mantendo o contexto) com instruções objetivas.
- Tarefas independentes podem ser delegadas em paralelo.
