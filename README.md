# Cedilha

Teclado Android para **teclado físico** que faz `'` + `c` = **ç**, como no layout
"EUA Internacional" do Windows. No Android, a mesma sequência gera **ć**, porque o sistema
junta o acento agudo com a letra seguindo a regra fixa do Unicode, e nenhum layout de teclado
consegue mudar isso.

## Como funciona

- **Teclado Cedilha** (`TecladoFisicoIME`): um teclado (IME) sem tela, feito só para o teclado
  físico. Ele trata as teclas de acento (`'`, `"`, `` ` ``, `~`, `^`) como no Windows:
  - `'` + `c` → `ç` e `'` + `C` → `Ç`
  - `'` + `e` → `é`, `~` + `a` → `ã`, `^` + `o` → `ô`, `"` + `u` → `ü`, `` ` `` + `a` → `à`
  - acento + espaço → o próprio acento; acento + letra sem acento → os dois (`'` + `t` → `'t`)
  - Backspace/Esc logo depois do acento cancelam só o acento

  As outras teclas e atalhos (Ctrl+C, setas etc.) passam direto para o app.
- **Sem teclado físico**, o teclado Cedilha passa a vez sozinho para o teclado virtual
  (o último usado, ou Gboard, Samsung ou SwiftKey).
- **Troca automática** (`TrocaTecladoService`, opcional, Android 11+): quando o teclado
  físico é conectado, volta para o teclado Cedilha. Usa o serviço de acessibilidade só para
  trocar de teclado: **não lê o que você digita nem o conteúdo da tela**.

## Instalação

1. Baixe o APK em **Actions → Gerar APK → (última execução) → Artifacts → Cedilha-apk** e instale.
2. Abra o app **Cedilha** e siga os botões:
   1. **Habilitar o teclado Cedilha** (em Teclados na tela).
   2. **Layout do teclado físico** → escolha **Inglês (EUA), Internacional**.
      No Android 14+ ele já costuma vir selecionado.
   3. **Ligar a troca automática** (opcional) → Acessibilidade → *Cedilha – troca automática*.
      No Android 13+, se a opção estiver bloqueada ("configuração restrita"), vá em
      *Configurações → Apps → Cedilha → ⋮ → Permitir configurações restritas* e tente de novo.
3. Com o teclado físico conectado, toque em **Escolher teclado agora** e selecione
   *Cedilha (teclado físico)*. Use o campo de teste para conferir.

Sem a troca automática, ao reconectar o teclado físico, troque manualmente para o Cedilha
pelo ícone de teclado na barra de navegação (ou com Ctrl+Espaço).

Requisitos: Android 9 ou superior.

## Compilação

O GitHub Actions (`.github/workflows/build.yml`) gera o APK de debug a cada push.
Localmente: `gradle assembleDebug`, com JDK 17, Gradle 8.7 e Android SDK 34.
