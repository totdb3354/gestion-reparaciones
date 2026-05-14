package com.reparaciones.controllers;

import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.dao.ReparacionDAO;
import com.reparaciones.dao.TecnicoDAO;
import com.reparaciones.models.Componente;
import com.reparaciones.models.PuntoEstadistica;
import com.reparaciones.models.Tecnico;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador de la vista de estadísticas.
 * <p>Muestra un gráfico de líneas ({@code LineChart}) con las reparaciones finalizadas
 * por técnico a lo largo del tiempo, con granularidades: día, semana, mes y año.</p>
 *
 * <p><b>Funcionalidades principales:</b></p>
 * <ul>
 *   <li>Checkbox "Todos" — línea negra con la suma de todos los técnicos.</li>
 *   <li>Checkbox "Actividad" — muestra/oculta las líneas continuas de datos.</li>
 *   <li>Checkbox "Media" — muestra/oculta las líneas de media discontinuas por técnico.</li>
 *   <li>Línea de referencia (gris punteada) — media del total / nTécnicos, para evaluar
 *       la performance individual respecto al benchmark del equipo.</li>
 *   <li>Ventana deslizante (slider) — limita los puntos visibles en periodos con muchos datos.</li>
 *   <li>Clic en vértice — navega al historial de reparaciones con el filtro de fecha y
 *       técnico pre-aplicado, vía callback {@link com.reparaciones.utils.Navegable}.</li>
 *   <li>Los técnicos inactivos no tienen línea individual pero sí cuentan en "Todos".</li>
 * </ul>
 *
 * <p>El doble {@code Platform.runLater} en el flujo de renderizado garantiza que el eje Y
 * y las líneas de media se dibujen con las coordenadas correctas tras los dos pases de layout.</p>
 *
 * @role ADMIN
 */
public class EstadisticasController implements com.reparaciones.utils.Recargable {

    @FXML private Button   btnTabReparaciones;
    @FXML private Button   btnTabStock;

    @FXML private VBox     pnlReparaciones;
    @FXML private VBox     pnlStock;

    @FXML private ComboBox<String> cmbGranularidad;
    @FXML private DatePicker       dpDesde;
    @FXML private DatePicker       dpHasta;
    @FXML private LineChart<String, Number> chartReparaciones;
    @FXML private CategoryAxis     ejeX;
    @FXML private NumberAxis       ejeY;
    @FXML private MenuButton       menuTecnicos;
    @FXML private CheckBox         chkTodos;
    @FXML private CheckBox         chkMedia;
    @FXML private CheckBox         chkActividad;
    @FXML private Label            lblSinDatos;
    @FXML private HBox             hboxSlider;
    @FXML private Slider           sliderVentana;
    @FXML private Label            lblSliderDesde;
    @FXML private Label            lblSliderHasta;

    // nombre → CheckBox  (orden de inserción = orden de la BD)
    private final Map<String, CheckBox> checksPorNombre  = new LinkedHashMap<>();
    // nombre → color hex fijo por ID_TEC
    private final Map<String, String>   coloresPorNombre = new LinkedHashMap<>();
    // Serie actualmente resaltada (null = ninguna)
    private XYChart.Series<String, Number> serieResaltada = null;
    // True mientras el ratón esté sobre una línea de media (evita que onMouseMoved la deshaga)
    private boolean sobreLineaMedia = false;
    // Líneas de media por técnico (para limpiarlas entre renders)
    private final java.util.List<Node> lineasMedia      = new java.util.ArrayList<>();
    // Nodos de la línea de referencia del equipo (separados para control independiente)
    private final java.util.List<Node> lineasReferencia = new java.util.ArrayList<>();
    // Nodo visual de la línea de referencia (para participar en el sistema de highlight)
    private javafx.scene.shape.Line lineaRefVisual = null;
    // Mapa serie → línea discontinua para poder afectarla en hover
    private final Map<XYChart.Series<String, Number>, javafx.scene.shape.Line> lineaMediaPorSerie = new LinkedHashMap<>();
    // Mapa serie → valor medio (para tooltip)
    private final Map<XYChart.Series<String, Number>, Double> mediaPorSerie = new LinkedHashMap<>();
    // Tooltip reutilizable para la media
    private final Tooltip tooltipMedia = new Tooltip();

    // Todos los puntos cargados de BD (sin filtrar por checkbox)
    private List<PuntoEstadistica> todosPuntos   = List.of();
    // Periodos únicos ordenados
    private List<String>           todosPeriodos = List.of();
    // Tamaño de la ventana visible según granularidad
    private int ventanaTamanio = 30;

    // Ventanas por granularidad
    private static final int VENTANA_DIA    = 30;
    private static final int VENTANA_SEMANA = 16;
    private static final int VENTANA_MES    = 12;
    private static final int VENTANA_ANO    =  5;

    // Color fijo para la serie "Todos" (suma de todos los técnicos)
    private static final String COLOR_TODOS       = "#000000";
    // Color de la línea de referencia (benchmark del equipo)
    private static final String COLOR_REFERENCIA  = "#C07800";

    // Callback de navegación inyectado por MainController
    private com.reparaciones.utils.Navegable navegacion;

    // Nombre del técnico en sesión (null si es admin)
    private String nombreTecnicoSesion;

    // ─── Stock fields ──────────────────────────────────────────────────────────

    @FXML private javafx.scene.chart.BarChart<String, Number> chartStock;
    @FXML private CategoryAxis    ejeXStock;
    @FXML private NumberAxis      ejeYStock;
    @FXML private ComboBox<String> cmbModeloFiltro;
    @FXML private Label           lblSinDatosStock;

    private List<Componente> todosComponentesGestionados = List.of();
    private int numCategoriasStock = 1;

