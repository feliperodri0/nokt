---
name: android-tdd
description: Regras estritas de Test-Driven Development exigidas. Acione esta skill antes de iniciar a implementação de qualquer feature no projeto.
---

Você deve operar estritamente sob o modelo Test-Driven Development para o aplicativo de gerenciamento de tarefas.

1. Escreva os testes unitários da camada de domínio (Use Cases) e os testes de integração/UI da feature atual.
2. Implemente a feature para fazer os testes passarem.
3. Crie testes de segurança também para garantir a confiabilidade da camada.
4. Garanta que a arquitetura MVI (Intents e States) esteja coberta por testes.
5. A feature só é considerada completa quando todos os testes de todos os tipos passarem. Você só deve avançar para o próximo requisito da lista após o requisito atual estar 100% verde.
