package br.com.gfservice.cedilha;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;

/** Tela de configuração: mostra o estado e leva para as telas do sistema. */
public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int margem = dp(16);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(margem, margem, margem, margem);

        TextView titulo = new TextView(this);
        titulo.setText(R.string.app_name);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        raiz.addView(titulo);

        TextView explicacao = new TextView(this);
        explicacao.setText(R.string.explicacao);
        explicacao.setPadding(0, dp(8), 0, dp(8));
        raiz.addView(explicacao);

        status = new TextView(this);
        status.setPadding(0, dp(8), 0, dp(16));
        raiz.addView(status);

        botao(raiz, R.string.passo_habilitar, new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
        botao(raiz, R.string.passo_layout, new Intent("android.settings.HARD_KEYBOARD_SETTINGS"));
        botao(raiz, R.string.passo_troca, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: apps instalados por APK precisam liberar as "configurações restritas"
            // nas informações do app (⋮ → Permitir configurações restritas).
            botao(raiz, R.string.liberar_restritas, new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null)));
        }

        Button escolher = new Button(this);
        escolher.setText(R.string.escolher_agora);
        escolher.setOnClickListener(v -> {
            InputMethodManager imm = Teclados.imm(this);
            if (imm != null) imm.showInputMethodPicker();
        });
        raiz.addView(escolher);

        EditText teste = new EditText(this);
        teste.setHint(R.string.campo_teste);
        raiz.addView(teste, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView rolagem = new ScrollView(this);
        rolagem.addView(raiz);
        setContentView(rolagem);
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarStatus();
    }

    @Override
    public void onWindowFocusChanged(boolean temFoco) {
        super.onWindowFocusChanged(temFoco);
        if (temFoco) atualizarStatus(); // depois de fechar o seletor de teclado
    }

    private void atualizarStatus() {
        status.setText(getString(R.string.status,
                simNao(Teclados.desteTecladoHabilitado(this)),
                simNao(Teclados.desteTecladoAtivo(this)),
                simNao(Teclados.tecladoFisicoConectado(this)),
                simNao(trocaAutomaticaLigada())));
    }

    private boolean trocaAutomaticaLigada() {
        String ligados = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (ligados == null) return false;
        ComponentName nosso = new ComponentName(this, TrocaTecladoService.class);
        for (String s : ligados.split(":")) {
            if (nosso.equals(ComponentName.unflattenFromString(s))) return true;
        }
        return false;
    }

    private String simNao(boolean v) {
        return getString(v ? R.string.sim : R.string.nao);
    }

    private void botao(LinearLayout raiz, int texto, Intent intent) {
        Button b = new Button(this);
        b.setText(texto);
        b.setOnClickListener(v -> {
            try {
                startActivity(intent);
            } catch (RuntimeException e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        raiz.addView(b);
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