    @FXML
    public void initialize() {
        cmbGranularidad.setItems(FXCollections.observableArrayList("Día", "Semana", "Mes", "Año"));
        cmbGranularidad.setValue("Semana");
        dpDesde.setValue(null);
        dpHasta.setValue(null);

        sliderVentana.valueProperty().addListener((obs, oldVal, newVal) ->
                renderVentana(newVal.intValue()));

        tooltipMedia.setShowDelay(Duration.ZERO);
        tooltipMedia.setShowDuration(Duration.INDEFINITE);
        tooltipMedia.setHideDelay(Duration.millis(100));

        dpDesde.getEditor().setDisable(true);
        dpDesde.getEditor().setOpacity(1.0);
        dpHasta.getEditor().setDisable(true);
        dpHasta.getEditor().setOpacity(1.0);
        dpDesde.valueProperty().addListener((obs, o, n) -> recargarDatos());
        dpHasta.valueProperty().addListener((obs, o, n) -> recargarDatos());

        cargarTecnicos();

        // Registrar color de "Todos" para que todo el sistema de colores/hover funcione automáticamente
        coloresPorNombre.put("Todos", COLOR_TODOS);
        chkTodos.setSelected(true);
        chkTodos.selectedProperty().addListener((obs, o, n) -> {
            renderVentana((int) sliderVentana.getValue());
            actualizarVisibilidadReferencia();
        });

        chkMedia.setSelected(true);
        chkMedia.selectedProperty().addListener((obs, o, n) -> actualizarVisibilidadMedia());

        chkActividad.setSelected(true);
        chkActividad.selectedProperty().addListener((obs, o, n) -> actualizarVisibilidadActividad());

        recargarDatos();

        // ── Stock ──────────────────────────────────────────────────────────────
        cmbModeloFiltro.valueProperty().addListener((obs, o, n) -> renderStockActual());
        chartStock.widthProperty().addListener((obs, o, w) -> ajustarAnchoBarras(w.doubleValue()));
        poblarFiltrosComponente();
    }

    /** Carga los técnicos de la BD, crea un CheckBox por cada uno y asigna su color fijo. */
    private void cargarTecnicos() {
        List<Tecnico> tecnicos;
        try {
            tecnicos = new TecnicoDAO().getAll();
        } catch (SQLException e) {
            mostrarError(e);
            return;
        }

        Integer idTecSesion = com.reparaciones.Sesion.getIdTec();
        if (idTecSesion != null)
            nombreTecnicoSesion = tecnicos.stream()
                    .filter(t -> t.getIdTec() == idTecSesion)
                    .map(Tecnico::getNombre).findFirst().orElse(null);

        List<Tecnico> activos   = tecnicos.stream().filter(Tecnico::isActivo).collect(Collectors.toList());
        List<Tecnico> inactivos = tecnicos.stream().filter(t -> !t.isActivo()).collect(Collectors.toList());

        for (Tecnico t : activos) {
            String colorHex = generarColor(t.getIdTec());
            coloresPorNombre.put(t.getNombre(), colorHex);
            CheckBox cb = new CheckBox(t.getNombre());
            cb.setSelected(true);
            cb.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold;");
            cb.selectedProperty().addListener((obs, o, n) -> {
                actualizarTextoMenuTecnicos();
                renderVentana((int) sliderVentana.getValue());
            });
            checksPorNombre.put(t.getNombre(), cb);
            menuTecnicos.getItems().add(new CustomMenuItem(cb, false));
        }

        if (!inactivos.isEmpty()) {
            menuTecnicos.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            for (Tecnico t : inactivos) {
                String colorHex = generarColor(t.getIdTec());
                coloresPorNombre.put(t.getNombre(), colorHex);
                CheckBox cb = new CheckBox(t.getNombre() + " (inactivo)");
                cb.setSelected(false);
                cb.setStyle("-fx-text-fill: #9A9A9A; -fx-font-style: italic;");
                cb.selectedProperty().addListener((obs, o, n) -> {
                    actualizarTextoMenuTecnicos();
                    renderVentana((int) sliderVentana.getValue());
                });
                checksPorNombre.put(t.getNombre(), cb);
                menuTecnicos.getItems().add(new CustomMenuItem(cb, false));
            }
        }

        actualizarTextoMenuTecnicos();
    }

    /**
     * Genera un color hex determinista para un técnico a partir de su ID.
     * Usa el ángulo áureo (137.508°) sobre el espacio HSB para maximizar
     * la separación perceptual entre técnicos consecutivos.
     */
    private String generarColor(int idTec) {
        double hue = (idTec * 137.508) % 360;
        Color c = Color.hsb(hue, 0.75, 0.72);
        return String.format("#%02X%02X%02X",
                (int) (c.getRed()   * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue()  * 255));
    }

    /** Recarga datos de BD y reinicia la ventana. Llamado al cambiar fechas o granularidad. */
    @FXML
    private void recargarGrafico() {
        recargarDatos();
    }

    private void recargarDatos() {
        // Sin fechas: usar todo el rango disponible (1900-01-01 → 2999-12-31)
        java.time.LocalDate desde = dpDesde.getValue() != null
                ? dpDesde.getValue() : java.time.LocalDate.of(1900, 1, 1);
        java.time.LocalDate hasta = dpHasta.getValue() != null
                ? dpHasta.getValue() : java.time.LocalDate.of(2999, 12, 31);

        String granularidad = switch (cmbGranularidad.getValue()) {
            case "Día"  -> "dia";
            case "Mes"  -> "mes";
            case "Año"  -> "ano";
            default     -> "semana";
        };

        ventanaTamanio = switch (granularidad) {
            case "dia"    -> VENTANA_DIA;
            case "semana" -> VENTANA_SEMANA;
            case "ano"    -> VENTANA_ANO;
            default       -> VENTANA_MES;
        };

        try {
            todosPuntos = new ReparacionDAO().getEstadisticasPorTecnico(
                    granularidad, desde, hasta);
        } catch (SQLException e) {
            mostrarError(e);
            return;
        }

        // Periodos únicos ordenados (de todos los técnicos)
        todosPeriodos = todosPuntos.stream()
                .map(PuntoEstadistica::getPeriodo)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        configurarSlider();
    }

