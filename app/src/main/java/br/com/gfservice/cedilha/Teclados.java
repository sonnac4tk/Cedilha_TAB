package br.com.gfservice.cedilha;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.Collections;
import java.util.List;

/** Funções compartilhadas para saber qual teclado está ativo e para qual trocar. */
final class Teclados {

    /** Teclados virtuais preferidos para quando o teclado físico for desconectado, em ordem. */
    private static final String[] PREFERIDOS = {
            "com.google.android.inputmethod.latin", // Gboard
            "com.samsung.android.honeyboard",       // Teclado Samsung
            "com.touchtype.swiftkey",               // SwiftKey
    };

    private static final String PREFS = "teclados";
    private static final String CHAVE_ULTIMO_VIRTUAL = "ultimo_teclado_virtual";

    private Teclados() {
    }

    /** Há um teclado físico alfabético conectado e não oculto (mesma regra que o Android usa). */
    static boolean tecladoFisicoConectado(Context c) {
        Configuration cfg = c.getResources().getConfiguration();
        return cfg.keyboard == Configuration.KEYBOARD_QWERTY
                && cfg.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES;
    }

    static String idDesteTeclado(Context c) {
        return new ComponentName(c, TecladoFisicoIME.class).flattenToShortString();
    }

    /** ID do teclado atualmente selecionado no sistema, ou null se não for possível ler. */
    static String tecladoAtual(Context c) {
        try {
            return Settings.Secure.getString(c.getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static boolean desteTecladoAtivo(Context c) {
        return idDesteTeclado(c).equals(tecladoAtual(c));
    }

    static boolean desteTecladoHabilitado(Context c) {
        String nosso = idDesteTeclado(c);
        for (InputMethodInfo info : habilitados(c)) {
            if (nosso.equals(info.getId())) return true;
        }
        return false;
    }

    /** Guarda o teclado virtual que estava em uso, para voltar a ele depois. */
    static void lembrarTecladoVirtual(Context c, String id) {
        if (id == null || id.equals(idDesteTeclado(c))) return;
        prefs(c).edit().putString(CHAVE_ULTIMO_VIRTUAL, id).apply();
    }

    /**
     * Escolhe o teclado virtual para usar sem teclado físico: o último usado, senão um dos
     * preferidos, senão qualquer outro teclado habilitado. Retorna null se não houver nenhum.
     */
    static String tecladoVirtual(Context c) {
        String nosso = idDesteTeclado(c);
        List<InputMethodInfo> lista = habilitados(c);

        String ultimo = prefs(c).getString(CHAVE_ULTIMO_VIRTUAL, null);
        for (InputMethodInfo info : lista) {
            if (info.getId().equals(ultimo)) return ultimo;
        }
        for (String pacote : PREFERIDOS) {
            for (InputMethodInfo info : lista) {
                if (pacote.equals(info.getPackageName())) return info.getId();
            }
        }
        for (InputMethodInfo info : lista) {
            if (!nosso.equals(info.getId())) return info.getId();
        }
        return null;
    }

    static InputMethodManager imm(Context c) {
        return (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private static List<InputMethodInfo> habilitados(Context c) {
        InputMethodManager imm = imm(c);
        if (imm == null) return Collections.emptyList();
        List<InputMethodInfo> lista = imm.getEnabledInputMethodList();
        return lista != null ? lista : Collections.<InputMethodInfo>emptyList();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
