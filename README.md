# nokt

Aplicativo Android de anotações e tarefas, com lembretes, recorrência, widget de tela inicial e sincronização com a tela de bloqueio.

## Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Clean Architecture** (`data` / `domain` / `presentation`) com padrão **MVI**
- **Hilt** para injeção de dependência
- **Room** para persistência local
- **WorkManager** para tarefas em segundo plano e agendamentos
- **Jetpack Glance** para o widget de tela inicial
- **DataStore Preferences** para configurações
- **EncryptedSharedPreferences** para dados sensíveis
- **Navigation Compose** para navegação
- **Coil** para carregamento de imagens
- Testes com **JUnit**, **MockK**, **Truth** e **Espresso**

## Estrutura do projeto

```
app/src/main/java/br/com/anotacoes/
├── data/            # Implementações concretas (db, repository, alarm, notification, icon, settings)
├── domain/           # Modelos, casos de uso, portas e contratos de repositório
├── presentation/      # Telas Compose, componentes e tema (MVI)
├── receiver/         # BroadcastReceivers (boot, alarmes)
├── service/          # Foreground services (ex.: tela de bloqueio)
├── widget/           # Widget Jetpack Glance
└── di/               # Módulos Hilt
```

## Funcionalidades principais

- Criação e edição de tarefas com data/hora, dia inteiro e detalhes opcionais
- Recorrência estruturada (diária, semanal, mensal) com correção de agendamento mensal
- Lembretes antecipados, com notificação via serviço de tela de bloqueio
- Visão de calendário isolada e tela principal focada no dia atual
- Widget de tela inicial (Jetpack Glance)
- Sobrevivência a boot (`BootReceiver`) e isenção de otimização de bateria
- Splash screen e menu lateral (Sidebar/Drawer)

## Requisitos

- Android Studio (Ladybug ou superior recomendado)
- JDK 17
- Android SDK: `compileSdk` 34, `minSdk` 26

## Como rodar

1. Clone o repositório.
2. Abra o projeto no Android Studio e aguarde a sincronização do Gradle.
3. Rode em um emulador ou dispositivo físico (Android 8.0+).

Ou via linha de comando:

```bash
./gradlew assembleDebug
```

## Testes

```bash
# Testes unitários
./gradlew test

# Testes instrumentados
./gradlew connectedAndroidTest
```

## Ambiente Docker (build headless)

O `docker-compose.yml` sobe um container com JDK 17 para builds fora do Android Studio, usando o SDK Android local montado como volume:

```bash
USER_ID=$(id -u) GROUP_ID=$(id -g) docker compose run --rm android-dev
```

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE) para mais detalhes.
