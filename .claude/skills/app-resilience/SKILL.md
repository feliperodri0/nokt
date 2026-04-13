---
name: app-resilience
description: Tratamento de permissões críticas do sistema, sobrevivência a boot (BootReceiver) e isenção de otimização de bateria.
---

Fluxo de Permissões no Primeiro Acesso (First Launch):
O aplicativo NÃO PODE esperar o usuário realizar uma ação para pedir permissões. Assim que o aplicativo for aberto pela primeira vez (logo após a Splash Screen), ele DEVE solicitar um fluxo de onboarding de permissões para:
- POST_NOTIFICATIONS
- SCHEDULE_EXACT_ALARM
- FOREGROUND_SERVICE

Tratamento de Bateria:
O sistema deve solicitar ao usuário que isente o aplicativo da "Otimização de Bateria" do Android, garantindo que o gerenciador do sistema operacional não mate o serviço em segundo plano responsável pelas tarefas da tela de bloqueio.

Sobrevivência a Reboot:
O aplicativo deve utilizar um BroadcastReceiver escutando a intenção ACTION_BOOT_COMPLETED. Sempre que o celular for reiniciado, o sistema deve recalcular e reagendar silenciosamente todos os alarmes e atualizar os widgets ativos.
