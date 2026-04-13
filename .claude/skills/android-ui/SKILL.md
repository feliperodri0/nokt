---
name: android-ui
description: Regras para a tela principal (somente hoje), menu lateral (Sidebar/Drawer), visão de calendário isolada, splash screen e edição de tarefas.
---

Navegação e Estrutura (Sidebar):
A navegação principal do app deve utilizar um Menu de Barra Lateral (Navigation Drawer). A tela principal NÃO deve conter o calendário. O calendário deve ser acessado exclusivamente via barra lateral.

Tela Inicial (Home):
Deve exibir EXCLUSIVAMENTE as tarefas do dia atual.
Deve possuir um Floating Action Button (FAB) para criar novas tarefas.
Se necessário, inicie o app com uma tela de Loading (Splash Screen) enquanto os dados locais e permissões são carregados.

Visão de Calendário (Isolada):
A tela de calendário deve ter um design limpo e profissional, semelhante ao Google Calendar.
Deve exibir o Mês/Ano no topo e um grid claro com os dias da semana.
A visão de calendário deve ter a visualização máxima de 5 anos a frente do atual, e 5 anos atrás do atual. O usuário deve ser capaz de navegar facilmente entre os meses e anos.
A tela de calendário deve mostrar um indicador visual (como um ponto ou cor diferente) nos dias que possuem tarefas agendadas, e ao clicar no dia, deve mostrar uma lista das tarefas daquele dia específico.

Interação e Detalhes da Tarefa:
O usuário deve obrigatoriamente conseguir clicar em uma tarefa listada (seja na Home ou no Calendário) para abrir a tela de "Detalhes da Tarefa".
A tela de detalhes deve permitir a edição completa e visualização de todos os atributos da tarefa.

Resiliência de UI (Prevenção de Crash):
Para evitar o erro "Standalone Coroutine was cancelled" e problemas de renderização (onde a tarefa não aparece a menos que clique várias vezes), o gerenciamento de estado no Compose DEVE usar `collectAsStateWithLifecycle()`. Nunca use coroutines soltas ou escopos globais na UI.