    /** Configura el slider según el total de periodos y el tamaño de ventana. */
    private void configurarSlider() {
        if (todosPeriodos.size() <= ventanaTamanio) {
            hboxSlider.setVisible(false);
            hboxSlider.setManaged(false);
            sliderVentana.setValue(0);
            renderVentana(0); // setValue(0) no dispara el listener si ya era 0
            return;
        }

        int maxOffset = todosPeriodos.size() - ventanaTamanio;
        hboxSlider.setVisible(true);
        hboxSlider.setManaged(true);
        sliderVentana.setMin(0);
        sliderVentana.setMax(maxOffset);
        sliderVentana.setMajorTickUnit(1);
        sliderVentana.setBlockIncrement(1);
        sliderVentana.setSnapToTicks(true);
        // Posicionar al final (datos más recientes); si ya era maxOffset, forzar render
        if (sliderVentana.getValue() == maxOffset) renderVentana(maxOffset);
        else sliderVentana.setValue(maxOffset);
    }

    /** Renderiza la ventana de periodos que empieza en `offset`. */
    private void renderVentana(int offset) {
        if (todosPeriodos.isEmpty()) {
            chartReparaciones.getData().clear();
            lblSinDatos.setVisible(true);
            return;
        }
        lblSinDatos.setVisible(false);

        int tamanio = Math.min(ventanaTamanio, todosPeriodos.size());
        int inicio  = Math.max(0, Math.min(offset, todosPeriodos.size() - tamanio));
        int fin     = inicio + tamanio;

        Set<String> periodosVisibles = new LinkedHashSet<>(todosPeriodos.subList(inicio, fin));

        // Actualizar etiquetas del slider
        lblSliderDesde.setText(todosPeriodos.get(inicio));
        lblSliderHasta.setText(todosPeriodos.get(fin - 1));

        Set<String> seleccionados = checksPorNombre.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<String, XYChart.Series<String, Number>> series = new LinkedHashMap<>();
        for (PuntoEstadistica p : todosPuntos) {
            if (!seleccionados.contains(p.getNombreTecnico())) continue;
            if (!periodosVisibles.contains(p.getPeriodo()))    continue;
            series.computeIfAbsent(p.getNombreTecnico(), nombre -> {
                XYChart.Series<String, Number> s = new XYChart.Series<>();
                s.setName(nombre);
                return s;
            }).getData().add(new XYChart.Data<>(p.getPeriodo(), p.getCantidad()));
        }

        // Serie "Todos": suma de TODOS los técnicos por periodo (independiente de checkboxes)
        List<XYChart.Series<String, Number>> listaFinal = new java.util.ArrayList<>(series.values());
        if (chkTodos.isSelected()) {
            Map<String, Integer> sumaPorPeriodo = new java.util.LinkedHashMap<>();
            for (String p : periodosVisibles) sumaPorPeriodo.put(p, 0);
            for (PuntoEstadistica p : todosPuntos) {
                if (periodosVisibles.contains(p.getPeriodo()))
                    sumaPorPeriodo.merge(p.getPeriodo(), p.getCantidad(), Integer::sum);
            }
            XYChart.Series<String, Number> serieTodos = new XYChart.Series<>();
            serieTodos.setName("Todos");
            sumaPorPeriodo.forEach((periodo, cantidad) ->
                    serieTodos.getData().add(new XYChart.Data<>(periodo, cantidad)));
            listaFinal.add(serieTodos);
        }
        chartReparaciones.getData().setAll(listaFinal);

        // Eje Y: siempre enteros, upper = máximo visible + 1 de margen (mínimo 5)
        int maxVisible = todosPuntos.stream()
                .filter(p -> seleccionados.contains(p.getNombreTecnico())
                          && periodosVisibles.contains(p.getPeriodo()))
                .mapToInt(PuntoEstadistica::getCantidad)
                .max().orElse(0);
        if (chkTodos.isSelected()) {
            Map<String, Integer> sumaPorPeriodo = new java.util.HashMap<>();
            for (PuntoEstadistica p : todosPuntos) {
                if (periodosVisibles.contains(p.getPeriodo()))
                    sumaPorPeriodo.merge(p.getPeriodo(), p.getCantidad(), Integer::sum);
            }
            int maxTodos = sumaPorPeriodo.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            maxVisible = Math.max(maxVisible, maxTodos);
        }
        ejeY.setUpperBound(Math.max(5, maxVisible + 1));

        Runnable render = () -> {
            chartReparaciones.applyCss();
            chartReparaciones.layout();
            aplicarColores();
            chartReparaciones.applyCss();
            chartReparaciones.layout();
            // Las líneas de media necesitan un pulso completo de escena para que el eje
            // haya recalculado su escala con el nuevo upperBound antes de getDisplayPosition()
            final List<XYChart.Series<String, Number>> ts =
                    new java.util.ArrayList<>(chartReparaciones.getData());
            Platform.runLater(() -> {
                chartReparaciones.applyCss();
                chartReparaciones.layout();
                dibujarLineasMedia(periodosVisibles, ts);
            });
        };
        if (chartReparaciones.getScene() != null) render.run();
        else Platform.runLater(render);
    }

