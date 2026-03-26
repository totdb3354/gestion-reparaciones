package com.reparaciones.controllers;

import com.reparaciones.dao.ReparacionComponenteDAO;
import com.reparaciones.dao.ReparacionDAO;
import com.reparaciones.dao.TecnicoDAO;
import com.reparaciones.dao.TelefonoDAO;
import com.reparaciones.utils.ConfirmDialog;
import com.reparaciones.models.ReparacionResumen;
import com.reparaciones.models.Tecnico;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class ReparacionControllerAdmin {

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
    @FXML private MenuButton filtroTecnico;
    @FXML private DatePicker filtroFechaDesde;
    @FXML private DatePicker filtroFechaHasta;
    @FXML private MenuButton filtroIncidencias;
    private CheckBox cbIncidenciasAbiertas;
    private CheckBox cbIncidenciasCerradas;
    private CheckBox cbNormales;

    private final ReparacionDAO reparacionDAO = new ReparacionDAO();
    private final ReparacionComponenteDAO reparacionComponenteDAO = new ReparacionComponenteDAO();
    private final TecnicoDAO tecnicoDAO = new TecnicoDAO();
    private final TelefonoDAO telefonoDAO = new TelefonoDAO();

    private ObservableList<ReparacionResumen> datos = FXCollections.observableArrayList();
    private FilteredList<ReparacionResumen> datosFiltrados;
    private final List<CheckBox> checksTecnico = new java.util.ArrayList<>();
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @FXML
    public void initialize() {
        tablaReparaciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        configurarColumnas();
        configurarFilas();
        configurarFiltros();
        cargarDatos();
    }

    // ─── Label expandible (click abre popup de lectura) ───────────────────────

    private Label labelExpandible(String titulo, String texto) {
        Label lbl = new Label(texto != null ? texto : "");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
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
                lblLink.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
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
        Image imgBorrar = new Image(getClass().getResourceAsStream("/images/borrar.png"));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final ImageView ivBorrar = new ImageView(imgBorrar);
            private final HBox contenedor = new HBox(ivBorrar);
            {
                ivBorrar.setFitWidth(16);
                ivBorrar.setFitHeight(16);
                ivBorrar.setPreserveRatio(true);
                ivBorrar.setStyle("-fx-cursor: hand;");
                contenedor.setAlignment(Pos.CENTER);
                ivBorrar.setOnMouseClicked(e -> borrarReparacion(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : contenedor);
            }
        });
    }

    private void configurarColIncidencia() {
        Image imgBorrar = new Image(getClass().getResourceAsStream("/images/borrar.png"));
        Image imgLapiz = new Image(getClass().getResourceAsStream("/images/añadir_incidencia.png"));

        colIncidencia.setCellFactory(col -> new TableCell<>() {
            private final ImageView ivLapiz = new ImageView(imgLapiz);
            private final Button btnAniadir = new Button("Añadir una incidencia");
            private final ImageView ivBorrar = new ImageView(imgBorrar);
            private final Button btnBorrarIncidencia = new Button();
            private final Label lblComentario = new Label();
            private final HBox casoUno = new HBox(btnAniadir);
            private final HBox casoDos = new HBox(8, btnBorrarIncidencia, lblComentario);
            {
                ivLapiz.setFitWidth(16);
                ivLapiz.setFitHeight(16);
                ivLapiz.setPreserveRatio(true);
                btnAniadir.setGraphic(ivLapiz);
                btnAniadir.setContentDisplay(ContentDisplay.LEFT);
                btnAniadir.setStyle(
                        "-fx-background-color: #FB8888; -fx-text-fill: white;" +
                                "-fx-font-weight: bold; -fx-font-size: 12px;" +
                                "-fx-cursor: hand; -fx-background-radius: 0;");
                HBox.setHgrow(btnAniadir, Priority.ALWAYS);
                btnAniadir.setMaxWidth(Double.MAX_VALUE);
                casoUno.setMaxWidth(Double.MAX_VALUE);

                ivBorrar.setFitWidth(16);
                ivBorrar.setFitHeight(16);
                ivBorrar.setPreserveRatio(true);
                btnBorrarIncidencia.setGraphic(ivBorrar);
                btnBorrarIncidencia.setStyle(
                        "-fx-background-color: #FB8888; -fx-background-radius: 2;" +
                                "-fx-min-width: 35; -fx-max-width: 35;" +
                                "-fx-min-height: 35; -fx-max-height: 35; -fx-cursor: hand;");
                lblComentario.setMaxWidth(Double.MAX_VALUE);
                lblComentario.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                HBox.setHgrow(lblComentario, Priority.ALWAYS);
                casoDos.setAlignment(Pos.CENTER_LEFT);
                casoDos.setMaxWidth(Double.MAX_VALUE);

                btnAniadir.setOnAction(e -> abrirDialogoIncidencia(getTableView().getItems().get(getIndex())));
                btnBorrarIncidencia.setOnAction(e -> borrarIncidencia(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); setStyle(""); return; }
                ReparacionResumen rep = getTableView().getItems().get(getIndex());
                if (rep.isEsIncidencia()) {
                    String texto = rep.getIncidencia() != null ? rep.getIncidencia() : "";
                    lblComentario.setText(texto);
                    lblComentario.setStyle("-fx-font-size: 12px; -fx-text-fill: #000000;" +
                            (!texto.isEmpty() ? " -fx-cursor: hand;" : ""));
                    lblComentario.setOnMouseClicked(texto.isEmpty() ? null :
                            e -> ConfirmDialog.mostrarTexto("Incidencia", texto));
                    if (rep.isEsResuelto()) {
                        casoDos.setStyle("-fx-background-color: #E7E7E7;");
                        btnBorrarIncidencia.setVisible(false);
                        btnBorrarIncidencia.setManaged(false);
                    } else {
                        casoDos.setStyle("");
                        btnBorrarIncidencia.setVisible(true);
                        btnBorrarIncidencia.setManaged(true);
                    }
                    setStyle("");
                    setGraphic(casoDos);
                } else {
                    setStyle("");
                    setGraphic(casoUno);
                }
            }
        });
    }

    private void configurarFilas() {
        tablaReparaciones.setRowFactory(tv -> new TableRow<>() {
            {
                // Menú contextual — copiar celda seleccionada
                ContextMenu menu = new ContextMenu();
                MenuItem copiar = new MenuItem("📋  Copiar celda");// podemos meter un png para q sea mas bonito despues
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
            datos.setAll(reparacionDAO.getReparacionesResumen());
            if (datosFiltrados == null) {
                datosFiltrados = new FilteredList<>(datos, p -> true);
                tablaReparaciones.setItems(datosFiltrados);
            }
            aplicarFiltros();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Filtros ──────────────────────────────────────────────────────────────

    private void configurarFiltros() {
        // Cargar checkboxes de técnicos en el MenuButton
        filtroTecnico.setStyle(
                "-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-background-radius: 4;" +
                "-fx-font-size: 12px;");
        try {
            List<com.reparaciones.models.Tecnico> tecnicos = tecnicoDAO.getAll();
            for (com.reparaciones.models.Tecnico t : tecnicos) {
                CheckBox cb = new CheckBox(t.getNombre());
                cb.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
                cb.selectedProperty().addListener((obs, o, n) -> {
                    actualizarTextoFiltroTecnico();
                    aplicarFiltros();
                });
                checksTecnico.add(cb);
                CustomMenuItem item = new CustomMenuItem(cb, false);
                item.setStyle("-fx-background-color: white;");
                filtroTecnico.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Listeners de los demás filtros
        filtroImei.textProperty().addListener((obs, o, n) -> {
            // Solo números, máx 15
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
        // Deshabilitar escritura manual — solo selección por calendario
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
        List<String> tecnicosSeleccionados = checksTecnico.stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(java.util.stream.Collectors.toList());

        datosFiltrados.setPredicate(rep -> {
            if (imeiStr.length() == 15 && !String.valueOf(rep.getImei()).equals(imeiStr))
                return false;
            if (!tecnicosSeleccionados.isEmpty() && !tecnicosSeleccionados.contains(rep.getNombreTecnico()))
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

    private void actualizarTextoFiltroTecnico() {
        List<String> seleccionados = checksTecnico.stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(java.util.stream.Collectors.toList());
        if (seleccionados.isEmpty())
            filtroTecnico.setText("Técnico");
        else if (seleccionados.size() == 1)
            filtroTecnico.setText(seleccionados.get(0));
        else
            filtroTecnico.setText(seleccionados.size() + " técnicos");
    }

    @FXML
    private void limpiarFiltros() {
        filtroImei.clear();
        filtroImei.setStyle("");
        checksTecnico.forEach(cb -> cb.setSelected(false));
        filtroTecnico.setText("Técnico");
        filtroFechaDesde.setValue(null);
        filtroFechaHasta.setValue(null);
        cbIncidenciasAbiertas.setSelected(false);
        cbIncidenciasCerradas.setSelected(false);
        cbNormales.setSelected(false);
        filtroIncidencias.setText("Incidencias");
    }

    // ─── Modal pendientes ─────────────────────────────────────────────────────

    @FXML
    private void abrirModalPendientes() {
        PendientesAdminController.abrir(this::cargarDatos);
    }

    @FXML
    private void abrirMisPendientes() {
        PendientesTecnicoController.abrir(this::cargarDatos);
    }

    // ─── Formulario asignación ────────────────────────────────────────────────

    private void abrirFormularioAsignacion(TableView<ReparacionResumen> tablaModal) {
        Label lblImei = new Label("Introduzca IMEI de teléfono a reparar");
        TextField tfImei = new TextField();
        Label lblImeiErr = new Label();
        tfImei.setPromptText("Introduce un IMEI para identificar");
        tfImei.setStyle("-fx-background-color: white; -fx-border-color: #A9A9A9; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
        lblImeiErr.setStyle("-fx-font-size: 11px; -fx-text-fill: #FB8888;");

        tfImei.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*"))
                tfImei.setText(n.replaceAll("[^\\d]", ""));
            if (tfImei.getText().length() > 15)
                tfImei.setText(tfImei.getText().substring(0, 15));
        });

        Label lblTecnico = new Label("Técnico a asignar");
        ComboBox<Tecnico> cbTecnico = new ComboBox<>();
        cbTecnico.setPromptText("Selecciona técnico a asignar reparación");
        cbTecnico.setMaxWidth(Double.MAX_VALUE);
        cbTecnico.setStyle("-fx-background-color: white; -fx-border-color: #A9A9A9; " +
                "-fx-border-radius: 4; -fx-background-radius: 4;");
        try {
            cbTecnico.getItems().addAll(tecnicoDAO.getAll());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        cbTecnico.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Tecnico t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getNombre());
            }
        });
        cbTecnico.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Tecnico t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "Selecciona técnico a asignar reparación" : t.getNombre());
            }
        });

        Button btnConfirmar = new Button("Asignar reparación");
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        btnConfirmar.setDisable(true);
        btnConfirmar.setStyle(
                "-fx-background-color: #E7E7E7; -fx-text-fill: #A9A9A9;" +
                        "-fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 8;");

        Runnable validar = () -> {
            String imeiStr = tfImei.getText().trim();
            boolean imeiOk = imeiStr.length() == 15;
            boolean ok = imeiOk && cbTecnico.getValue() != null;
            tfImei.setStyle(imeiStr.isEmpty()
                    ? "-fx-background-color: white; -fx-border-color: #A9A9A9; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;"
                    : imeiOk
                            ? "-fx-background-color: white; -fx-border-color: #8AC7AF; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;"
                            : "-fx-background-color: white; -fx-border-color: #FB8888; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
            lblImeiErr.setText(!imeiStr.isEmpty() && !imeiOk ? "El IMEI debe tener exactamente 15 dígitos" : "");
            btnConfirmar.setDisable(!ok);
            btnConfirmar.setStyle(ok
                    ? "-fx-background-color: #8AC7AF; -fx-text-fill: white; -fx-font-size: 12px; " +
                            "-fx-background-radius: 4; -fx-padding: 8; -fx-cursor: hand;"
                    : "-fx-background-color: #E7E7E7; -fx-text-fill: #A9A9A9; -fx-font-size: 12px; " +
                            "-fx-background-radius: 4; -fx-padding: 8;");
        };
        tfImei.textProperty().addListener((obs, o, n) -> validar.run());
        cbTecnico.valueProperty().addListener((obs, o, n) -> validar.run());

        btnConfirmar.setOnAction(ev -> {
            long imei = Long.parseLong(tfImei.getText().trim());
            try {
                if (!telefonoDAO.exists(imei))
                    telefonoDAO.insertar(imei);
                reparacionDAO.insertarAsignacion(imei, cbTecnico.getValue().getIdTec());
                tablaModal.getItems().setAll(reparacionDAO.getAsignaciones());
                tfImei.clear();
                cbTecnico.setValue(null);
                lblImeiErr.setText("");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        VBox form = new VBox(6, lblImei, tfImei, lblImeiErr, lblTecnico, cbTecnico, btnConfirmar);
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

    // ─── Incidencias ──────────────────────────────────────────────────────────

    private void abrirDialogoIncidencia(ReparacionResumen rep) {

        // ── Campo comentario ──────────────────────────────────────────────────
        Label lblComentario = new Label("Comentario de incidencia");
        String textoInicial = rep.getIncidencia() != null ? rep.getIncidencia() : "";
        TextArea tfComentario = new TextArea(textoInicial);
        tfComentario.setPromptText("Describe la incidencia...");
        tfComentario.setWrapText(true);
        tfComentario.setPrefRowCount(4);
        tfComentario.setStyle("-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");

        // ── Selector técnico ──────────────────────────────────────────────────
        Label lblTecnico = new Label("Técnico asignado");
        ComboBox<Tecnico> cbTecnico = new ComboBox<>();
        cbTecnico.setMaxWidth(Double.MAX_VALUE);
        cbTecnico.setStyle("-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-background-radius: 4;");

        try {
            List<Tecnico> tecnicos = tecnicoDAO.getAll();
            cbTecnico.getItems().addAll(tecnicos);
            // Preseleccionar el técnico actual de la reparación
            tecnicos.stream()
                    .filter(t -> t.getIdTec() == rep.getIdTec())
                    .findFirst()
                    .ifPresent(cbTecnico::setValue);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        cbTecnico.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Tecnico t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getNombre());
            }
        });
        cbTecnico.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Tecnico t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "Selecciona técnico" : t.getNombre());
            }
        });

        // ── Botón confirmar ───────────────────────────────────────────────────
        Button btnConfirmar = new Button("Añadir incidencia y asignar");
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        btnConfirmar.setStyle("-fx-background-color: #E7E7E7; -fx-text-fill: #A9A9A9;" +
                "-fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 8;");

        Runnable validar = () -> {
            boolean ok = !tfComentario.getText().trim().isEmpty() && cbTecnico.getValue() != null;
            btnConfirmar.setDisable(!ok);
            btnConfirmar.setStyle(ok
                    ? "-fx-background-color: #8AC7AF; -fx-text-fill: white; -fx-font-size: 12px;" +
                            "-fx-background-radius: 4; -fx-padding: 8; -fx-cursor: hand;"
                    : "-fx-background-color: #E7E7E7; -fx-text-fill: #A9A9A9; -fx-font-size: 12px;" +
                            "-fx-background-radius: 4; -fx-padding: 8;");
        };

        // Validar estado inicial
        validar.run();
        tfComentario.textProperty().addListener((obs, o, n) -> validar.run());
        cbTecnico.valueProperty().addListener((obs, o, n) -> validar.run());

        VBox form = new VBox(8,
                lblComentario, tfComentario,
                lblTecnico, cbTecnico,
                btnConfirmar);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 8;");
        form.setPrefWidth(480);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Añadir incidencia");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        btnConfirmar.setOnAction(e -> {
            String comentario = tfComentario.getText().trim();
            int idTec = cbTecnico.getValue().getIdTec();
            try {
                reparacionDAO.marcarIncidenciaYAsignar(rep.getIdRep(), comentario, rep.getImei(), idTec);
                dialog.close();
                cargarDatos();
            } catch (SQLException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR,
                        "No se pudo guardar: " + ex.getMessage()).showAndWait();
            }
        });

        dialog.showAndWait();
    }

    private void borrarIncidencia(ReparacionResumen rep) {
        ConfirmDialog.mostrar(
                "Borrar incidencia",
                "Esta acción solo es válida si fue un error al añadirla.",
                "Borrar incidencia",
                () -> {
                    try {
                        reparacionComponenteDAO.borrarIncidencia(rep.getIdRep());
                        cargarDatos();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    private void borrarReparacion(ReparacionResumen rep) {
        try {
            String ref = reparacionDAO.getReferenciadora(rep.getIdRep());
            if (ref != null) {
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                alerta.setTitle("No se puede borrar");
                alerta.setHeaderText("Esta reparación está siendo referenciada");
                alerta.setContentText("La reparación " + ref + " apunta a esta. Bórrala primero.");
                alerta.showAndWait();
                return;
            }
            ConfirmDialog.mostrar(
                    "Borrar reparación",
                    "Se borrará " + rep.getIdRep() + " y no se podrá recuperar.",
                    "Borrar reparación",
                    () -> {
                        try {
                            reparacionDAO.eliminar(rep.getIdRep());
                            cargarDatos();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
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