package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.ReparacionDAO;
import com.reparaciones.utils.ConfirmDialog;
import com.reparaciones.models.ReparacionResumen;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;

import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

/**
 * Controlador de la vista de reparaciones para el rol TECNICO.
 * <p>Presenta dos secciones accesibles desde el sidebar:</p>
 * <ul>
 *   <li><b>Historial</b> — tabla con las reparaciones propias del técnico, filtrable
 *       por IMEI, rango de fechas e incidencias. Solo lectura (sin edición ni eliminación).</li>
 *   <li><b>Mis pendientes</b> — asignaciones del técnico, gestionadas por
 *       {@link PendientesTecnicoController} (incrustado como controlador anidado).</li>
 * </ul>
 * <p>Incluye un poller periódico que recarga en segundo plano para detectar nuevas
 * asignaciones sin que el técnico tenga que navegar manualmente.</p>
 * <p>Implementa {@link com.reparaciones.utils.Recargable} y
 * {@link com.reparaciones.utils.Exportable}.</p>
 *
 * @role TECNICO
 */
public class ReparacionControllerTecnico implements com.reparaciones.utils.Recargable, com.reparaciones.utils.Exportable {

    @FXML
    private TableView<ReparacionResumen> tablaReparaciones;
    @FXML
    private TableColumn<ReparacionResumen, String> colIdRep;
    @FXML
    private TableColumn<ReparacionResumen, String> colImei;
    @FXML
    private TableColumn<ReparacionResumen, String> colReparador;
    @FXML
    private TableColumn<ReparacionResumen, String> colFecha;
    @FXML
    private TableColumn<ReparacionResumen, String> colComponente;
    @FXML
    private TableColumn<ReparacionResumen, String> colObservaciones;
    @FXML
    private TableColumn<ReparacionResumen, Void> colEstado;
    @FXML
    private TableColumn<ReparacionResumen, Void> colIncidencia;
    @FXML
    private TableColumn<ReparacionResumen, String> colIdAnterior;
    @FXML private TextField  filtroImei;
    @FXML private DatePicker filtroFechaDesde;
    @FXML private DatePicker filtroFechaHasta;
    @FXML private MenuButton filtroIncidencias;

    // ── Sidebar + paneles ─────────────────────────────────────────────────────
    @FXML private Button btnTabHistorial;
    @FXML private Button btnTabMisPendientes;
    @FXML private VBox   pnlHistorial;
    @FXML private VBox   pnlMisPendientes;
    @FXML private Label  lblUltimaActualizacion;
    @FXML private PendientesTecnicoController misPendientesController;
    private CheckBox cbIncidenciasAbiertas;
    private CheckBox cbIncidenciasCerradas;
    private CheckBox cbNormales;