    private void dibujarLineasMedia(Set<String> periodosVisibles, List<XYChart.Series<String, Number>> todasSeries) {
        Node bg = chartReparaciones.lookup(".chart-plot-background");
        if (bg == null || !(bg.getParent() instanceof javafx.scene.layout.Pane)) return;
        javafx.scene.layout.Pane plotArea = (javafx.scene.layout.Pane) bg.getParent();

        // Quitar líneas anteriores
        plotArea.getChildren().removeAll(lineasMedia);
        plotArea.getChildren().removeAll(lineasReferencia);
        lineasMedia.clear();
        lineasReferencia.clear();
        lineaRefVisual = null;
        lineaMediaPorSerie.clear();
        mediaPorSerie.clear();

        // Precomputar suma total por periodo (reutilizado en "Todos" y en la referencia)
        Map<String, Integer> sumaPorPeriodo = new java.util.HashMap<>();
        for (PuntoEstadistica p : todosPuntos) {
            if (periodosVisibles.contains(p.getPeriodo()))
                sumaPorPeriodo.merge(p.getPeriodo(), p.getCantidad(), Integer::sum);
        }

        for (XYChart.Series<String, Number> serie : chartReparaciones.getData()) {
            String color = coloresPorNombre.getOrDefault(serie.getName(), "#888888");

            double media;
            if ("Todos".equals(serie.getName())) {
                media = sumaPorPeriodo.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            } else {
                media = todosPuntos.stream()
                        .filter(p -> p.getNombreTecnico().equals(serie.getName())
                                  && periodosVisibles.contains(p.getPeriodo()))
                        .mapToInt(PuntoEstadistica::getCantidad)
                        .average().orElse(0);
            }

            if (media <= 0) continue;

            double yEnEje   = ejeY.getDisplayPosition(media);
            double yEnScene = ejeY.localToScene(0, yEnEje).getY();
            double y        = plotArea.sceneToLocal(0, yEnScene).getY();

            // Usar los límites reales del área de trazado (chart-plot-background),
            // no los del pane padre que incluye el espacio de las etiquetas del eje Y
            double x0 = bg.getBoundsInParent().getMinX();
            double x1 = bg.getBoundsInParent().getMaxX();

            javafx.scene.shape.Line linea = new javafx.scene.shape.Line(x0, y, x1, y);
            linea.setStroke(javafx.scene.paint.Color.web(color));
            linea.setStrokeWidth(1.2);
            linea.getStrokeDashArray().addAll(8.0, 5.0);
            linea.setOpacity(0.6);
            linea.setMouseTransparent(true);
            bg.boundsInParentProperty().addListener((obs, o, b) -> {
                linea.setStartX(b.getMinX()); linea.setEndX(b.getMaxX());
            });

            // Línea invisible ancha para detección del ratón (la visual es de 1.2px, imposible de clicar)
            final XYChart.Series<String, Number> serieRef = serie;
            javafx.scene.shape.Line hitLinea = new javafx.scene.shape.Line(x0, y, x1, y);
            hitLinea.setStroke(javafx.scene.paint.Color.color(0, 0, 0, 0.01));
            hitLinea.setStrokeWidth(12);
            bg.boundsInParentProperty().addListener((obs, o, b) -> {
                hitLinea.setStartX(b.getMinX()); hitLinea.setEndX(b.getMaxX());
            });
            hitLinea.setOnMouseEntered(e -> { sobreLineaMedia = true;  serieResaltada = serieRef; resaltarSerie(serieRef, todasSeries); });
            hitLinea.setOnMouseExited (e -> { sobreLineaMedia = false; serieResaltada = null;     restaurarSeries(todasSeries); });

            Label lbl = new Label(String.format("x̄ %.1f", media));
            lbl.setStyle("-fx-font-size:10px; -fx-text-fill:" + color +
                         "; -fx-background-color:white; -fx-padding:0 2 0 2;");
            lbl.setLayoutX(x0 + 4);
            lbl.setLayoutY(y - 14);
            lbl.setMouseTransparent(true);

            mediaPorSerie.put(serie, media);
            Tooltip tipMedia = new Tooltip(String.format("Media %s: %.1f rep.", serie.getName(), media));
            tipMedia.setShowDelay(Duration.ZERO);
            tipMedia.setShowDuration(Duration.INDEFINITE);
            tipMedia.setHideDelay(Duration.millis(100));
            Tooltip.install(hitLinea, tipMedia);

            plotArea.getChildren().addAll(linea, lbl, hitLinea);
            lineasMedia.add(linea);
            lineasMedia.add(lbl);
            lineasMedia.add(hitLinea);
            lineaMediaPorSerie.put(serie, linea);
        }

        // Línea de referencia: media de las medias individuales de cada técnico
        // (solo periodos activos de cada uno, más justo con técnicos nuevos o con ausencias)
        int nTecnicos = checksPorNombre.size();
        if (nTecnicos > 0 && !sumaPorPeriodo.isEmpty()) {
            double refMedia = checksPorNombre.keySet().stream()
                    .mapToDouble(nombre -> todosPuntos.stream()
                            .filter(p -> p.getNombreTecnico().equals(nombre)
                                      && periodosVisibles.contains(p.getPeriodo()))
                            .mapToInt(PuntoEstadistica::getCantidad)
                            .average().orElse(0))
                    .filter(m -> m > 0)
                    .average().orElse(0);

            if (refMedia > 0) {
                double x0ref = bg.getBoundsInParent().getMinX();
                double x1ref = bg.getBoundsInParent().getMaxX();
                double yRef  = plotArea.sceneToLocal(0,
                        ejeY.localToScene(0, ejeY.getDisplayPosition(refMedia)).getY()).getY();

                javafx.scene.shape.Line lineaRef = new javafx.scene.shape.Line(x0ref, yRef, x1ref, yRef);
                lineaRef.setStroke(javafx.scene.paint.Color.web(COLOR_REFERENCIA));
                lineaRef.setStrokeWidth(1.8);
                lineaRef.getStrokeDashArray().addAll(10.0, 4.0, 2.0, 4.0);
                lineaRef.setOpacity(0.85);
                lineaRef.setMouseTransparent(true);
                bg.boundsInParentProperty().addListener((obs, o, b) -> {
                    lineaRef.setStartX(b.getMinX()); lineaRef.setEndX(b.getMaxX());
                });

                javafx.scene.shape.Line hitRef = new javafx.scene.shape.Line(x0ref, yRef, x1ref, yRef);
                hitRef.setStroke(javafx.scene.paint.Color.color(0, 0, 0, 0.01));
                hitRef.setStrokeWidth(12);
                bg.boundsInParentProperty().addListener((obs, o, b) -> {
                    hitRef.setStartX(b.getMinX()); hitRef.setEndX(b.getMaxX());
                });
                List<String> porEncima = checksPorNombre.keySet().stream()
                        .filter(nombre -> {
                            double media = todosPuntos.stream()
                                    .filter(p -> p.getNombreTecnico().equals(nombre)
                                              && periodosVisibles.contains(p.getPeriodo()))
                                    .mapToInt(PuntoEstadistica::getCantidad)
                                    .average().orElse(0);
                            return media > refMedia;
                        }).collect(Collectors.toList());
                List<String> porDebajo = checksPorNombre.keySet().stream()
                        .filter(nombre -> {
                            double media = todosPuntos.stream()
                                    .filter(p -> p.getNombreTecnico().equals(nombre)
                                              && periodosVisibles.contains(p.getPeriodo()))
                                    .mapToInt(PuntoEstadistica::getCantidad)
                                    .average().orElse(0);
                            return media > 0 && media <= refMedia;
                        }).collect(Collectors.toList());

                String encimaTxt = porEncima.isEmpty() ? "—" : String.join(", ", porEncima);
                String debajTxt  = porDebajo.isEmpty() ? "—" : String.join(", ", porDebajo);
                Tooltip tipRef = new Tooltip(String.format(
                        "Referencia del equipo: %.1f rep./técnico%n" +
                        "(media de medias individuales)%n" +
                        "Por encima: %s%n" +
                        "Por debajo: %s", refMedia, encimaTxt, debajTxt));
                tipRef.setShowDelay(Duration.ZERO);
                tipRef.setShowDuration(Duration.INDEFINITE);
                tipRef.setHideDelay(Duration.millis(100));
                Tooltip.install(hitRef, tipRef);

                Label lblRef = new Label(String.format("ref. %.1f", refMedia));
                lblRef.setStyle("-fx-font-size:10px; -fx-text-fill:" + COLOR_REFERENCIA +
                                "; -fx-background-color:white; -fx-padding:0 2 0 2;");
                lblRef.setLayoutX(x0ref + 4);
                lblRef.setLayoutY(yRef - 14);
                lblRef.setMouseTransparent(true);

                lineaRefVisual = lineaRef;
                hitRef.setOnMouseEntered(e -> {
                    sobreLineaMedia = true;
                    resaltarReferencia(new java.util.ArrayList<>(chartReparaciones.getData()));
                });
                hitRef.setOnMouseExited(e -> {
                    sobreLineaMedia = false;
                    restaurarSeries(new java.util.ArrayList<>(chartReparaciones.getData()));
                });

                plotArea.getChildren().addAll(lineaRef, lblRef, hitRef);
                lineasReferencia.add(lineaRef);
                lineasReferencia.add(lblRef);
                lineasReferencia.add(hitRef);
            }
        }

        actualizarVisibilidadMedia();
        actualizarVisibilidadReferencia();
    }

