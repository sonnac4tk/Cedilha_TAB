package br.com.gfservice.cedilha;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Troca ć -> ç e Ć -> Ç no campo de texto em que o usuário está digitando.
 * No Android, o acento agudo + c do layout internacional gera ć; este serviço corrige na hora.
 */
public class CedilhaService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return;

        AccessibilityNodeInfo campo = event.getSource();
        if (campo == null || !campo.isEditable() || campo.isPassword()) return;

        CharSequence texto = campo.getText();
        if (texto == null) return;

        String atual = texto.toString();
        if (atual.indexOf('ć') < 0 && atual.indexOf('Ć') < 0) return;

        int cursor = campo.getTextSelectionEnd();
        String corrigido = atual.replace('ć', 'ç').replace('Ć', 'Ç');

        Bundle argTexto = new Bundle();
        argTexto.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, corrigido);

        if (campo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, argTexto) && cursor >= 0) {
            // Mesmo número de caracteres: o cursor volta para a posição original.
            Bundle argCursor = new Bundle();
            argCursor.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor);
            argCursor.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor);
            campo.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, argCursor);
        }
    }

    @Override
    public void onInterrupt() {
    }
}
