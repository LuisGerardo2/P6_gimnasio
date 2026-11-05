package mx.tecnm.orizaba.tp.gimnasio.ui;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Estado;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Inscripcion;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Miembro;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

import javax.swing.*;
import java.awt.*;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tablero con indicadores y gráficas construidos a partir de los reportes del servicio. */
final class PanelResumen extends Vista {

    private final Gimnasio g;
    private final JLabel kActividades = Tema.etiqueta("0", Tema.F_KPI, Tema.TEXTO);
    private final JLabel kMiembros = Tema.etiqueta("0", Tema.F_KPI, Tema.TEXTO);
    private final JLabel kActivas = Tema.etiqueta("0", Tema.F_KPI, Tema.TEXTO);
    private final JLabel kIngresos = Tema.etiqueta("$0", Tema.F_KPI, Tema.TEXTO);
    private final Graficas.Barras barras = new Graficas.Barras();
    private final Graficas.Dona dona = new Graficas.Dona();
    private final JPanel ranking = new JPanel();
    private final JLabel masDemandado = Tema.etiqueta("—", Tema.F_NEGRITA, Tema.TEXTO);
    private final JLabel estadisticas = Tema.etiqueta(" ", Tema.F_PEQUENA, Tema.TEXTO_SUAVE);
    private final JLabel resumenCatalogo = Tema.etiqueta(" ", Tema.F_PEQUENA, Tema.TEXTO_SUAVE);