    private void actualizarVisibilidadReferencia() {
        boolean visible = chkTodos.isSelected();
        lineasReferencia.forEach(n -> n.setVisible(visible));
    }

    private void actualizarVisibilidadMedia() {
        boolean visible = chkMedia.isSelected();
        lineasMedia.forEach(n -> n.setVisible(visible));
    }

    private void actualizarVisibilidadActividad() {
        boolean visible = chkActividad.isSelected();
        for (XYChart.Series<String, Number> serie : chartReparaciones.getData()) {
            if (serie.getNode() != null) serie.getNode().setVisible(visible);
            serie.getData().forEach(d -> { if (d.getNode() != null) d.getNode().setVisible(visible); });
        }
    }

    /** Aplica el color fijo de cada técnico a su línea, puntos y símbolo de leyenda. */
    private void aplicarColores() {
        List<XYChart.Series<String, Number>> todasSeries = new java.util.ArrayList<>(chartReparaciones.getData());

        for (XYChart.Series<String, Number> serie : todasSeries) {
            String color = coloresPorNombre.getOrDefault(serie.getName(), "#888888");

            // Línea visual
            Node lineaNodo = serie.getNode();
            if (lineaNodo != null)
                lineaNodo.setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2px;");

            // Puntos: siempre visibles en vista anual o si la serie tiene un solo punto;
            // ocultos por defecto en el resto de granularidades (se revelan al hover)
            boolean puntosVisibles = "Año".equals(cmbGranularidad.getValue())
                    || serie.getData().size() == 1;
            for (XYChart.Data<String, Number> d : serie.getData()) {
                Node nodo = d.getNode();
                if (nodo == null) continue;

                nodo.setStyle(puntosVisibles
                        ? "-fx-background-color: " + color + ", white;"
                        : "-fx-background-color: transparent, transparent;");

                Tooltip tip = new Tooltip(d.getXValue() + "\n" + d.getYValue().intValue() + " reparaciones");
                tip.setShowDelay(Duration.ZERO);
                tip.setShowDuration(Duration.INDEFINITE);
                tip.setHideDelay(Duration.millis(100));
                Tooltip.install(nodo, tip);

                boolean navegable = navegacion != null &&
                        (com.reparaciones.Sesion.esAdminOSuperTecnico() ||
                         serie.getName().equals(nombreTecnicoSesion));
                nodo.setOnMouseEntered(e -> {
                    String cursor = navegable ? "; -fx-cursor: hand;" : ";";
                    nodo.setStyle("-fx-background-color: " + color + ", white" + cursor);
                    if (serieResaltada != serie) { serieResaltada = serie; resaltarSerie(serie, todasSeries); }
                });
                nodo.setOnMouseExited(e -> {
                    nodo.setStyle(puntosVisibles
                            ? "-fx-background-color: " + color + ", white;"
                            : "-fx-background-color: transparent, transparent;");
                });
                if (navegable) {
                    final XYChart.Series<String, Number> serieClick = serie;
                    nodo.setOnMouseClicked(e -> {
                        String tecnico = "Todos".equals(serieClick.getName()) ? null : serieClick.getName();
                        java.time.LocalDate[] rango = periodoAFechas(d.getXValue());
                        navegacion.navegarAReparaciones(rango[0], rango[1], tecnico);
                    });
                }
            }
        }

        // Hover sobre la línea continua: detectar serie más cercana (umbral 8px)
        chartReparaciones.setOnMouseMoved(e -> {
            if (sobreLineaMedia) return;

            // Si el ratón está encima de un nodo de dato, no mostrar tooltip de media
            boolean sobrePunto = todasSeries.stream()
                    .flatMap(s -> s.getData().stream())
                    .map(XYChart.Data::getNode)
                    .filter(n -> n != null)
                    .anyMatch(n -> n.contains(n.sceneToLocal(e.getSceneX(), e.getSceneY())));

            XYChart.Series<String, Number> cercana = serieMasCercana(e.getX(), e.getY(), todasSeries, 8);
            if (cercana != serieResaltada) {
                serieResaltada = cercana;
                tooltipMedia.hide();
                if (cercana != null) {
                    resaltarSerie(cercana, todasSeries);
                    if (!sobrePunto) {
                        Double m = mediaPorSerie.get(cercana);
                        if (m != null && m > 0) {
                            tooltipMedia.setText(String.format("Media %s: %.1f rep.", cercana.getName(), m));
                            tooltipMedia.show(chartReparaciones, e.getScreenX() + 12, e.getScreenY() - 20);
                        }
                    }
                } else {
                    restaurarSeries(todasSeries);
                }
            } else if (sobrePunto) {
                tooltipMedia.hide();
            }
        });
        chartReparaciones.setOnMouseExited(e -> {
            serieResaltada = null;
            sobreLineaMedia = false;
            tooltipMedia.hide();
            restaurarSeries(todasSeries);
        });

        // Símbolos de leyenda
        chartReparaciones.lookupAll(".chart-legend-item").forEach(item -> {
            if (item instanceof Label lbl) {
                String color = coloresPorNombre.get(lbl.getText());
                if (color == null) return;
                Node simbolo = lbl.lookup(".chart-legend-item-symbol");
                if (simbolo != null)
                    simbolo.setStyle("-fx-background-color: " + color + ", white;");
            }
        });
        actualizarVisibilidadActividad();
    }

