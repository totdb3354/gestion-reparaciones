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

public class PendientesAdminController {

    @FXML private TableView<ReparacionResumen>           tablaPendientes;
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

        tablaPendientes.setRowFactory(tv -> new TableRow<>() {
            {
                selectedProperty().addListener((obs, o, sel) -> actualizarEstilo());
            }
            private void actualizarEstilo() {
                ReparacionResumen item = getItem();
                if (isEmpty() || item == null) { setStyle(""); return; }
                if (isSelected()) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.AZUL_MEDIO + ";" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SELECTED_BRD + " transparent;" +
                            "-fx-border-width: 0 0 0.2 0;");
                    return;
                }
                if (item.getEsSolicitud() == 1) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BG + ";" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + " transparent;" +
                            "-fx-border-width: 0 0 0.2 0;");
                } else if (item.isEsIncidencia()) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BG + ";" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + " transparent;" +
                            "-fx-border-width: 0 0 0.2 0;");
                } else {
                    setStyle("");
                }
            }
            @Override
            protected void updateItem(ReparacionResumen item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstilo();
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
                                    datos.remove(rep);
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
        filtroTecnico.setStyle("-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
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
                item.setStyle("-fx-background-color: white;");
                filtroTecnico.getItems().add(item);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Filtro tipo
        filtroSolicitud.setStyle("-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
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
        CustomMenuItem itemSol = new CustomMenuItem(cbSoloSolicitudes, false);
        itemSol.setStyle("-fx-background-color: white;");
        CustomMenuItem itemInc = new CustomMenuItem(cbSoloIncidencias, false);
        itemInc.setStyle("-fx-background-color: white;");
        CustomMenuItem itemAsig = new CustomMenuItem(cbSoloAsignaciones, false);
        itemAsig.setStyle("-fx-background-color: white;");
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void abrirFormularioAsignacion() {
        // ── IMEI ──────────────────────────────────────────────────────────────
        Label lblImei = new Label("Introduzca IMEI de teléfono a reparar");
        TextField tfImei = new TextField();
        Label lblImeiErr = new Label();
        tfImei.setPromptText("Introduce un IMEI para identificar");
        tfImei.setStyle("-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
        lblImeiErr.setStyle("-fx-font-size: 11px; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ERROR + ";");

        // ── Lista de técnicos con checkboxes ──────────────────────────────────
        Label lblTecnicos = new Label("Técnicos a asignar");
        VBox listaTecnicos = new VBox(4);
        listaTecnicos.setStyle("-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-padding: 8;");

        List<Tecnico> tecnicos = new ArrayList<>();
        List<CheckBox> checkboxes = new ArrayList<>();
        try {
            tecnicos.addAll(tecnicoDAO.getAll());
            for (Tecnico t : tecnicos) {
                CheckBox cb = new CheckBox(t.getNombre());
                cb.setStyle("-fx-font-size: 12px;");
                checkboxes.add(cb);
                listaTecnicos.getChildren().add(cb);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        // ── Botón confirmar ───────────────────────────────────────────────────
        Button btnConfirmar = new Button("Asignar reparación");
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        btnConfirmar.setDisable(true);
        btnConfirmar.setStyle("-fx-background-color: #E7E7E7; -fx-text-fill: #A9A9A9;" +
                "-fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 8;");

        // ── Validación + actualización de checkboxes ──────────────────────────
        Runnable validar = () -> {
            String imeiStr = tfImei.getText().trim();
            boolean imeiOk = imeiStr.length() == 15;
            boolean bloqueadoPorHistorial = false;

            // Primero comprobar si el IMEI tiene historial
            if (imeiOk) {
                try {
                    if (telefonoDAO.exists(imeiStr)) {
                        bloqueadoPorHistorial = true;
                        lblImeiErr.setText("Este teléfono ya tiene historial. Marca una incidencia desde la tabla si necesita reparación.");
                        lblImeiErr.setStyle("-fx-font-size: 11px; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ERROR + ";");
                        tfImei.setStyle("-fx-background-color: white; -fx-border-color: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + ";" +
                                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
                        // Deshabilitar todos los checkboxes
                        checkboxes.forEach(cb -> { cb.setDisable(true); cb.setSelected(false); });
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            if (!bloqueadoPorHistorial) {
                // Restablecer checkboxes
                for (int i = 0; i < checkboxes.size(); i++) {
                    checkboxes.get(i).setDisable(false);
                    checkboxes.get(i).setText(tecnicos.get(i).getNombre());
                }
                tfImei.setStyle(imeiStr.isEmpty()
                        ? "-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;"
                        : imeiOk
                                ? "-fx-background-color: white; -fx-border-color: #8AC7AF;" +
                                        "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;"
                                : "-fx-background-color: white; -fx-border-color: " + com.reparaciones.utils.Colores.FILA_INCIDENCIA_BRD + ";" +
                                        "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
                lblImeiErr.setText(!imeiStr.isEmpty() && !imeiOk
                        ? "El IMEI debe tener exactamente 15 dígitos" : "");
            }

            boolean algunoSeleccionado = checkboxes.stream()
                    .anyMatch(cb -> cb.isSelected() && !cb.isDisabled());
            boolean ok = imeiOk && !bloqueadoPorHistorial && algunoSeleccionado;

            btnConfirmar.setDisable(!ok);
            btnConfirmar.setStyle(ok
                    ? "-fx-background-color: #8AC7AF; -fx-text-fill: white; -fx-font-size: 12px;" +
                            "-fx-background-radius: 4; -fx-padding: 8; -fx-cursor: hand;"
                    : "-fx-background-color: #E7E7E7; -fx-text-fill: #A9A9A9;" +
                            "-fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 8;");
        };

        // ── Listeners ─────────────────────────────────────────────────────────
        tfImei.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*"))
                tfImei.setText(n.replaceAll("[^\\d]", ""));
            if (tfImei.getText().length() > 15)
                tfImei.setText(tfImei.getText().substring(0, 15));
            validar.run();
        });

        checkboxes.forEach(cb -> cb.selectedProperty().addListener((obs, o, n) -> validar.run()));

        // ── Confirmar ─────────────────────────────────────────────────────────
        btnConfirmar.setOnAction(ev -> {
            String imei = tfImei.getText().trim();
            try {
                telefonoDAO.insertar(imei); // sabemos que no existe, la validación lo garantiza
                for (int i = 0; i < checkboxes.size(); i++) {
                    if (checkboxes.get(i).isSelected()) {
                        reparacionDAO.insertarAsignacion(imei, tecnicos.get(i).getIdTec());
                    }
                }
                cargar();
                tfImei.clear();
                for (int i = 0; i < checkboxes.size(); i++) {
                    checkboxes.get(i).setSelected(false);
                    checkboxes.get(i).setDisable(false);
                    checkboxes.get(i).setText(tecnicos.get(i).getNombre());
                }
                lblImeiErr.setText("");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        VBox form = new VBox(8, lblImei, tfImei, lblImeiErr, lblTecnicos, listaTecnicos, btnConfirmar);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 8;");
        form.setPrefWidth(560);

        Dialog<Void> formDialog = new Dialog<>();
        formDialog.setTitle("Asignación de Reparación");
        formDialog.getDialogPane().setContent(form);
        formDialog.getDialogPane().setPrefWidth(600);
        formDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        formDialog.showAndWait();
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
        boolean huboConflicto = false;
        for (Map.Entry<String, Tecnico> entry : new java.util.ArrayList<>(cambiosPendientes.entrySet())) {
            String idRep   = entry.getKey();
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
                huboConflicto = true;
            } catch (SQLException e) { e.printStackTrace(); }
        }
        if (huboConflicto) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,
                    "Algunas asignaciones fueron modificadas por otro usuario y no se guardaron.\n" +
                    "Los datos se han recargado.").showAndWait();
            cargar();
        }
        actualizarVisibilidadConfirmar();
        tablaPendientes.refresh();
    }

}