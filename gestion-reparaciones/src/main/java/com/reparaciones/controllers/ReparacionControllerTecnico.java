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

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class ReparacionControllerTecnico {

    @FXML
    private TableView<ReparacionResumen> tablaReparaciones;
    @FXML
    private TableColumn<ReparacionResumen, Void> colAcciones;
    @FXML
    private TableColumn<ReparacionResumen, String> colIdRep;
    @FXML
    private TableColumn<ReparacionResumen, Long> colImei;
    @FXML
    private TableColumn<ReparacionResumen, String> colReparador;
    @FXML
    private TableColumn<ReparacionResumen, String> colFecha;
    @FXML
    private TableColumn<ReparacionResumen, String> colComponente;
    @FXML
    private TableColumn<ReparacionResumen, String> colObservaciones;
    @FXML
    private TableColumn<ReparacionResumen, Void> colIncidencia;
    @FXML
    private TableColumn<ReparacionResumen, String> colIdAnterior;
    @FXML private TextField  filtroImei;
    @FXML private DatePicker filtroFechaDesde;
    @FXML private DatePicker filtroFechaHasta;
    @FXML private MenuButton filtroIncidencias;
    private CheckBox cbIncidenciasAbiertas;
    private CheckBox cbIncidenciasCerradas;
    private CheckBox cbNormales;

    private final ReparacionDAO reparacionDAO = new ReparacionDAO();
    private final ObservableList<ReparacionResumen> datos = FXCollections.observableArrayList();
    private FilteredList<ReparacionResumen> datosFiltrados;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @FXML
    public void initialize() {
        tablaReparaciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        datosFiltrados = new FilteredList<>(datos, p -> true);
        tablaReparaciones.setItems(datosFiltrados);
        configurarColumnas();
        configurarFilas();
        configurarFiltros();
        cargarDatos();
    }

    // ─── Label expandible (click abre popup de lectura) ───────────────────────

    private Label labelExpandible(String titulo, String texto) {
        Label lbl = new Label(texto != null ? texto : "");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setTextOverrun(OverrunStyle.ELLIPSIS);
        if (texto != null && !texto.isEmpty()) {
            lbl.setStyle("-fx-cursor: hand;");
            lbl.setOnMouseClicked(e -> ConfirmDialog.mostrarTexto(titulo, texto));
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

        Image imgHistorial = new Image(getClass().getResourceAsStream("/images/editar.png"));
        colImei.setCellFactory(col -> new TableCell<>() {
            private final Label lblImei = new Label();
            private final ImageView ivHist = new ImageView(imgHistorial);
            private final HBox contenedor = new HBox(6, lblImei, ivHist);
            {
                ivHist.setFitWidth(14);
                ivHist.setFitHeight(14);
                ivHist.setPreserveRatio(true);
                ivHist.setStyle("-fx-cursor: hand; -fx-opacity: 0.5;");
                contenedor.setAlignment(Pos.CENTER_LEFT);
                ivHist.setOnMouseClicked(e -> abrirHistorialImei(getTableView().getItems().get(getIndex()).getImei()));
            }

            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                lblImei.setText(String.valueOf(getTableView().getItems().get(getIndex()).getImei()));
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
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty) {
                    setText(null);
                    return;
                }
                ReparacionResumen rep = getTableView().getItems().get(getIndex());
                setText(rep.getFechaFin() != null ? rep.getFechaFin().format(FORMATO_FECHA) : "");
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
                lblLink.setStyle("-fx-text-fill: #5B8CFF; -fx-cursor: hand;");
                lblLink.setTextOverrun(OverrunStyle.ELLIPSIS);
                lblLink.setMaxWidth(Double.MAX_VALUE);
                lblLink.setOnMouseEntered(
                        e -> lblLink.setStyle("-fx-text-fill: #5B8CFF; -fx-cursor: hand; -fx-underline: true;"));
                lblLink.setOnMouseExited(
                        e -> lblLink.setStyle("-fx-text-fill: #5B8CFF; -fx-cursor: hand; -fx-underline: false;"));
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

        configurarColAcciones();
        configurarColIncidencia();
    }

    private void configurarColAcciones() {
        colAcciones.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
            }
        });
    }

    private void configurarColIncidencia() {
        colIncidencia.setCellFactory(col -> new TableCell<>() {
            private final Label lblComentario = new Label();
            {
                lblComentario.setMaxWidth(Double.MAX_VALUE);
                lblComentario.setTextOverrun(OverrunStyle.ELLIPSIS);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); setStyle(""); return; }
                ReparacionResumen rep = getTableView().getItems().get(getIndex());
                if (!rep.isEsIncidencia()) {
                    setGraphic(null);
                    setStyle("");
                } else if (!rep.isEsResuelto()) {
                    String texto = rep.getIncidencia() != null ? rep.getIncidencia() : "";
                    lblComentario.setText(texto);
                    lblComentario.setStyle("-fx-font-size: 12px; -fx-text-fill: #000000;" +
                            (!texto.isEmpty() ? " -fx-cursor: hand;" : ""));
                    lblComentario.setOnMouseClicked(texto.isEmpty() ? null :
                            e -> ConfirmDialog.mostrarTexto("Incidencia", texto));
                    setStyle("");
                    setGraphic(lblComentario);
                } else {
                    String texto = rep.getIncidencia() != null ? rep.getIncidencia() : "";
                    lblComentario.setText(texto);
                    lblComentario.setStyle("-fx-font-size: 12px; -fx-text-fill: #A9A9A9;" +
                            (!texto.isEmpty() ? " -fx-cursor: hand;" : ""));
                    lblComentario.setOnMouseClicked(texto.isEmpty() ? null :
                            e -> ConfirmDialog.mostrarTexto("Incidencia", texto));
                    setStyle("-fx-background-color: #E7E7E7;");
                    setGraphic(lblComentario);
                }
            }
        });
    }

    private void configurarFilas() {
        tablaReparaciones.setRowFactory(tv -> new TableRow<>() {
            {
                ContextMenu menu = new ContextMenu();
                MenuItem copiar = new MenuItem("📋  Copiar celda");
                copiar.setOnAction(e -> {
                    if (getItem() == null)
                        return;
                    var seleccion = tablaReparaciones.getSelectionModel().getSelectedCells();
                    if (seleccion.isEmpty())
                        return;
                    var pos = seleccion.get(0);
                    String texto = textoDeCelda(getItem(), pos.getTableColumn());
                    if (texto == null || texto.isEmpty())
                        return;
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(texto);
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                });
                menu.getItems().add(copiar);
                setContextMenu(menu);
            }

            @Override
            protected void updateItem(ReparacionResumen item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                if (item.isEsIncidencia() && !item.isEsResuelto()) {
                    setStyle("-fx-background-color: rgba(251,136,136,0.16);" +
                            "-fx-border-color: transparent transparent #FB8888 transparent;" +
                            "-fx-border-width: 0 0 0.2 0;");
                } else {
                    setStyle("");
                }
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
        if (col == colImei)          return String.valueOf(rep.getImei());
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
        } catch (SQLException e) {
            e.printStackTrace();
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
                filtroImei.setStyle("-fx-background-color: #F3F3F3; -fx-border-color: #FB8888;" +
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
        filtroIncidencias.setStyle(
                "-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-background-radius: 4;" +
                "-fx-font-size: 12px;");
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
        itemAbiertas.setStyle("-fx-background-color: white;");
        CustomMenuItem itemCerradas = new CustomMenuItem(cbIncidenciasCerradas, false);
        itemCerradas.setStyle("-fx-background-color: white;");
        CustomMenuItem itemNormales = new CustomMenuItem(cbNormales, false);
        itemNormales.setStyle("-fx-background-color: white;");
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
            if (imeiStr.length() == 15 && !String.valueOf(rep.getImei()).equals(imeiStr))
                return false;
            if (desde != null && rep.getFechaFin() != null
                    && rep.getFechaFin().toLocalDate().isBefore(desde))
                return false;
            if (hasta != null && rep.getFechaFin() != null
                    && rep.getFechaFin().toLocalDate().isAfter(hasta))
                return false;
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

    // ─── Modal pendientes ─────────────────────────────────────────────────────

    @FXML
    private void abrirModalPendientes() {
        PendientesTecnicoController.abrir(this::cargarDatos);
    }

    // ─── Historial IMEI ───────────────────────────────────────────────────────

    private void abrirHistorialImei(long imei) {
        try {
            List<ReparacionResumen> historial = reparacionDAO.getResumenPorImei(imei);

            TableColumn<ReparacionResumen, String> cId = new TableColumn<>("ID");
            TableColumn<ReparacionResumen, String> cTecnico = new TableColumn<>("Técnico");
            TableColumn<ReparacionResumen, String> cFecha = new TableColumn<>("Fecha");
            TableColumn<ReparacionResumen, String> cComp = new TableColumn<>("Componente");
            TableColumn<ReparacionResumen, String> cObs = new TableColumn<>("Observaciones");
            TableColumn<ReparacionResumen, String> cIncid = new TableColumn<>("Incidencia");

            cId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getIdRep()));
            cTecnico.setCellValueFactory(
                    d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreTecnico()));
            cFecha.setCellValueFactory(d -> {
                ReparacionResumen r = d.getValue();
                String texto = r.getFechaFin() != null
                        ? r.getFechaFin().format(FORMATO_FECHA)
                        : r.getFechaAsig() != null
                                ? "Asig. " + r.getFechaAsig().format(FORMATO_FECHA)
                                : "";
                return new javafx.beans.property.SimpleStringProperty(texto);
            });
            cComp.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue().getTipoComponente() != null ? d.getValue().getTipoComponente() : "—"));
            cObs.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue().getObservaciones() != null ? d.getValue().getObservaciones() : ""));
            cIncid.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue().isEsIncidencia() && d.getValue().getIncidencia() != null
                            ? d.getValue().getIncidencia()
                            : ""));

            TableView<ReparacionResumen> tabla = new TableView<>();
            tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            tabla.setPrefHeight(300);
            tabla.getColumns().addAll(cId, cTecnico, cFecha, cComp, cObs, cIncid);
            tabla.setItems(FXCollections.observableArrayList(historial));

            VBox contenido = new VBox(8, new Label("Historial completo del IMEI: " + imei), tabla);
            contenido.setPadding(new Insets(8));

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Historial IMEI");
            dialog.getDialogPane().setContent(contenido);
            dialog.getDialogPane().setPrefWidth(750);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void descargarHistorial() {
        // TODO: exportación TXT
    }
}