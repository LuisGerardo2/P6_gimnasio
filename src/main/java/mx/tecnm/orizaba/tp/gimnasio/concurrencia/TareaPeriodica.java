package mx.tecnm.orizaba.tp.gimnasio.concurrencia;

/**
 * Tarea que se ejecuta periódicamente en su propio hilo.
 * <ul>
 *   <li>Implementa {@link Runnable} en lugar de extender {@link Thread}
 *       (véase {@code java_4_modelo/t6_concurrencia/EjemploHilos}).</li>
 *   <li>Detención cooperativa: bandera {@code volatile} + {@code interrupt()} + {@code join()}.</li>
 *   <li>Template Method: {@link #run()} es final; la variación está en {@link #ejecutarCiclo()}.</li>
 * </ul>
 */
public abstract class TareaPeriodica implements Runnable {

    private final long periodoMs;
    private volatile boolean activa;
    private Thread hilo;
    private volatile long ciclos;

    protected TareaPeriodica(long periodoMs) {
        if (periodoMs <= 0) {
            throw new IllegalArgumentException("Periodo inválido: " + periodoMs);
        }
        this.periodoMs = periodoMs;
    }

    /** Arranca el hilo (demonio) con el nombre indicado. */
    public final synchronized void iniciar(String nombreHilo) {
        // TODO E12: Si ya está activa lanzar IllegalStateException; crear new Thread(this, nombreHilo), marcarlo demonio, activar y start().
        if(activa()){throw new IllegalStateException("Tarea periodica 29");}
        hilo=new Thread(this,nombreHilo);
        hilo.setDaemon(true);
        activa=true;
        hilo.start();
    }

    /** Solicita la detención y espera a que el hilo termine. */
    public final void detener() throws InterruptedException {

        activa=false;
        Thread h;
        synchronized (this) {
            h = hilo;
        }
        if(h != null){
            h.interrupt();
            h.join();
        }
        // HECHO TODO E12: Bajar la bandera, interrumpir el hilo y esperar con join() (si existe).

    }

    public final boolean activa() {
        return activa;
    }

    public final long ciclos() {
        return ciclos;
    }

    @Override
    public final void run() {
        try {
            while (activa) {
                try {
                    ejecutarCiclo();
                } catch (RuntimeException e) {
                    // Un fallo en un ciclo no debe matar al monitor.
                    System.err.println("Error en el ciclo periódico: " + e);
                }
                ciclos++;
                try {
                    Thread.sleep(periodoMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            activa = false;
        }
        //HECHO  TODO E12: Mientras activa: ejecutarCiclo(), incrementar ciclos y dormir periodoMs; ante InterruptedException restaurar la interrupción y salir.

    }

    /** Trabajo de un ciclo. */
    protected abstract void ejecutarCiclo();
}
