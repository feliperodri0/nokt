---
name: android-security
description: Regras fundamentais sobre manipulação segura de chaves com EncryptedSharedPreferences e clonagem de anexos para o Context.filesDir.
---

Segurança em Profundidade: Cada camada deve ser independentemente segura. Utilize o EncryptedSharedPreferences da biblioteca AndroidX Security caso precise salvar chaves, tokens ou preferências sensíveis.

Manipulação de Anexos:
Quando o usuário anexar um documento ou imagem, o sistema deve obrigatoriamente copiar o arquivo original através do seu InputStream para o diretório de armazenamento interno privado do aplicativo (Context.filesDir). O banco de dados Room deve armazenar apenas o nome do arquivo interno salvo, garantindo a persistência do anexo.
Os arquivos anexados devem ser acessados utilizando URIs via FileProvider ou Storage Access Framework, evitando expor caminhos reais do sistema.
