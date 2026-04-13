---
name: android-arch 
description: Regras estritas sobre a stack tecnológica, Clean Architecture, MVI e as bibliotecas exigidas no projeto.
---

A arquitetura base do aplicativo deve seguir a Clean Architecture, dividida nas camadas Data, Domain e Presentation. A camada de apresentação deve ser obrigatoriamente orientada a MVI (Model-View-Intent).

| Categoria | Tecnologia Exigida |
| :--- | :--- |
| Linguagem | Kotlin |
| Interface (UI) | Jetpack Compose |
| Widgets | Jetpack Glance |
| Injeção de Dependência | Hilt |
| Banco de Dados Local | Room Database |
| Assincronismo/Reatividade | Kotlin Coroutines e StateFlow/SharedFlow |
| Testes | JUnit4, MockK e Compose Test Rule |
