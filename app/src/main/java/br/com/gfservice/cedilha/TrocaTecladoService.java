package br.com.gfservice.cedilha;

import android.accessibilityservice.AccessibilityService;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

/**
 * Opcional (Android 11+): troca sozinho para o teclado Cedilha quando o teclado físico é
 * conectado, e de volta para o teclado virtual quando ele é desconectado.
 *
 * Não lê nada do que é digitado nem o conteúdo da tela; só observa a conexão de teclados.
 */
public class TrocaTecladoService extends AccessibilityService {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ajustar = this::ajustarTeclado;

    private final InputManager.InputDeviceListener ouvinte =
            new InputManager.InputDeviceListener() {
                @Override
                public void onInputDeviceAdded(int deviceId) {
                    agendar();
                }

                @Override
                public void onInputDeviceRemoved(int deviceId) {
                    agendar();
                }

                @Override
                public void onInputDeviceChanged(int deviceId) {
                    agendar();
                }
            };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        InputManager im = (InputManager) getSystemService(INPUT_SERVICE);
        if (im != null) im.registerInputDeviceListener(ouvinte, handler);
        agendar();
    }

    @Override
    public void onConfigurationChanged(Configuration novaConfig) {
        super.onConfigurationChanged(novaConfig);
        agendar();
    }

    @Override
    public void onDestroy() {
        InputManager im = (InputManager) getSystemService(INPUT_SERVICE);
        if (im != null) im.unregisterInputDeviceListener(ouvinte);
        handler.removeCallbacks(ajustar);
        super.onDestroy();
    }

    /** A configuração do sistema demora um pouco para refletir a conexão do teclado. */
    private void agendar() {
        handler.removeCallbacks(ajustar);
        handler.postDelayed(ajustar, 700);
    }

    private void ajustarTeclado() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
        if (!Teclados.desteTecladoHabilitado(this)) return;

        String atual = Teclados.tecladoAtual(this);
        String nosso = Teclados.idDesteTeclado(this);
        boolean fisico = Teclados.tecladoFisicoConectado(this);

        if (fisico && !nosso.equals(atual)) {
            Teclados.lembrarTecladoVirtual(this, atual);
            trocarPara(nosso);
        } else if (!fisico && nosso.equals(atual)) {
            String virtual = Teclados.tecladoVirtual(this);
            if (virtual != null) trocarPara(virtual);
        }
    }

    private void trocarPara(String id) {
        try {
            getSoftKeyboardController().switchToInputMethod(id);
        } catch (RuntimeException ignorada) {
            // Sem permissão ou teclado indisponível: o usuário troca manualmente.
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}
