---
name: os-integration
description: Regras de negócio vitais para sincronização da tela de bloqueio, Foreground Services, Jetpack Glance Widgets e WorkManager.
---

Tela de Bloqueio (Notificações Ativas):
Tarefas cujo intervalo de tempo seja coincidente com o horário atual do sistema operacional devem aparecer na tela de bloqueio do celular. Implemente utilizando Foreground Services ou Notificações de Alta Prioridade com visibilidade configurada para aparecer na Lock Screen.
Regra de negócio: Se o relógio marca 11h, apenas a "Tarefa Y (11h-12h)" deve estar visível e fixada na tela de bloqueio. A "Tarefa X (08h-10h)" não deve aparecer.

App Widget:
O aplicativo deve expor um widget na tela inicial do Android utilizando Jetpack Glance. O widget deve seguir estritamente a mesma regra de negócio de visibilidade da tela de bloqueio (mostrar apenas tarefas do período vigente).

Atualização e Sincronização:
A sincronização e atualização de dados do Jetpack Glance Widget deve ser gerenciada pelo WorkManager, garantindo que o estado visual do widget reflita as mudanças no banco de dados sem drenar a bateria do dispositivo.

Ação de Descarte:
O usuário deve conseguir dispensar individualmente as tarefas que estão na tela de bloqueio ou no widget sem precisar abrir o aplicativo principal.
