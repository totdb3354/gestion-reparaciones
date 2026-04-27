package com.reparaciones.controllers;

import com.reparaciones.dao.ReparacionDAO;
import com.reparaciones.utils.ConfirmDialog;
import com.reparaciones.dao.TecnicoDAO;
import com.reparaciones.dao.TelefonoDAO;
import com.reparaciones.models.ReparacionResumen;
import com.reparaciones.models.Tecnico;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de la tabla de asignaciones pendientes (vista del administrador).
 * <p>Incrustado como controlador anidado en {@link ReparacionControllerAdmin}.
 * Muestra todas las asignaciones ({@code A*}) con estado pendiente y permite:</p>
 * <ul>
 *   <li>Crear nuevas asignaciones para cualquier técnico y IMEI.</li>
 *   <li>Reasignar asignaciones a otro técnico.</li>
 *   <li>Eliminar asignaciones sin reparación asociada.</li>
 *   <li>Filtrar por IMEI, técnico y solicitudes pendientes de componente.</li>
 * </ul>
 *
 * @role ADMIN
 */
public class PendientesAdminController {

    @FXML private TableView<ReparacionResumen>           tablaPendientes;
    @FXML private TableColumn<ReparacionResumen, Void>   cEstado;
    @FXML private TableColumn<ReparacionResumen, String> cId;
    @FXML private TableColumn<ReparacionResumen, String> cTecnico;
    @FXML private TableColumn<ReparacionResumen, String> cImei;
    @FXML private TableColumn<ReparacionResumen, String> cFecha;
    @FXML private TableColumn<ReparacionResumen, Void>   cAccion;
    @FXML private MenuButton filtroTecnico;
    @FXML private MenuButton filtroSolicitud;

    private final ReparacionDAO  reparacionDAO = new ReparacionDAO();
    private final TecnicoDAO     tecnicoDAO    = new TecnicoDAO();
    private final TelefonoDAO    telefonoDAO   = new TelefonoDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final ObservableList<ReparacionResumen> datos = FXCollections.observableArrayList();
    private FilteredList<ReparacionResumen> datosFiltrados;

    @FXML private Button btnConfirmarCambios;
    @FXML private Label  lblUltimaActualizacion;

    private CheckBox cbSoloSolicitudes;
    private CheckBox cbSoloIncidencias;
    private CheckBox cbSoloAsignaciones;
    private final List<CheckBox>        cbsTecnico       = new ArrayList<>();
    private final List<Tecnico>         tecnicos         = new ArrayList<>();
    private final Map<String, Tecnico>  cambiosPendientes = new HashMap<>();

