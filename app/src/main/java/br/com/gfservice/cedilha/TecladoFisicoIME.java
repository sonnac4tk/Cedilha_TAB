package br.com.gfservice.cedilha;

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Teclado (IME) só para teclado físico, com acentos no estilo "EUA Internacional" do Windows:
 * ' + c = ç e ' + C = Ç; os demais acentos seguem a regra normal (' + e = é, ~ + a = ã ...).
 *
 * As teclas comuns passam direto para o app. Só a tecla de acento (tecla morta) e a tecla
 * seguinte são tratadas aqui. Sem teclado físico, troca para o teclado virtual (Gboard etc.).
 */
public class TecladoFisicoIME extends InputMethodService {

    /** Acento aguardando a próxima tecla (caractere de exibição do KeyCharacterMap), ou 0. */
    private int acentoPendente;

    /** Teclas cujo "apertar" foi consumido aqui; o "soltar" também precisa ser consumido. */
    private final Set<Integer> teclasConsumidas = new HashSet<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean trocaAgendada;

    // ---------------------------------------------------------------- troca de teclado

    @Override
    public boolean onEvaluateInputViewShown() {
        super.onEvaluateInputViewShown();
        // Este teclado não tem tela. Sem teclado físico, passa a vez ao teclado virtual.
        if (!Teclados.tecladoFisicoConectado(this)) agendarTrocaParaVirtual();
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration novaConfig) {
        super.onConfigurationChanged(novaConfig);
        if (!Teclados.tecladoFisicoConectado(this)) agendarTrocaParaVirtual();
    }

    private void agendarTrocaParaVirtual() {
        if (trocaAgendada) return;
        trocaAgendada = true;
        handler.post(() -> {
            trocaAgendada = false;
            trocarParaVirtual();
        });
    }

    private void trocarParaVirtual() {
        if (Teclados.tecladoFisicoConectado(this)) return;
        String id = Teclados.tecladoVirtual(this);
        if (id != null) {
            try {
                switchInputMethod(id);
                return;
            } catch (RuntimeException ignorada) {
                // Teclado desinstalado ou desabilitado: tenta as alternativas abaixo.
            }
        }
        if (!switchToPreviousInputMethod()) {
            InputMethodManager imm = Teclados.imm(this);
            if (imm != null) imm.showInputMethodPicker();
        }
    }

    // ---------------------------------------------------------------- acentos

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        acentoPendente = 0;
    }

    @Override
    public void onFinishInput() {
        cancelarAcento(getCurrentInputConnection());
        super.onFinishInput();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Shift, AltGr etc. não interrompem o acento (ex.: ' depois Shift+C = Ç).
        if (KeyEvent.isModifierKey(keyCode)) return super.onKeyDown(keyCode, event);

        // Repetição de uma tecla que já tratamos (tecla segurada): ignora.
        if (event.getRepeatCount() > 0 && teclasConsumidas.contains(keyCode)) return true;

        InputConnection ic = getCurrentInputConnection();
        if (ic == null || event.isCtrlPressed() || event.isMetaPressed()) {
            cancelarAcento(ic);
            return super.onKeyDown(keyCode, event);
        }

        int c = event.getUnicodeChar(event.getMetaState());

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            int acento = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
            if (acentoPendente != 0) {
                // Dois acentos seguidos: o mesmo acento duas vezes vira o próprio acento;
                // acentos diferentes escrevem o primeiro e deixam o segundo pendente.
                boolean mesmo = acentoPendente == acento;
                ic.commitText(texto(visivel(acentoPendente)), 1);
                acentoPendente = 0;
                if (mesmo) {
                    teclasConsumidas.add(keyCode);
                    return true;
                }
            }
            acentoPendente = acento;
            ic.setComposingText(texto(visivel(acento)), 1);
            teclasConsumidas.add(keyCode);
            return true;
        }

        if (acentoPendente == 0) return super.onKeyDown(keyCode, event);

        if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            // Apaga só o acento pendente, sem apagar o caractere anterior.
            cancelarAcento(ic);
            teclasConsumidas.add(keyCode);
            return true;
        }

        if (c == 0 || Character.isISOControl(c)) {
            // Enter, Tab, setas...: descarta o acento e deixa a tecla seguir.
            cancelarAcento(ic);
            return super.onKeyDown(keyCode, event);
        }

        int combinado = combinar(acentoPendente, c);
        String saida = combinado != 0
                ? texto(combinado)
                : texto(visivel(acentoPendente)) + texto(c); // ex.: ' + t = 't
        acentoPendente = 0;
        ic.commitText(saida, 1);
        teclasConsumidas.add(keyCode);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (teclasConsumidas.remove(keyCode)) return true;
        return super.onKeyUp(keyCode, event);
    }

    private void cancelarAcento(InputConnection ic) {
        if (acentoPendente == 0) return;
        acentoPendente = 0;
        if (ic != null) {
            ic.setComposingText("", 1);
            ic.finishComposingText();
        }
    }

    /** Junta acento + letra. Diferença em relação ao Android: acento agudo + c dá ç, não ć. */
    static int combinar(int acento, int c) {
        if (acento == '\'' || acento == '´') {
            if (c == 'c') return 'ç'; // ç
            if (c == 'C') return 'Ç'; // Ç
        }
        if (c == ' ') return visivel(acento);
        int r = KeyCharacterMap.getDeadChar(acento, c);
        return r == acento ? visivel(acento) : r;
    }

    /** Caractere que aparece ao digitar o acento sozinho, como no Windows (` ^ ~ em vez de ˋ ˆ ˜). */
    static int visivel(int acento) {
        switch (acento) {
            case 'ˋ': return '`';
            case 'ˆ': return '^';
            case '˜': return '~';
            default: return acento;
        }
    }

    private static String texto(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}