    PanelResumen(Gimnasio g) {
        super("Resumen", "Panorama general del gimnasio a partir de las inscripciones concluidas");
        this.g = g;

        JPanel kpis = new JPanel(new GridLayout(1, 4, 16, 0));
        kpis.setOpaque(false);
        kpis.add(kpi("Actividades", kActividades, Tema.PRIMARIO));
        kpis.add(kpi("Miembros", kMiembros, Tema.INFO));
        kpis.add(kpi("Inscripciones activas", kActivas, Tema.EXITO));
        kpis.add(kpi("Ingresos acumulados", kIngresos, Tema.AVISO));

        JPanel graf = new JPanel(new GridLayout(1, 2, 16, 0));
        graf.setOpaque(false);
        graf.add(conTitulo("Ingresos por tipo de actividad", barras));
        graf.add(conTitulo("Inscripciones por estado", dona));

        ranking.setOpaque(false);
        ranking.setLayout(new BoxLayout(ranking, BoxLayout.Y_AXIS));
        JPanel detalle = new JPanel(new GridLayout(1, 2, 16, 0));
        detalle.setOpaque(false);
        detalle.add(conTitulo("Mejores miembros", ranking));

        JPanel destacados = new JPanel();
        destacados.setOpaque(false);
        destacados.setLayout(new BoxLayout(destacados, BoxLayout.Y_AXIS));
        destacados.add(Tema.etiqueta("Actividad más demandada", Tema.F_PEQUENA, Tema.TEXTO_SUAVE));
        destacados.add(Box.createVerticalStrut(4));
        destacados.add(masDemandado);
        destacados.add(Box.createVerticalStrut(16));
        destacados.add(Tema.etiqueta("Estadísticas de inscripciones concluidas", Tema.F_PEQUENA, Tema.TEXTO_SUAVE));
        destacados.add(Box.createVerticalStrut(4));
        destacados.add(estadisticas);
        destacados.add(Box.createVerticalStrut(16));
        destacados.add(Tema.etiqueta("Catálogo", Tema.F_PEQUENA, Tema.TEXTO_SUAVE));
        destacados.add(Box.createVerticalStrut(4));
        destacados.add(resumenCatalogo);
        detalle.add(conTitulo("Indicadores destacados", destacados));

        JPanel centro = new JPanel(new BorderLayout(0, 16));
        centro.setOpaque(false);
        JPanel arriba = new JPanel(new BorderLayout(0, 16));
        arriba.setOpaque(false);
        arriba.add(kpis, BorderLayout.NORTH);
        arriba.add(graf, BorderLayout.CENTER);
        centro.add(arriba, BorderLayout.CENTER);
        detalle.setPreferredSize(new Dimension(100, 225));
        centro.add(detalle, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        refrescar();
    }

    private static String bonito(String tipo) {
        return switch (tipo) {
            case "ClaseGrupal" -> "Clase grupal";
            case "AccesoLibre" -> "Acceso libre";
            default -> tipo;
        };
    }

    private static JPanel kpi(String titulo, JLabel valor, Color acento) {
        Tema.Tarjeta t = new Tema.Tarjeta(new BorderLayout(0, 6));
        JLabel barra = new JLabel(" ");
        barra.setOpaque(true);
        barra.setBackground(acento);
        barra.setPreferredSize(new Dimension(34, 4));
        JPanel cab = new JPanel(new BorderLayout(0, 10));
        cab.setOpaque(false);
        JPanel marco = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        marco.setOpaque(false);
        marco.add(barra);
        cab.add(marco, BorderLayout.NORTH);
        cab.add(Tema.etiqueta(titulo, Tema.F_BASE, Tema.TEXTO_SUAVE), BorderLayout.SOUTH);
        t.add(cab, BorderLayout.NORTH);
        t.add(valor, BorderLayout.CENTER);
        return t;
    }

    private static JPanel conTitulo(String titulo, JComponent contenido) {
        Tema.Tarjeta t = new Tema.Tarjeta(new BorderLayout(0, 10));
        t.add(Tema.etiqueta(titulo, Tema.F_SUBTITULO, Tema.TEXTO), BorderLayout.NORTH);
        t.add(contenido, BorderLayout.CENTER);
        return t;
    }

    @Override
    void refrescar() {
        List<Inscripcion> todos = g.registros();
        kActividades.setText(String.valueOf(g.catalogo().tamanio()));
        kMiembros.setText(String.valueOf(g.miembros().size()));
        kActivas.setText(String.valueOf(g.registrosEn(Estado.EN_CURSO).size()));

        DoubleSummaryStatistics st = g.estadisticas();
        kIngresos.setText(Util.dinero(st.getSum()));
        estadisticas.setText(st.getCount() == 0 ? "Aún no hay inscripciones concluidas"
                : "<html>%d concluidas · promedio %s<br>mínimo %s · máximo %s</html>".formatted(
                st.getCount(), Util.dinero(st.getAverage()), Util.dinero(st.getMin()), Util.dinero(st.getMax())));
        resumenCatalogo.setText("<html>" + g.catalogo().resumen() + "</html>");

        Map<String, Double> porTipo = new LinkedHashMap<>();
        g.ingresosPorTipo().forEach((k, v) -> porTipo.put(bonito(k), v));
        barras.datos(porTipo);

        Map<String, Long> porEstado = new LinkedHashMap<>();
        Map<String, Color> colores = new LinkedHashMap<>();
        Map<Estado, Long> conteo = g.conteoPorEstado();
        for (Estado e : Estado.values()) {
            long n = conteo.getOrDefault(e, 0L);
            if (n > 0) {
                porEstado.put(e.etiqueta(), n);
                colores.put(e.etiqueta(), Util.colorEstado(e));
            }
        }
        dona.datos(porEstado, colores);

        masDemandado.setText(g.masDemandado().map(Actividad::nombre).orElse("— (sin datos)"));

        ranking.removeAll();
        List<Miembro> mejores = g.mejores(5);
        if (mejores.isEmpty()) {
            ranking.add(Tema.etiqueta("Sin datos de inscripciones concluidas", Tema.F_BASE, Tema.TEXTO_SUAVE));
        }
        int pos = 1;
        for (Miembro m : mejores) {
            double total = todos.stream().filter(i -> i.estado() == Estado.CERRADA && i.miembro().equals(m))
                    .mapToDouble(Inscripcion::total).sum();
            JPanel fila = new JPanel(new BorderLayout(10, 0));
            fila.setOpaque(false);
            fila.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            fila.add(Tema.etiqueta(pos++ + ".", Tema.F_NEGRITA, Tema.PRIMARIO), BorderLayout.WEST);
            fila.add(Tema.etiqueta(m.nombre() + "  ·  " + m.nivel().etiqueta(), Tema.F_BASE, Tema.TEXTO), BorderLayout.CENTER);
            fila.add(Tema.etiqueta(Util.dinero(total), Tema.F_NEGRITA, Tema.TEXTO), BorderLayout.EAST);
            ranking.add(Tema.acotar(fila));
        }
        ranking.add(Box.createVerticalGlue());
        ranking.revalidate();
        revalidate();
        repaint();
    }
}
