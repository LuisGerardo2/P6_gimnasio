package mx.tecnm.orizaba.tp.gimnasio.modelo;

/**
 * Ciclo de vida de un(a) inscripción: máquina de estados finita.
 * <pre>ABIERTA → EN_CURSO → CERRADA;  ABIERTA|EN_CURSO → CANCELADA</pre>
 */
public enum Estado {
    ABIERTA("Pendiente"), EN_CURSO("Activa"), CERRADA("Concluida"), CANCELADA("Cancelada");

    private final String etiqueta;

    Estado(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Indica si la transición hacia {@code destino} es válida (switch como expresión). */
    public boolean permiteTransicionA(Estado destino) {

        return switch (this) {
            case ABIERTA -> destino == EN_CURSO || destino == CANCELADA;
            case EN_CURSO -> destino == CERRADA || destino == CANCELADA;
            case CERRADA, CANCELADA -> false; // Estados terminales no permiten transiciones
        };


        // TODO E07: Implementar con switch-expresión las transiciones del diagrama; los estados terminales no permiten ninguna.

    }

    public boolean esTerminal() {
        return this == CERRADA || this == CANCELADA;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
