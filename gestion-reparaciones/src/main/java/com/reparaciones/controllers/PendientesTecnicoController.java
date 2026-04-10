package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.ReparacionDAO;
import com.reparaciones.models.ReparacionResumen;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class PendientesTecnicoController {

    @FXML private TableView<ReparacionResumen>           tablaPendientes;
    @FXML private TableColumn<ReparacionResumen, String> cId;
    @FXML private TableColumn<ReparacionResumen, String> cImei;
    @FXML private TableColumn<ReparacionResumen, String> cFecha;
    @FXML private TableColumn<ReparacionResumen, Void>   cAccion;
    @FXML private MenuButton filtroSolicitud;
    @FXML private Label      lblUltimaActualizacion;

    private final ReparacionDAO reparacionDAO = new ReparacionDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final ObservableList<ReparacionResumen> datos = FXCollections.observableArrayList();
    private FilteredList<ReparacionResumen> datosFiltrados;
    private CheckBox cbSoloSolicitudes;
    private CheckBox cbSoloIncidencias;
    private CheckBox cbSoloAsignaciones;

    private Runnable onCerrar;
    private Runnable onVolverAHistorial;

    @FXML
    public void initialize() {
        tablaPendientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        cId.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getIdRep()));
        cImei.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getImei()));
        cFecha.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
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
                } else setStyle("");
            }
            @Override protected void updateItem(ReparacionResumen item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstilo();
            }
        });

        cAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Añadir reparación");
            {
                btn.getStyleClass().add("btn-primary");
                btn.setOnAction(e -> {
                    ReparacionResumen asig = getTableView().getItems().get(getIndex());
                    if (onVolverAHistorial != null) onVolverAHistorial.run();
                    FormularioReparacionController.abrir(
                            asig.getImei(), null, asig.getIdRep(), onCerrar);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        configurarFiltros();
        cargar();
    }

    // ─── Filtros ──────────────────────────────────────────────────────────────

    private void configurarFiltros() {
        filtroSolicitud.setStyle("-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        cbSoloSolicitudes  = new CheckBox("Solicitudes pieza");
        cbSoloIncidencias  = new CheckBox("Incidencias");
        cbSoloAsignaciones = new CheckBox("Asignaciones");

        for (CheckBox cb : new CheckBox[]{cbSoloSolicitudes, cbSoloIncidencias, cbSoloAsignaciones}) {
            cb.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
            cb.selectedProperty().addListener((obs, o, n) -> {
                actualizarTextoFiltroSolicitud();
                aplicarFiltros();
            });
            CustomMenuItem item = new CustomMenuItem(cb, false);
            item.setStyle("-fx-background-color: white;");
            filtroSolicitud.getItems().add(item);
        }
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
        boolean filtrarSol  = cbSoloSolicitudes.isSelected();
        boolean filtrarInc  = cbSoloIncidencias.isSelected();
        boolean filtrarAsig = cbSoloAsignaciones.isSelected();
        datosFiltrados.setPredicate(rep -> {
            if (!filtrarSol && !filtrarInc && !filtrarAsig) return true;
            boolean esSol  = rep.getEsSolicitud() == 1;
            boolean esInc  = rep.isEsIncidencia();
            boolean esAsig = !esSol && !esInc;
            boolean mostrar = false;
            if (filtrarSol  && esSol)  mostrar = true;
            if (filtrarInc  && esInc)  mostrar = true;
            if (filtrarAsig && esAsig) mostrar = true;
            return mostrar;
        });
    }

    @FXML
    private void limpiarFiltros() {
        cbSoloSolicitudes.setSelected(false);
        cbSoloIncidencias.setSelected(false);
        cbSoloAsignaciones.setSelected(false);
        filtroSolicitud.setText("Tipo");
    }

    // ─── Carga ────────────────────────────────────────────────────────────────

    public void cargar() {
        try {
            Integer idTec = Sesion.getIdTec();
            if (idTec == null) return;
            datos.setAll(reparacionDAO.getAsignacionesPorTecnico(idTec));
            String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            if (lblUltimaActualizacion != null) lblUltimaActualizacion.setText("Actualizado " + hora);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setOnCerrar(Runnable onCerrar) {
        this.onCerrar = onCerrar;
    }

    public void setOnVolverAHistorial(Runnable onVolverAHistorial) {
        this.onVolverAHistorial = onVolverAHistorial;
    }
}