    private final ReparacionDAO reparacionDAO = new ReparacionDAO();
    private final ObservableList<ReparacionResumen> datos = FXCollections.observableArrayList();
    private FilteredList<ReparacionResumen> datosFiltrados;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final java.util.concurrent.ScheduledExecutorService poller =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "poller-reparaciones-tecnico");
                t.setDaemon(true);
                return t;
            });

    @FXML
    public void initialize() {
        tablaReparaciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaReparaciones.setFixedCellSize(44);

        datosFiltrados = new FilteredList<>(datos, p -> true);
        tablaReparaciones.setItems(datosFiltrados);
        configurarColumnas();
        configurarFilas();
        configurarFiltros();

        misPendientesController.setOnCerrar(this::cargarDatos);

        misPendientesController.cargar();

        poller.scheduleAtFixedRate(
                () -> javafx.application.Platform.runLater(this::recargar),
                60, 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    /** Detiene el poller periódico al salir de la vista. */
    @Override
    public void detenerPolling() { poller.shutdownNow(); }

    // ─── Sidebar ─────────────────────────────────────────────────────────────

    /**
     * Recarga la sección visible: mis pendientes o historial.
     * Invocado por {@link MainController} cuando la ventana recupera el foco.
     */
    @Override
    public void recargar() {
        if (pnlMisPendientes.isVisible()) misPendientesController.cargar();
        else                              cargarDatos();
    }

    @FXML private void mostrarHistorial() {
        mostrarPanel(pnlHistorial, btnTabHistorial);
        cargarDatos();
    }

    public void irAInicio() { mostrarPanel(pnlMisPendientes, btnTabMisPendientes); }

    private void mostrarPanel(VBox panel, Button btnActivo) {
        pnlHistorial    .setVisible(false); pnlHistorial    .setManaged(false);
        pnlMisPendientes.setVisible(false); pnlMisPendientes.setManaged(false);
        panel.setVisible(true); panel.setManaged(true);
        for (Button b : new Button[]{btnTabHistorial, btnTabMisPendientes}) {
            b.getStyleClass().removeAll("stock-sidebar-btn-active", "stock-sidebar-btn");
            b.getStyleClass().add(b == btnActivo ? "stock-sidebar-btn-active" : "stock-sidebar-btn");
        }
    }

    // ─── Label expandible (click abre popup de lectura) ───────────────────────

    private Label labelExpandible(String titulo, String texto) {
        Label lbl = new Label(texto != null ? texto : "");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setTextOverrun(OverrunStyle.ELLIPSIS);
        if (texto != null && !texto.isEmpty()) {
            lbl.setStyle("-fx-cursor: hand;");
            lbl.setOnMouseClicked(e -> { if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) ConfirmDialog.mostrarTexto(titulo, texto); });
        }
        return lbl;
    }

    // ─── Columnas ─────────────────────────────────────────────────────────────

    private void configurarColumnas() {

        colIdRep.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty ? null : getTableView().getItems().get(getIndex()).getIdRep());
            }
        });

        Image imgHistorial = new Image(getClass().getResourceAsStream("/images/Historial.png"));
        colImei.setCellFactory(col -> new TableCell<>() {
            private final Label lblImei = new Label();
            private final ImageView ivHist = new ImageView(imgHistorial);
            private final HBox contenedor = new HBox(6, lblImei, ivHist);
            {
                ivHist.setFitWidth(25);
                ivHist.setFitHeight(25);
                ivHist.setPreserveRatio(true);
                ivHist.setStyle("-fx-cursor: hand;");
                contenedor.setAlignment(Pos.CENTER_LEFT);
                ivHist.setOnMouseClicked(e -> abrirHistorialImei(getTableView().getItems().get(getIndex()).getImei()));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                lblImei.setText(getTableView().getItems().get(getIndex()).getImei());
                setGraphic(contenedor);
            }
        });

        colReparador.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty ? null : getTableView().getItems().get(getIndex()).getNombreTecnico());
            }
        });

        colFecha.setCellFactory(col -> new TableCell<>() {
            private final Label lblInicio = new Label();
            private final Label lblFin    = new Label();
            private final VBox  box       = new VBox(1, lblInicio, lblFin);
            {
                actualizarColores(false);
                tableRowProperty().addListener((obs, oldRow, newRow) -> {
                    if (newRow != null)
                        newRow.selectedProperty().addListener((o, wasSelected, isSelected) -> actualizarColores(isSelected));
                });
            }
            private void actualizarColores(boolean selected) {
                lblInicio.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (selected ? "white" : "#9AA0AA") + ";");
                lblFin.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (selected ? "white" : "#2C3B54") + ";");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty) { setGraphic(null); return; }
                ReparacionResumen rep = getTableView().getItems().get(getIndex());
                lblInicio.setText(rep.getFechaAsig() != null ? rep.getFechaAsig().format(FORMATO_FECHA) : "—");
                lblFin.setText("→ " + (rep.getFechaFin() != null ? rep.getFechaFin().format(FORMATO_FECHA) : "—"));
                actualizarColores(getTableRow() != null && getTableRow().isSelected());
                setGraphic(box);
            }
        });

        colComponente.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty ? null : getTableView().getItems().get(getIndex()).getTipoComponente());
            }
        });

        // CON tooltip
        colObservaciones.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                setGraphic(labelExpandible("Observaciones", getTableView().getItems().get(getIndex()).getObservaciones()));
            }
        });

        colIdAnterior.setCellFactory(col -> new TableCell<>() {
            private final Label lblLink = new Label();
            {
                lblLink.setStyle("-fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ACCION + "; -fx-cursor: hand;");
                lblLink.setTextOverrun(OverrunStyle.ELLIPSIS);
                lblLink.setMaxWidth(Double.MAX_VALUE);
                lblLink.setOnMouseEntered(
                        e -> lblLink.setStyle("-fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ACCION + "; -fx-cursor: hand; -fx-underline: true;"));
                lblLink.setOnMouseExited(
                        e -> lblLink.setStyle("-fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ACCION + "; -fx-cursor: hand; -fx-underline: false;"));
                lblLink.setOnMouseClicked(e -> {
                    String idAnterior = getTableView().getItems().get(getIndex()).getIdRepAnterior();
                    if (idAnterior == null)
                        return;
                    for (int i = 0; i < getTableView().getItems().size(); i++) {
                        if (idAnterior.equals(getTableView().getItems().get(i).getIdRep())) {
                            getTableView().getSelectionModel().select(i);
                            getTableView().scrollTo(i);
                            getTableView().requestFocus();
                            break;
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                String idAnterior = getTableView().getItems().get(getIndex()).getIdRepAnterior();
                if (idAnterior != null) {
                    lblLink.setText(idAnterior);
                    setGraphic(lblLink);
                } else
                    setGraphic(null);
            }
        });

        configurarColEstado();
        configurarColIncidencia();
    }

    private void configurarColEstado() {
        colEstado.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                ReparacionResumen rep = getTableView().getItems().get(getIndex());
                String base = "-fx-background-radius: 10; -fx-padding: 2 10 2 10;" +
                              "-fx-font-size: 11px; -fx-font-weight: bold;";
                if (rep.isEsIncidencia() && !rep.isEsResuelto()) {
                    badge.setText("Incidencia");
                    badge.setStyle(base +
                        "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BG + ";" +
                        "-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + ";");
                } else if (rep.isEsIncidencia()) {
                    badge.setText("Resuelta");
                    badge.setStyle(base +
                        "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_REPARADO_BG + ";" +
                        "-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_REPARADO_ICO + ";");
                } else {
                    badge.setText("Normal");
                    badge.setStyle(base +
                        "-fx-background-color: #E8EAF0;" +
                        "-fx-text-fill: #586376;");
                }
                setGraphic(badge);
            }
        });
    }

    private void configurarColIncidencia() {
        colIncidencia.setCellFactory(col -> new TableCell<>() {
            private final Label lblComentario = new Label();
            private final Label lblSin = new Label("Sin incidencia");
            {
                lblComentario.setMaxWidth(Double.MAX_VALUE);
                lblComentario.setTextOverrun(OverrunStyle.ELLIPSIS);
                lblSin.setStyle("-fx-font-size: 12px; -fx-text-fill: #A0A0A0; -fx-font-style: italic;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); setStyle(""); return; }
                ReparacionResumen rep = getTableView().getItems().get(getIndex());
                if (!rep.isEsIncidencia()) {
                    setGraphic(lblSin);
                    setStyle("");
                } else if (!rep.isEsResuelto()) {
                    String texto = rep.getIncidencia() != null ? rep.getIncidencia() : "";
                    lblComentario.setText(texto);
                    lblComentario.setStyle("-fx-font-size: 12px; -fx-text-fill: #000000;" +
                            (!texto.isEmpty() ? " -fx-cursor: hand;" : ""));
                    lblComentario.setOnMouseClicked(texto.isEmpty() ? null :
                            e -> { if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) ConfirmDialog.mostrarTexto("Incidencia", texto); });
                    setStyle("");
                    setGraphic(lblComentario);
                } else {
                    String texto = rep.getIncidencia() != null ? rep.getIncidencia() : "";
                    lblComentario.setText(texto);
                    lblComentario.setStyle("-fx-font-size: 12px; -fx-text-fill: #A9A9A9;" +
                            (!texto.isEmpty() ? " -fx-cursor: hand;" : ""));
                    lblComentario.setOnMouseClicked(texto.isEmpty() ? null :
                            e -> { if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) ConfirmDialog.mostrarTexto("Incidencia", texto); });
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.FILA_REPARADO_BG + ";");
                    setGraphic(lblComentario);
                }
            }
        });
    }

    private void configurarFilas() {
        tablaReparaciones.setRowFactory(tv -> new TableRow<>() {
            {
                ContextMenu menu = new ContextMenu();
                TableColumn<?, ?>[] colRightClick = {null};
                MenuItem copiar = new MenuItem("📋  Copiar celda");
                copiar.setOnAction(e -> {
                    if (getItem() == null || colRightClick[0] == null) return;
                    TableColumn<?, ?> col = colRightClick[0];
                    String texto = textoDeCelda(getItem(), col);
                    if (texto == null || texto.isEmpty())
                        return;
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(texto);
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                    getChildrenUnmodifiable().stream()
                        .filter(n -> n instanceof TableCell && ((TableCell<?, ?>) n).getTableColumn() == col)
                        .findFirst()
                        .ifPresent(cell -> {
                            javafx.beans.property.DoubleProperty flashAlpha = new javafx.beans.property.SimpleDoubleProperty(1.0);
                            flashAlpha.addListener((obs2, o2, n2) -> {
                                double a = n2.doubleValue();
                                if (a <= 0.02) cell.setStyle("");
                                else cell.setStyle(String.format(java.util.Locale.US,
                                    "-fx-background-color: rgba(224,247,250,%.2f);", a));
                            });
                            cell.setStyle("-fx-background-color: rgba(224,247,250,1.0);");
                            new javafx.animation.Timeline(
                                new javafx.animation.KeyFrame(javafx.util.Duration.millis(600),
                                    new javafx.animation.KeyValue(flashAlpha, 0.0))
                            ).play();
                        });
                });
                menu.getItems().add(copiar);
                setContextMenu(menu);
                setOnContextMenuRequested(e -> {
                    double x = e.getX(); double offset = 0;
                    for (TableColumn<?, ?> c : tv.getVisibleLeafColumns()) {
                        offset += c.getWidth();
                        if (x < offset) { colRightClick[0] = c; break; }
                    }
                });
                selectedProperty().addListener((obs, wasSelected, isSelected) -> aplicarEstilo(getItem(), isEmpty()));
            }

            private void aplicarEstilo(ReparacionResumen item, boolean empty) {
                if (empty || item == null) { setStyle("-fx-border-width: 0 0 0 8; -fx-border-color: transparent;"); return; }
                if (isSelected()) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.AZUL_MEDIO + ";" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SELECTED_BRD + " transparent;" +
                            "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;");
                } else if (item.isEsIncidencia() && !item.isEsResuelto()) {
                    setStyle("-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + ";");
                } else if (item.isEsIncidencia()) {
                    setStyle("-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " " + com.reparaciones.utils.Colores.FILA_REPARADO_BRD + ";");
                } else {
                    setStyle("-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " transparent;");
                }
            }

            @Override
            protected void updateItem(ReparacionResumen item, boolean empty) {
                super.updateItem(item, empty);
                aplicarEstilo(item, empty);
            }
        });
    }

    private void actualizarTextoFiltroIncidencias() {
        boolean a = cbIncidenciasAbiertas.isSelected();
        boolean c = cbIncidenciasCerradas.isSelected();
        boolean n = cbNormales.isSelected();
        long total = java.util.stream.Stream.of(a, c, n).filter(Boolean::booleanValue).count();
        if      (total == 0) filtroIncidencias.setText("Incidencias");
        else if (total == 3) filtroIncidencias.setText("Todas");
        else if (total == 1) filtroIncidencias.setText(a ? "Abiertas" : c ? "Cerradas" : "Sin incidencia");
        else                 filtroIncidencias.setText(total + " filtros");
    }

    private String textoDeCelda(ReparacionResumen rep, TableColumn<?, ?> col) {
        if (col == colIdRep)         return rep.getIdRep();
        if (col == colImei)          return rep.getImei();
        if (col == colReparador)     return rep.getNombreTecnico();
        if (col == colFecha)         return rep.getFechaFin() != null ? rep.getFechaFin().format(FORMATO_FECHA) : "";
        if (col == colComponente)    return rep.getTipoComponente();
        if (col == colObservaciones) return rep.getObservaciones();
        if (col == colIncidencia)    return rep.getIncidencia();
        if (col == colIdAnterior)    return rep.getIdRepAnterior();
        return null;
    }

    private void cargarDatos() {
        try {
            Integer idTec = Sesion.getIdTec();
            if (idTec == null) return;
            datos.setAll(reparacionDAO.getReparacionesPorTecnico(idTec));
            aplicarFiltros();
            String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            if (lblUltimaActualizacion != null)
                lblUltimaActualizacion.setText("Actualizado " + hora);
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    // ─── Filtros ──────────────────────────────────────────────────────────────

    private void configurarFiltros() {
        filtroImei.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) filtroImei.setText(n.replaceAll("[^\\d]", ""));
            if (filtroImei.getText().length() > 15)
                filtroImei.setText(filtroImei.getText().substring(0, 15));
            String val = filtroImei.getText();
            if (val.isEmpty())
                filtroImei.setStyle("");
            else if (val.length() < 15)
                filtroImei.setStyle("-fx-background-color: #F3F3F3; -fx-border-color: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + ";" +
                        "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 10; -fx-font-size: 12px;");
            else
                filtroImei.setStyle("-fx-background-color: #F3F3F3; -fx-border-color: #8AC7AF;" +
                        "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 10; -fx-font-size: 12px;");
            aplicarFiltros();
        });
        filtroFechaDesde.getEditor().setDisable(true);
        filtroFechaDesde.getEditor().setOpacity(1.0);
        filtroFechaHasta.getEditor().setDisable(true);
        filtroFechaHasta.getEditor().setOpacity(1.0);
        filtroFechaDesde.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
        filtroFechaHasta.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
        cbIncidenciasAbiertas = new CheckBox("Abiertas");
        cbIncidenciasAbiertas.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
        cbIncidenciasAbiertas.selectedProperty().addListener((obs, o, n) -> {
            actualizarTextoFiltroIncidencias();
            aplicarFiltros();
        });
        cbIncidenciasCerradas = new CheckBox("Cerradas");
        cbIncidenciasCerradas.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
        cbIncidenciasCerradas.selectedProperty().addListener((obs, o, n) -> {
            actualizarTextoFiltroIncidencias();
            aplicarFiltros();
        });
        cbNormales = new CheckBox("Sin incidencia");
        cbNormales.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
        cbNormales.selectedProperty().addListener((obs, o, n) -> {
            actualizarTextoFiltroIncidencias();
            aplicarFiltros();
        });
        CustomMenuItem itemAbiertas = new CustomMenuItem(cbIncidenciasAbiertas, false);
        CustomMenuItem itemCerradas = new CustomMenuItem(cbIncidenciasCerradas, false);
        CustomMenuItem itemNormales = new CustomMenuItem(cbNormales, false);
        filtroIncidencias.getItems().addAll(itemAbiertas, itemCerradas, itemNormales);
    }

    private void aplicarFiltros() {
        if (datosFiltrados == null) return;
        String imeiStr = filtroImei.getText().trim();
        LocalDate desde = filtroFechaDesde.getValue();
        LocalDate hasta = filtroFechaHasta.getValue();
        boolean filtrarAbiertas = cbIncidenciasAbiertas.isSelected();
        boolean filtrarCerradas = cbIncidenciasCerradas.isSelected();
        boolean filtrarNormales = cbNormales.isSelected();

        datosFiltrados.setPredicate(rep -> {
            if (imeiStr.length() == 15 && !rep.getImei().equals(imeiStr))
                return false;
            if (desde != null || hasta != null) {
                if (rep.getFechaFin() == null) return false;
                LocalDate fechaFin = rep.getFechaFin().toLocalDate();
                if (desde != null && fechaFin.isBefore(desde)) return false;
                if (hasta != null && fechaFin.isAfter(hasta))  return false;
            }
            if (filtrarAbiertas || filtrarCerradas || filtrarNormales) {
                boolean mostrar = false;
                if (filtrarNormales && !rep.isEsIncidencia())                        mostrar = true;
                if (filtrarAbiertas && rep.isEsIncidencia() && !rep.isEsResuelto()) mostrar = true;
                if (filtrarCerradas && rep.isEsIncidencia() &&  rep.isEsResuelto()) mostrar = true;
                if (!mostrar) return false;
            }
            return true;
        });
    }

    /**
     * Aplica un filtro inicial de fecha.
     * <p>Llamado por {@link MainController} al navegar desde la vista de estadísticas
     * (clic en un vértice del gráfico). Navega al historial antes de aplicar el filtro.</p>
     *
     * @param desde fecha de inicio del filtro
     * @param hasta fecha de fin del filtro
     */
    public void setFiltroInicial(java.time.LocalDate desde, java.time.LocalDate hasta) {
        mostrarHistorial();
        filtroFechaDesde.setValue(desde);
        filtroFechaHasta.setValue(hasta);
    }

    @FXML
    private void limpiarFiltros() {
        filtroImei.clear();
        filtroFechaDesde.setValue(null);
        filtroFechaHasta.setValue(null);
        cbIncidenciasAbiertas.setSelected(false);
        cbIncidenciasCerradas.setSelected(false);
        cbNormales.setSelected(false);
        filtroIncidencias.setText("Incidencias");
        filtroImei.setStyle("");
    }

    @FXML
    private void abrirModalPendientes() {
        mostrarPanel(pnlMisPendientes, btnTabMisPendientes);
        misPendientesController.cargar();
    }

    // ─── Historial IMEI ───────────────────────────────────────────────────────

    private void abrirHistorialImei(String imei) {
        try {
            List<ReparacionResumen> historial = reparacionDAO.getResumenPorImei(imei);

            TableColumn<ReparacionResumen, String> cId     = new TableColumn<>("ID");
            TableColumn<ReparacionResumen, String> cTecnico = new TableColumn<>("Técnico");
            TableColumn<ReparacionResumen, String> cFecha  = new TableColumn<>("Fechas");
            TableColumn<ReparacionResumen, String> cComp   = new TableColumn<>("Componente");
            TableColumn<ReparacionResumen, String> cObs    = new TableColumn<>("Observaciones");
            TableColumn<ReparacionResumen, String> cIncid  = new TableColumn<>("Incidencia");

            cId.setPrefWidth(130);     cId.setMinWidth(100);
            cTecnico.setPrefWidth(110); cTecnico.setMinWidth(80);
            cFecha.setPrefWidth(130);  cFecha.setMinWidth(110);
            cComp.setPrefWidth(120);   cComp.setMinWidth(90);
            cObs.setPrefWidth(200);    cObs.setMinWidth(100); cObs.setMaxWidth(250);
            cIncid.setPrefWidth(200);  cIncid.setMinWidth(100);

            cId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getIdRep()));
            cTecnico.setCellValueFactory(
                    d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreTecnico()));

            cFecha.setCellFactory(col -> new TableCell<>() {
                private final Label lblInicio = new Label();
                private final Label lblFin    = new Label();
                private final VBox  box       = new VBox(1, lblInicio, lblFin);
                {
                    actualizarColores(false);
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null)
                            newRow.selectedProperty().addListener((o, was, sel) -> actualizarColores(sel));
                    });
                }
                private void actualizarColores(boolean selected) {
                    lblInicio.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (selected ? "white" : "#9AA0AA") + ";");
                    lblFin.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (selected ? "white" : "#2C3B54") + ";");
                }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty); setText(null);
                    if (empty) { setGraphic(null); return; }
                    ReparacionResumen r = getTableView().getItems().get(getIndex());
                    lblInicio.setText(r.getFechaAsig() != null ? r.getFechaAsig().format(FORMATO_FECHA) : "—");
                    lblFin.setText("→ " + (r.getFechaFin() != null ? r.getFechaFin().format(FORMATO_FECHA) : "—"));
                    actualizarColores(getTableRow() != null && getTableRow().isSelected());
                    setGraphic(box);
                }
            });

            cComp.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue().getTipoComponente() != null ? d.getValue().getTipoComponente() : "—"));
            cObs.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue().getObservaciones() != null ? d.getValue().getObservaciones() : ""));
            cObs.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isBlank()) { setText(null); setTooltip(null); }
                    else { setText(item); setTooltip(new Tooltip(item)); }
                }
            });
            cIncid.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue().isEsIncidencia() && d.getValue().getIncidencia() != null
                            ? d.getValue().getIncidencia() : ""));
            cIncid.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isBlank()) { setText(null); setTooltip(null); }
                    else { setText(item); setTooltip(new Tooltip(item)); }
                }
            });

            TableView<ReparacionResumen> tabla = new TableView<>();
            tabla.getStyleClass().add("tabla-reparaciones");
            tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            tabla.setPrefHeight(440);
            tabla.getColumns().addAll(cId, cTecnico, cFecha, cComp, cObs, cIncid);
            tabla.setItems(FXCollections.observableArrayList(historial));

            Label lblTitulo = new Label("Historial del IMEI");
            lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3B54;");
            Label lblImeiLabel = new Label(imei);
            lblImeiLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #586376;");

            Button btnCerrar = new Button("Cerrar");
            btnCerrar.getStyleClass().add("btn-secondary");

            HBox botones = new HBox(btnCerrar);
            botones.setAlignment(Pos.CENTER_RIGHT);

            VBox contenido = new VBox(12, lblTitulo, lblImeiLabel, tabla, botones);
            contenido.setPadding(new Insets(28));
            contenido.setPrefWidth(1000);
            contenido.setStyle("-fx-background-color: #DDE1E7;");

            javafx.stage.Stage ventana = new javafx.stage.Stage();
            ventana.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            ventana.setResizable(true);
            ventana.setTitle("Historial — " + imei);

            btnCerrar.setOnAction(ev -> ventana.close());

            javafx.scene.Scene scene = new javafx.scene.Scene(contenido);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            ventana.setScene(scene);
            ventana.showAndWait();

        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    @FXML
    private void descargarHistorial() {
        exportarCSV((Stage) tablaReparaciones.getScene().getWindow());
    }

    @Override
    public void exportarCSV(Stage owner) {
        java.util.List<ReparacionResumen> items;
        String nombre;
        if (pnlMisPendientes.isVisible()) {
            items  = misPendientesController.getItemsVisibles();
            nombre = "mis_pendientes";
        } else {
            items  = tablaReparaciones.getItems();
            nombre = "mis_reparaciones";
        }

        List<String> cabeceras = List.of(
                "ID Reparación", "IMEI", "Fecha asig.", "Fecha fin",
                "Componente", "Observaciones", "Incidencia", "Resuelto", "ID Rep. anterior");
        List<List<String>> filas = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (ReparacionResumen r : items) {
            filas.add(List.of(
                    r.getIdRep(),
                    com.reparaciones.utils.CsvExporter.textoForzado(r.getImei()),
                    r.getFechaAsig() != null ? r.getFechaAsig().format(fmt) : "",
                    r.getFechaFin()  != null ? r.getFechaFin().format(fmt)  : "",
                    r.getTipoComponente() != null ? r.getTipoComponente() : "",
                    r.getObservaciones()  != null ? r.getObservaciones()  : "",
                    r.isEsIncidencia() ? (r.getIncidencia() != null ? r.getIncidencia() : "Sí") : "No",
                    r.isEsResuelto() ? "Sí" : "No",
                    r.getIdRepAnterior() != null ? r.getIdRepAnterior() : ""
            ));
        }
        com.reparaciones.utils.CsvExporter.exportar(owner, nombre, cabeceras, filas);
    }

    private void mostrarError(Exception e) {
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, e.getMessage()).showAndWait();
    }
}