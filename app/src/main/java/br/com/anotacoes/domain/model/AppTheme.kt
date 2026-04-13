package br.com.anotacoes.domain.model

enum class AppTheme(val displayName: String, val description: String) {
    SYSTEM("Padrão do sistema", "Adapta ao claro/escuro do celular (Ícone Verde)"),
    VERDE_CLARO("Verde Claro", "Tema claro com ícone verde"),
    VERDE_ESCURO("Verde Escuro", "Tema escuro com ícone verde"),
    ROSA_CLARO("Rosa Claro", "Tema claro com ícone rosa"),
    ROSA_ESCURO("Rosa Escuro", "Tema escuro com ícone rosa"),
    VITORIA_CLARO("Vitória Claro", "Tema claro pastel"),
    VITORIA_ESCURO("Vitória Escuro", "Tema escuro pastel"),
    OCEANO_CLARO("Oceano Claro", "Tema claro azul oceano"),
    OCEANO_ESCURO("Oceano Escuro", "Tema escuro azul oceano"),
    ROXO_CLARO("Roxo Claro", "Tema claro roxo profundo"),
    ROXO_ESCURO("Roxo Escuro", "Tema escuro roxo profundo")
}