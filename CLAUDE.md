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

## Regras de uso
- Planejamento, decisões de arquitetura e revisão final ficam com o orquestrador.
- Se a tarefa for pequena demais para justificar um subagente, o orquestrador faz direto.
- Se a revisão encontrar problema, o orquestrador devolve a correção ao subagente (via SendMessage, mantendo o contexto) com instruções objetivas.
- Tarefas independentes podem ser delegadas em paralelo.