    /**
     * Devuelve la serie cuya línea está más cerca del punto (mx, my) en coordenadas
     * locales del chart, o null si ninguna está dentro del umbral de píxeles dado.
     */
    private XYChart.Series<String, Number> serieMasCercana(
            double mx, double my,
            List<XYChart.Series<String, Number>> series, double umbral) {

        XYChart.Series<String, Number> mejor = null;
        double menorDist = umbral;

        for (XYChart.Series<String, Number> serie : series) {
            Node nodo = serie.getNode();
            if (!(nodo instanceof javafx.scene.shape.Path path)) continue;

            // Convertir punto del chart al sistema local del Path
            javafx.geometry.Point2D pt = path.sceneToLocal(
                    chartReparaciones.localToScene(mx, my));
            double px = pt.getX(), py = pt.getY();

            javafx.scene.shape.PathElement[] elems =
                    path.getElements().toArray(new javafx.scene.shape.PathElement[0]);
            double ax = 0, ay = 0;
            for (javafx.scene.shape.PathElement el : elems) {
                double bx, by;
                if (el instanceof javafx.scene.shape.MoveTo m) { ax = m.getX(); ay = m.getY(); continue; }
                else if (el instanceof javafx.scene.shape.LineTo l) { bx = l.getX(); by = l.getY(); }
                else continue;

                // Distancia punto-segmento
                double dx = bx - ax, dy = by - ay;
                double len2 = dx * dx + dy * dy;
                double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
                double cx = ax + t * dx, cy = ay + t * dy;
                double dist = Math.hypot(px - cx, py - cy);
                if (dist < menorDist) { menorDist = dist; mejor = serie; }
                ax = bx; ay = by;
            }
        }
        return mejor;
    }

    private void resaltarSerie(XYChart.Series<String, Number> activa,
                               List<XYChart.Series<String, Number>> todas) {
        for (XYChart.Series<String, Number> s : todas) {
            String color = coloresPorNombre.getOrDefault(s.getName(), "#888888");
            boolean estaActiva = s == activa;
            double opacidad = estaActiva ? 1 : 0.25;
            if (s.getNode() != null)
                s.getNode().setStyle("-fx-stroke: " + color + "; -fx-stroke-width: " +
                        (estaActiva ? "3px" : "1.5px") + "; -fx-opacity: " + opacidad + ";");
            javafx.scene.shape.Line ld = lineaMediaPorSerie.get(s);
            if (ld != null) ld.setOpacity(estaActiva ? 0.9 : 0.15);
        }
        if (lineaRefVisual != null) lineaRefVisual.setOpacity(0.15);
    }

    private void resaltarReferencia(List<XYChart.Series<String, Number>> todas) {
        for (XYChart.Series<String, Number> s : todas) {
            String color = coloresPorNombre.getOrDefault(s.getName(), "#888888");
            if (s.getNode() != null)
                s.getNode().setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 1.5px; -fx-opacity: 0.25;");
            javafx.scene.shape.Line ld = lineaMediaPorSerie.get(s);
            if (ld != null) ld.setOpacity(0.15);
        }
        if (lineaRefVisual != null) lineaRefVisual.setOpacity(0.9);
    }

    private void restaurarSeries(List<XYChart.Series<String, Number>> todas) {
        for (XYChart.Series<String, Number> s : todas) {
            String color = coloresPorNombre.getOrDefault(s.getName(), "#888888");
            if (s.getNode() != null)
                s.getNode().setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2px; -fx-opacity: 1;");
            javafx.scene.shape.Line ld = lineaMediaPorSerie.get(s);
            if (ld != null) ld.setOpacity(0.6);
        }
        if (lineaRefVisual != null) lineaRefVisual.setOpacity(0.85);
    }

