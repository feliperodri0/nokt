---
name: task-domain
description: Modelagem de entidades do app, validações de Time/All Day, detalhes opcionais e mecânica do sistema de recorrência estruturado.
---

O modelo de Tarefa deve conter obrigatoriamente: Título, Data e um campo de Tempo.
O campo de Tempo possui dois estados possíveis: "O dia todo" ou um intervalo específico.
Detalhes Opcionais permitidos na entidade: descrição em texto, lista de imagens anexadas, anexos de documentos e checklist.

Sistema de recorrência estruturado via Sealed Class ou Enum:
- Única: Ocorre apenas em data específica. Validação: a data deve estar no intervalo entre o dia atual e limite máximo de exatos 5 anos no futuro.
- Diária: A tarefa se repete todos os dias da semana.
- Semanal Customizada: A tarefa se repete em múltiplos dias específicos (granularidade escolhida pelo usuário).

Persistência no Room:
Para suportar a periodicidade Semanal Customizada, a entidade deve utilizar TypeConverters para serializar/desserializar a lista de dias da semana para String JSON e vice-versa.
