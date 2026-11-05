package mx.tecnm.orizaba.tp.gimnasio.ui;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reloj inyectado al servicio. Permite adelantar días para demostrar las alertas de renovación
 * y la cancelación automática sin esperar al calendario real.
 */
public final class RelojSimulado {

    private final AtomicInteger desplazamiento = new AtomicInteger();

    public LocalDate hoy() {
        return LocalDate.now().plusDays(desplazamiento.get());
    }

    public void avanzar(int dias) {
        desplazamiento.addAndGet(dias);
    }

    public void reiniciar() {
        desplazamiento.set(0);
    }

    public int dias() {
        return desplazamiento.get();
    }
}