    private void actualizarTextoMenuTecnicos() {
        long total      = checksPorNombre.size();
        long seleccionados = checksPorNombre.values().stream().filter(CheckBox::isSelected).count();
        if (seleccionados == 0 || seleccionados == total)
            menuTecnicos.setText("Técnicos");
        else if (seleccionados == 1)
            menuTecnicos.setText(checksPorNombre.entrySet().stream()
                    .filter(e -> e.getValue().isSelected())
                    .map(Map.Entry::getKey).findFirst().orElse("Técnicos"));
        else
            menuTecnicos.setText(seleccionados + " técnicos");
    }

    @FXML
    private void limpiarFiltros() {
        dpDesde.setValue(null);
        dpHasta.setValue(null);
        checksPorNombre.forEach((nombre, cb) -> {
            boolean esInactivo = cb.getStyle().contains("italic");
            cb.setSelected(!esInactivo);
        });
        chkTodos.setSelected(true);
        chkMedia.setSelected(true);
        chkActividad.setSelected(true);
        actualizarTextoMenuTecnicos();
        recargarDatos();
    }

    @FXML
    private void mostrarReparaciones() {
        pnlReparaciones.setVisible(true);  pnlReparaciones.setManaged(true);
        pnlStock.setVisible(false);        pnlStock.setManaged(false);
        setActivo(btnTabReparaciones, btnTabStock);
    }

    @FXML
    private void mostrarStock() {
        pnlStock.setVisible(true);         pnlStock.setManaged(true);
        pnlReparaciones.setVisible(false); pnlReparaciones.setManaged(false);
        setActivo(btnTabStock, btnTabReparaciones);
    }

    private void setActivo(Button activo, Button inactivo) {
        activo.getStyleClass().setAll("stock-sidebar-btn-active");
        inactivo.getStyleClass().setAll("stock-sidebar-btn");
    }

    @Override
    public void recargar() {
        if (pnlStock.isVisible()) renderStockActual();
        else                      recargarDatos();
    }

    @Override
    public void detenerPolling() { /* sin poller */ }

    /** Inyectado por MainController para permitir navegación a la vista de reparaciones. */
    public void setNavegacion(com.reparaciones.utils.Navegable navegacion) {
        this.navegacion = navegacion;
    }

    // ─── Stock methods ─────────────────────────────────────────────────────────

    private static final java.util.LinkedHashMap<String, String> PREFIJO_TIPO;
    static {
        PREFIJO_TIPO = new java.util.LinkedHashMap<>();
        PREFIJO_TIPO.put("bat", "Batería");
        PREFIJO_TIPO.put("cha", "Chasis");
        PREFIJO_TIPO.put("cam", "Cámara");
        PREFIJO_TIPO.put("lcd", "LCD");
        PREFIJO_TIPO.put("mc",  "Marco");
        PREFIJO_TIPO.put("g",   "Pantalla");
    }

    private static String tipoDeComponente(String sku) {
        String lower = sku.toLowerCase();
        for (var entry : PREFIJO_TIPO.entrySet()) {
            if (lower.startsWith(entry.getKey())) return entry.getValue();
        }
        return "Otro";
    }

    private static String modeloDeComponente(String sku) {
        String lower = sku.toLowerCase();
        for (String prefijo : PREFIJO_TIPO.keySet()) {
            if (lower.startsWith(prefijo)) {
                String raw = FormularioReparacionController.extraerModelo(lower, prefijo);
                return raw.isEmpty() ? sku : FormularioReparacionController.traducirModelo(raw);
            }
        }
        return sku;
    }

    private static String labelComponente(String sku) {
        String tipo  = tipoDeComponente(sku);
        String lower = sku.toLowerCase();
        for (String prefijo : PREFIJO_TIPO.keySet()) {
            if (lower.startsWith(prefijo)) {
                String raw   = FormularioReparacionController.extraerModelo(lower, prefijo);
                String resto = lower.substring(prefijo.length());
                if (resto.startsWith("i")) resto = resto.substring(1);
                String color = raw.isEmpty() ? resto : resto.substring(raw.length());
                if (!color.isEmpty()) return tipo + " (" + color + ")";
                break;
            }
        }
        return tipo;
    }

    private void poblarFiltrosComponente() {
        try {
            todosComponentesGestionados = new ComponenteDAO().getAllGestionados().stream()
                    .filter(com.reparaciones.models.Componente::isActivo)
                    .collect(java.util.stream.Collectors.toList());
        } catch (SQLException e) {
            mostrarError(e);
            return;
        }
        java.util.Set<String> rawModelos = new java.util.HashSet<>();
        for (Componente c : todosComponentesGestionados) {
            String lower = c.getTipo().toLowerCase();
            for (String prefijo : PREFIJO_TIPO.keySet()) {
                if (lower.startsWith(prefijo)) {
                    String raw = FormularioReparacionController.extraerModelo(lower, prefijo);
                    if (!raw.isEmpty()) rawModelos.add(raw);
                    break;
                }
            }
        }
        List<String> modelos = FormularioReparacionController.MODELOS_ORDENADOS.stream()
                .filter(rawModelos::contains)
                .map(FormularioReparacionController::traducirModelo)
                .collect(Collectors.toList());
        cmbModeloFiltro.setItems(FXCollections.observableArrayList(modelos));
        if (!modelos.isEmpty()) cmbModeloFiltro.setValue(modelos.get(0));
    }

    @FXML
    private void filtrarComponentes() {
        renderStockActual();
    }