    @FXML
    public void initialize() {
        tablaPendientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        cId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getIdRep()));
        cTecnico.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<Tecnico> cb = new ComboBox<>();
            private boolean actualizando = false;
            {
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.setStyle("-fx-font-size: 11px;");
                cb.setOnAction(e -> {
                    if (actualizando) return;
                    if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) return;
                    ReparacionResumen rep = getTableView().getItems().get(getIndex());
                    Tecnico sel = cb.getValue();
                    if (sel == null) return;
                    if (sel.getIdTec() != rep.getIdTec()) {
                        cambiosPendientes.put(rep.getIdRep(), sel);
                    } else {
                        cambiosPendientes.remove(rep.getIdRep());
                    }
                    actualizarVisibilidadConfirmar();
                    tablaPendientes.refresh();
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                actualizando = true;
                ReparacionResumen rep = getTableView().getItems().get(getIndex());
                cb.getItems().setAll(tecnicos);
                Tecnico mostrar = cambiosPendientes.getOrDefault(rep.getIdRep(),
                        tecnicos.stream().filter(t -> t.getIdTec() == rep.getIdTec())
                                .findFirst().orElse(null));
                cb.setValue(mostrar);
                actualizando = false;
                boolean modificada = cambiosPendientes.containsKey(rep.getIdRep());
                setStyle(modificada
                        ? "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_MODIFICADA_BG + ";"
                        : "");
                setGraphic(cb);
            }
        });
        cImei.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getImei()));
        cFecha.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getFechaAsig() != null ? d.getValue().getFechaAsig().format(FMT) : ""));

        datosFiltrados = new FilteredList<>(datos, p -> true);
        tablaPendientes.setItems(datosFiltrados);
        tablaPendientes.setColumnResizePolicy(param -> true);

        tablaPendientes.setRowFactory(tv -> new TableRow<>() {
            {
                ContextMenu menu = new ContextMenu();
                MenuItem copiar = new MenuItem("📋  Copiar celda");
                copiar.setOnAction(e -> {
                    if (getItem() == null) return;
                    var seleccion = tablaPendientes.getSelectionModel().getSelectedCells();
                    if (seleccion.isEmpty()) return;
                    String texto = textoDeCelda(getItem(), seleccion.get(0).getTableColumn());
                    if (texto == null || texto.isEmpty()) return;
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(texto);
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                });
                menu.getItems().add(copiar);
                setContextMenu(menu);
                selectedProperty().addListener((obs, o, sel) -> actualizarEstilo());
            }
            private void actualizarEstilo() {
                ReparacionResumen item = getItem();
                if (isEmpty() || item == null) { setStyle("-fx-border-width: 0 0 0 8; -fx-border-color: transparent;"); return; }
                if (isSelected()) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.AZUL_MEDIO + ";" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SELECTED_BRD + " transparent;" +
                            "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;");
                } else if (item.getEsSolicitud() == 1) {
                    setStyle("-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + ";");
                } else if (item.isEsIncidencia()) {
                    setStyle("-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + ";");
                } else {
                    setStyle("-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " transparent;");
                }
            }
            @Override
            protected void updateItem(ReparacionResumen item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstilo();
            }
        });

        cEstado.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                ReparacionResumen rep = getTableView().getItems().get(getIndex());
                String base = "-fx-background-radius: 10; -fx-padding: 2 10 2 10;" +
                              "-fx-font-size: 11px; -fx-font-weight: bold;";
                if (rep.isEsIncidencia()) {
                    badge.setText("Incidencia");
                    badge.setStyle(base +
                        "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BG + ";" +
                        "-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + ";");
                } else if (rep.getEsSolicitud() == 1) {
                    badge.setText("Solicitud");
                    badge.setStyle(base +
                        "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BG + ";" +
                        "-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + ";");
                } else {
                    badge.setText("Normal");
                    badge.setStyle(base +
                        "-fx-background-color: #E8EAF0;" +
                        "-fx-text-fill: #586376;");
                }
                setGraphic(badge);
            }
        });

        Image imgBorrar = new Image(getClass().getResourceAsStream("/images/borrar.png"));
        cAccion.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv  = new ImageView(imgBorrar);
            private final HBox      box = new HBox(iv);
            {
                iv.setFitWidth(25); iv.setFitHeight(25); iv.setPreserveRatio(true);
                iv.setStyle("-fx-cursor: hand;");
                box.setAlignment(Pos.CENTER);
                iv.setOnMouseClicked(e -> {
                    ReparacionResumen rep = getTableView().getItems().get(getIndex());
                    String desc = "El técnico dejará de verla en su lista de pendientes" +
                            (rep.isEsIncidencia()
                                    ? " y la incidencia se marcará como no activa en la tabla principal."
                                    : ".");
                    ConfirmDialog.mostrar("Borrar asignación " + rep.getIdRep(), desc,
                            "Borrar asignación", () -> {
                                try {
                                    if (rep.isEsIncidencia())
                                        reparacionDAO.borrarIncidenciaPorImei(rep.getImei());
                                    reparacionDAO.eliminarAsignacion(rep.getIdRep());
                                    cargar();
                                } catch (SQLException ex) { ex.printStackTrace(); }
                            });
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        configurarFiltros();
        cargar();
    }

    // ─── Filtros ──────────────────────────────────────────────────────────────

    private void configurarFiltros() {
        // Filtro técnico
        try {
            tecnicos.addAll(tecnicoDAO.getAll());
            for (Tecnico t : tecnicos) {
                CheckBox cb = new CheckBox(t.getNombre());
                cb.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
                cb.selectedProperty().addListener((obs, o, n) -> {
                    actualizarTextoFiltroTecnico();
                    aplicarFiltros();
                });
                cbsTecnico.add(cb);
                CustomMenuItem item = new CustomMenuItem(cb, false);
                filtroTecnico.getItems().add(item);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Filtro tipo
        cbSoloSolicitudes = new CheckBox("Solicitudes pieza");
        cbSoloSolicitudes.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
        cbSoloSolicitudes.selectedProperty().addListener((obs, o, n) -> {
            actualizarTextoFiltroSolicitud();
            aplicarFiltros();
        });
        cbSoloIncidencias = new CheckBox("Incidencias");
        cbSoloIncidencias.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
        cbSoloIncidencias.selectedProperty().addListener((obs, o, n) -> {
            actualizarTextoFiltroSolicitud();
            aplicarFiltros();
        });
        cbSoloAsignaciones = new CheckBox("Asignaciones");
        cbSoloAsignaciones.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
        cbSoloAsignaciones.selectedProperty().addListener((obs, o, n) -> {
            actualizarTextoFiltroSolicitud();
            aplicarFiltros();
        });
        CustomMenuItem itemSol  = new CustomMenuItem(cbSoloSolicitudes, false);
        CustomMenuItem itemInc  = new CustomMenuItem(cbSoloIncidencias, false);
        CustomMenuItem itemAsig = new CustomMenuItem(cbSoloAsignaciones, false);
        filtroSolicitud.getItems().addAll(itemSol, itemInc, itemAsig);
    }

    private void actualizarTextoFiltroTecnico() {
        long sel = cbsTecnico.stream().filter(CheckBox::isSelected).count();
        filtroTecnico.setText(sel == 0 ? "Técnico" : sel == 1
                ? cbsTecnico.stream().filter(CheckBox::isSelected)
                        .findFirst().map(CheckBox::getText).orElse("Técnico")
                : sel + " técnicos");
    }

    private void actualizarTextoFiltroSolicitud() {
        boolean sol  = cbSoloSolicitudes.isSelected();
        boolean inc  = cbSoloIncidencias.isSelected();
        boolean asig = cbSoloAsignaciones.isSelected();
        long total = java.util.stream.Stream.of(sol, inc, asig).filter(Boolean::booleanValue).count();
        if      (total == 0) filtroSolicitud.setText("Tipo");
        else if (total == 3) filtroSolicitud.setText("Todas");
        else if (total == 1) filtroSolicitud.setText(sol ? "Solicitudes pieza" : inc ? "Incidencias" : "Asignaciones");
        else                 filtroSolicitud.setText(total + " filtros");
    }

    private void aplicarFiltros() {
        if (datosFiltrados == null) return;
        List<Integer> idsTecSelec = new ArrayList<>();
        for (int i = 0; i < cbsTecnico.size(); i++)
            if (cbsTecnico.get(i).isSelected()) idsTecSelec.add(tecnicos.get(i).getIdTec());
        boolean filtrarSol  = cbSoloSolicitudes.isSelected();
        boolean filtrarInc  = cbSoloIncidencias.isSelected();
        boolean filtrarAsig = cbSoloAsignaciones.isSelected();

        datosFiltrados.setPredicate(rep -> {
            if (!idsTecSelec.isEmpty() && !idsTecSelec.contains(rep.getIdTec())) return false;
            if (filtrarSol || filtrarInc || filtrarAsig) {
                boolean esSol  = rep.getEsSolicitud() == 1;
                boolean esInc  = rep.isEsIncidencia();
                boolean esAsig = !esSol && !esInc;
                boolean mostrar = false;
                if (filtrarSol  && esSol)  mostrar = true;
                if (filtrarInc  && esInc)  mostrar = true;
                if (filtrarAsig && esAsig) mostrar = true;
                if (!mostrar) return false;
            }
            return true;
        });
    }

    @FXML
    private void limpiarFiltros() {
        cbsTecnico.forEach(cb -> cb.setSelected(false));
        cbSoloSolicitudes.setSelected(false);
        cbSoloIncidencias.setSelected(false);
        cbSoloAsignaciones.setSelected(false);
        filtroTecnico.setText("Técnico");
        filtroSolicitud.setText("Tipo");
    }

    // ─── Carga ────────────────────────────────────────────────────────────────

    public void cargar() {
        try {
            datos.setAll(reparacionDAO.getAsignaciones());
            String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            if (lblUltimaActualizacion != null) lblUltimaActualizacion.setText("Actualizado " + hora);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void abrirFormularioAsignacion() {
        // ── IMEI ──────────────────────────────────────────────────────────────
        Label lblTitulo = new Label("Asignar reparación");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3B54;");

        Label lblImei = new Label("IMEI del teléfono");
        lblImei.setStyle("-fx-font-size: 12px; -fx-text-fill: #586376; -fx-font-weight: bold;");

        TextField tfImei = new TextField();
        tfImei.setPromptText("15 dígitos");
        tfImei.setStyle("-fx-background-color: white; -fx-border-color: #C2C8D0;" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;" +
                "-fx-text-fill: #2C3B54; -fx-font-size: 13px;");

        Label lblImeiErr = new Label();
        lblImeiErr.setStyle("-fx-font-size: 11px; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ERROR + ";");

        // ── Lista de técnicos (checkboxes en ScrollPane) ─────────────────────
        Label lblTecnicos = new Label("Técnicos a asignar");
        lblTecnicos.setStyle("-fx-font-size: 12px; -fx-text-fill: #586376; -fx-font-weight: bold;");

        List<Tecnico> tecnicosModal = new ArrayList<>();
        List<CheckBox> checkboxes = new ArrayList<>();
        VBox cbContainer = new VBox(6);
        cbContainer.setStyle("-fx-background-color: white; -fx-padding: 8;");

        try {
            tecnicosModal.addAll(tecnicoDAO.getAllActivos());
            for (Tecnico t : tecnicosModal) {
                CheckBox cb = new CheckBox(t.getNombre());
                cb.setStyle("-fx-font-size: 12px;");
                checkboxes.add(cb);
                cbContainer.getChildren().add(cb);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        ScrollPane scrollTecnicos = new ScrollPane(cbContainer);
        scrollTecnicos.setFitToWidth(true);
        scrollTecnicos.setMaxHeight(150);
        scrollTecnicos.setPrefHeight(Math.min(150, tecnicosModal.size() * 30 + 16));
        scrollTecnicos.setStyle("-fx-background-color: white; -fx-border-color: #C2C8D0;" +
                "-fx-border-radius: 4; -fx-background-radius: 4;");

        // ── Botones ───────────────────────────────────────────────────────────
        Button btnConfirmar = new Button("Asignar reparación");
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        btnConfirmar.setDisable(true);
        btnConfirmar.getStyleClass().add("btn-primary");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.getStyleClass().add("btn-secondary");

        HBox botones = new HBox(10, btnCancelar, btnConfirmar);
        botones.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // ── Validación ────────────────────────────────────────────────────────
        Runnable validar = () -> {
            String imeiStr = tfImei.getText().trim();
            boolean imeiOk = imeiStr.length() == 15;
            boolean bloqueadoPorHistorial = false;

            if (imeiOk) {
                try {
                    if (telefonoDAO.exists(imeiStr)) {
                        bloqueadoPorHistorial = true;
                        lblImeiErr.setText("Este teléfono ya tiene historial. Marca una incidencia desde la tabla si necesita reparación.");
                        tfImei.setStyle("-fx-background-color: white; -fx-border-color: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + ";" +
                                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;" +
                                "-fx-text-fill: #2C3B54; -fx-font-size: 13px;");
                        checkboxes.forEach(cb -> { cb.setDisable(true); cb.setSelected(false); });
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            if (!bloqueadoPorHistorial) {
                checkboxes.forEach(cb -> cb.setDisable(false));
                tfImei.setStyle(imeiStr.isEmpty()
                        ? "-fx-background-color: white; -fx-border-color: #C2C8D0;" +
                                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;" +
                                "-fx-text-fill: #2C3B54; -fx-font-size: 13px;"
                        : imeiOk
                                ? "-fx-background-color: white; -fx-border-color: #8AC7AF;" +
                                        "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;" +
                                        "-fx-text-fill: #2C3B54; -fx-font-size: 13px;"
                                : "-fx-background-color: white; -fx-border-color: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + ";" +
                                        "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;" +
                                        "-fx-text-fill: #2C3B54; -fx-font-size: 13px;");
                lblImeiErr.setText(!imeiStr.isEmpty() && !imeiOk ? "El IMEI debe tener exactamente 15 dígitos" : "");
            }

            boolean algunoSeleccionado = checkboxes.stream().anyMatch(cb -> cb.isSelected() && !cb.isDisabled());
            btnConfirmar.setDisable(!(imeiOk && !bloqueadoPorHistorial && algunoSeleccionado));
        };

        // ── Listeners ─────────────────────────────────────────────────────────
        tfImei.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) tfImei.setText(n.replaceAll("[^\\d]", ""));
            if (tfImei.getText().length() > 15) tfImei.setText(tfImei.getText().substring(0, 15));
            validar.run();
        });
        checkboxes.forEach(cb -> cb.selectedProperty().addListener((obs, o, n) -> validar.run()));

        // ── Confirmar ─────────────────────────────────────────────────────────
        VBox contenido = new VBox(12, lblTitulo, lblImei, tfImei, lblImeiErr, lblTecnicos, scrollTecnicos, botones);
        contenido.setPadding(new Insets(28));
        contenido.setPrefWidth(400);
        contenido.setStyle("-fx-background-color: #DDE1E7;");

        javafx.stage.Stage ventana = new javafx.stage.Stage();
        ventana.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        ventana.setResizable(false);
        ventana.setTitle("Asignar reparación");

        btnCancelar.setOnAction(ev -> ventana.close());

        btnConfirmar.setOnAction(ev -> {
            String imei = tfImei.getText().trim();
            try {
                telefonoDAO.insertar(imei);
                for (int i = 0; i < checkboxes.size(); i++) {
                    if (checkboxes.get(i).isSelected())
                        reparacionDAO.insertarAsignacion(imei, tecnicosModal.get(i).getIdTec());
                }
                ventana.close();
                cargar();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        javafx.scene.Scene scene = new javafx.scene.Scene(contenido);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        ventana.setScene(scene);
        javafx.application.Platform.runLater(tfImei::requestFocus);
        ventana.showAndWait();
    }

    public void resetearCambios() {
        cambiosPendientes.clear();
        actualizarVisibilidadConfirmar();
        tablaPendientes.refresh();
    }

    private void actualizarVisibilidadConfirmar() {
        boolean hay = !cambiosPendientes.isEmpty();
        btnConfirmarCambios.setVisible(hay);
        btnConfirmarCambios.setManaged(hay);
    }

    @FXML
    private void confirmarCambiosTecnico() {
        List<String> conflictos = new ArrayList<>();
        for (Map.Entry<String, Tecnico> entry : new java.util.ArrayList<>(cambiosPendientes.entrySet())) {
            String  idRep   = entry.getKey();
            Tecnico tecnico = entry.getValue();
            ReparacionResumen rep = datos.stream()
                    .filter(r -> r.getIdRep().equals(idRep)).findFirst().orElse(null);
            if (rep == null) continue;
            try {
                reparacionDAO.actualizarTecnico(idRep, tecnico.getIdTec(), rep.getUpdatedAt());
                rep.setIdTec(tecnico.getIdTec());
                rep.setNombreTecnico(tecnico.getNombre());
                cambiosPendientes.remove(idRep);
            } catch (com.reparaciones.utils.StaleDataException e) {
                conflictos.add(mensajeConflicto(idRep, tecnico));
            } catch (SQLException e) { e.printStackTrace(); }
        }
        if (!conflictos.isEmpty()) {
            String detalle = String.join("\n", conflictos);
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,
                    "Los siguientes cambios no se pudieron guardar:\n\n" + detalle +
                    "\n\nLos datos se han recargado.")
                    .showAndWait();
            cargar();
        }
        actualizarVisibilidadConfirmar();
        tablaPendientes.refresh();
    }

    private String mensajeConflicto(String idRep, Tecnico tecnicoIntentado) {
        try {
            java.util.Optional<ReparacionResumen> actual = reparacionDAO.getAsignacionById(idRep);
            if (actual.isEmpty())
                return "• " + idRep + ": ya no está pendiente (fue completada por otro usuario).";
            ReparacionResumen rep = actual.get();
            if (rep.getIdTec() != tecnicoIntentado.getIdTec())
                return "• " + idRep + ": fue reasignada a " + rep.getNombreTecnico() + " por otro usuario.";
            return "• " + idRep + ": fue modificada por otro usuario.";
        } catch (SQLException e) {
            return "• " + idRep + ": fue modificada por otro usuario.";
        }
    }

    /** @return los ítems actualmente visibles en la tabla (respetando filtros activos) */
    public java.util.List<ReparacionResumen> getItemsVisibles() {
        return tablaPendientes.getItems();
    }

    /**
     * Extrae el texto copiable de la celda seleccionada para la acción "Copiar celda".
     *
     * @param rep datos de la fila
     * @param col columna seleccionada
     * @return texto de la celda, o {@code null} si la columna no es copiable
     */
    private String textoDeCelda(ReparacionResumen rep, TableColumn<?, ?> col) {
        if (col == cId)    return rep.getIdRep();
        if (col == cImei)  return rep.getImei();
        if (col == cFecha) return rep.getFechaAsig() != null ? rep.getFechaAsig().format(FMT) : "";
        return null;
    }
}