    private void renderStockActual() {
        String modelo = cmbModeloFiltro.getValue();
        if (modelo == null) {
            chartStock.getData().clear();
            lblSinDatosStock.setVisible(true);
            return;
        }

        List<Componente> filtrados = todosComponentesGestionados.stream()
                .filter(c -> modeloDeComponente(c.getTipo()).equals(modelo))
                .collect(Collectors.toList());

        if (filtrados.isEmpty()) {
            chartStock.getData().clear();
            lblSinDatosStock.setVisible(true);
            return;
        }
        lblSinDatosStock.setVisible(false);

        XYChart.Series<String, Number> serieStock    = new XYChart.Series<>();
        XYChart.Series<String, Number> serieEnCamino = new XYChart.Series<>();
        serieStock.setName("Stock actual");
        serieEnCamino.setName("En camino");

        Map<String, Componente> porLabel = new LinkedHashMap<>();
        for (Componente c : filtrados) {
            String label = labelComponente(c.getTipo());
            porLabel.put(label, c);
            serieStock.getData().add(new XYChart.Data<>(label, c.getStock()));
            serieEnCamino.getData().add(new XYChart.Data<>(label, c.getEnCamino()));
        }

        numCategoriasStock = filtrados.size();
        chartStock.getData().setAll(serieStock, serieEnCamino);

        int maxY = filtrados.stream()
                .mapToInt(c -> c.getStock() + c.getEnCamino())
                .max().orElse(10);
        ejeYStock.setUpperBound(Math.max(10, maxY + 2));
        ejeYStock.setTickUnit(Math.max(1, (maxY + 2) / 10));

        Runnable render = () -> {
            chartStock.applyCss();
            chartStock.layout();
            colorearBarrasStock(serieStock, serieEnCamino, porLabel);
            Platform.runLater(() -> {
                chartStock.applyCss();
                chartStock.layout();
                ajustarAnchoBarras(chartStock.getWidth());
                corregirLeyendaStock();
            });
        };
        if (chartStock.getScene() != null) render.run();
        else Platform.runLater(render);
    }

    private void colorearBarrasStock(XYChart.Series<String, Number> serieStock,
                                      XYChart.Series<String, Number> serieEnCamino,
                                      Map<String, Componente> porLabel) {
        for (XYChart.Data<String, Number> d : serieStock.getData()) {
            if (d.getNode() == null) continue;
            Componente c = porLabel.get(d.getXValue());
            boolean bajo = c != null && c.getStock() <= c.getStockMinimo();
            String color = bajo ? com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD
                                : com.reparaciones.utils.Colores.VERDE_OK;
            d.getNode().setStyle("-fx-bar-fill: " + color + ";");

            String tipTxt = d.getXValue() + "\nStock: " + d.getYValue().intValue() + " uds.";
            if (c != null) tipTxt += "\nMínimo: " + c.getStockMinimo() + " uds.";
            Tooltip tip = new Tooltip(tipTxt);
            tip.setShowDelay(Duration.ZERO);
            tip.setShowDuration(Duration.INDEFINITE);
            tip.setHideDelay(Duration.millis(100));
            Tooltip.install(d.getNode(), tip);
        }
        for (XYChart.Data<String, Number> d : serieEnCamino.getData()) {
            if (d.getNode() == null) continue;
            if (d.getYValue().intValue() == 0) {
                d.getNode().setStyle("-fx-bar-fill: transparent;");
            } else {
                d.getNode().setStyle("-fx-bar-fill: #90CAF9;");
                Tooltip tip = new Tooltip(
                        d.getXValue() + "\nEn camino: " + d.getYValue().intValue() + " uds.");
                tip.setShowDelay(Duration.ZERO);
                tip.setShowDuration(Duration.INDEFINITE);
                tip.setHideDelay(Duration.millis(100));
                Tooltip.install(d.getNode(), tip);
            }
        }
    }

    private void ajustarAnchoBarras(double chartWidth) {
        if (numCategoriasStock <= 0 || chartWidth <= 0) return;
        double available  = chartWidth - 80;
        double targetPair = 60.0;
        double gap = Math.max(10, (available - numCategoriasStock * targetPair) / (numCategoriasStock + 1));
        chartStock.setCategoryGap(gap);
    }

    private void corregirLeyendaStock() {
        java.util.Map<String, String> colores = java.util.Map.of(
                "Stock actual", com.reparaciones.utils.Colores.VERDE_OK,
                "En camino",    "#90CAF9");
        chartStock.lookupAll(".chart-legend-item").forEach(item -> {
            if (item instanceof Label lbl) {
                String color = colores.get(lbl.getText());
                if (color == null) return;
                Node simbolo = lbl.lookup(".bar-legend-symbol");
                if (simbolo == null) simbolo = lbl.lookup(".chart-legend-item-symbol");
                if (simbolo != null)
                    simbolo.setStyle("-fx-background-color: " + color + ";");
            }
        });
    }

    // ─── Navigation helpers ────────────────────────────────────────────────────

    /**
     * Convierte un periodo (según la granularidad activa) al rango de fechas que representa.
     * Formatos: día="2026-05-12", semana="2026-W15", mes="2026-05"
     */
    private java.time.LocalDate[] periodoAFechas(String periodo) {
        return switch (cmbGranularidad.getValue()) {
            case "Día" -> {
                java.time.LocalDate d = java.time.LocalDate.parse(periodo);
                yield new java.time.LocalDate[]{d, d};
            }
            case "Mes" -> {
                java.time.YearMonth ym = java.time.YearMonth.parse(periodo);
                yield new java.time.LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
            }
            case "Año" -> {
                int year = Integer.parseInt(periodo);
                yield new java.time.LocalDate[]{
                        java.time.LocalDate.of(year, 1, 1),
                        java.time.LocalDate.of(year, 12, 31)};
            }
            default -> { // "2026-W15" → lunes–domingo de esa semana ISO
                java.time.LocalDate lunes = java.time.LocalDate.parse(
                        periodo + "-1",
                        java.time.format.DateTimeFormatter.ISO_WEEK_DATE);
                yield new java.time.LocalDate[]{lunes, lunes.plusDays(6)};
            }
        };
    }

    private void mostrarError(Exception e) {
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, e.getMessage()).showAndWait();
    }
}
