package com.reparaciones.controllers;

import com.reparaciones.dao.CompraComponenteDAO;
import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.dao.ProveedorDAO;
import com.reparaciones.dao.TipoCambioDAO;
import com.reparaciones.models.Componente;
import com.reparaciones.models.CompraComponente;
import com.reparaciones.models.LineaPedido;
import com.reparaciones.models.Proveedor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Controlador del formulario de nuevo pedido de componentes.
 * <p>Permite al administrador crear pedidos multi-línea: cada línea incluye
 * componente, proveedor (con divisa asociada), cantidad, precio y urgencia.</p>
 *
 * @role ADMIN
 */
public class FormularioCompraController {

    @FXML private Label                                   lblTitulo;

    @FXML private TableView<LineaPedido>                  tablaLineas;
    @FXML private TableColumn<LineaPedido, Componente>    colComponente;
    @FXML private TableColumn<LineaPedido, Proveedor>     colProveedor;
    @FXML private TableColumn<LineaPedido, Integer>       colCantidad;
    @FXML private TableColumn<LineaPedido, Double>        colPrecio;
    @FXML private TableColumn<LineaPedido, Boolean>       colUrgente;
    @FXML private TableColumn<LineaPedido, String>        colTotalEur;
    @FXML private TableColumn<LineaPedido, Void>          colQuitar;

    private static final String ESTILO_EDITABLE =
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: #C2C8D0;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 3;" +
            "-fx-background-radius: 3;" +
            "-fx-padding: 5 8 5 8;" +
            "-fx-text-fill: #2C3B54;";

    private final ComponenteDAO       componenteDAO = new ComponenteDAO();
    private final ProveedorDAO        proveedorDAO  = new ProveedorDAO();
    private final TipoCambioDAO       tipoCambioDAO = new TipoCambioDAO();
    private final CompraComponenteDAO compraDAO     = new CompraComponenteDAO();

    private final ObservableList<LineaPedido> lineas = FXCollections.observableArrayList();
    private ObservableList<Componente> componentesDisponibles;
    private ObservableList<Proveedor>  proveedoresDisponibles;

    private Runnable onGuardado;

    // ─── Init ─────────────────────────────────────────────────────────────────

    public void init(Componente preselect, Runnable onGuardado) {
        this.onGuardado = onGuardado;
        try {
            componentesDisponibles = FXCollections.observableArrayList(
                    componenteDAO.getAllGestionados().stream()
                            .filter(com.reparaciones.models.Componente::isActivo)
                            .collect(java.util.stream.Collectors.toList()));
            proveedoresDisponibles = FXCollections.observableArrayList(proveedorDAO.getActivos());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        configurarTabla();
        tablaLineas.setItems(lineas);
        if (preselect != null) añadirFila(preselect);
    }

    // ─── Tasa de cambio por línea ─────────────────────────────────────────────

    private void fetchTasaParaLinea(LineaPedido linea, String divisa) {
        if ("EUR".equalsIgnoreCase(divisa)) {
            linea.setTasa(1.0);
            tablaLineas.refresh();
            return;
        }
        new Thread(() -> {
            try {
                double t = tipoCambioDAO.getTasa(divisa);
                Platform.runLater(() -> { linea.setTasa(t); tablaLineas.refresh(); });
            } catch (SQLException e) {
                Platform.runLater(tablaLineas::refresh);
            }
        }, "tasa-fetch").start();
    }

    // ─── Tabla editable ───────────────────────────────────────────────────────

    private void configurarTabla() {
        tablaLineas.setEditable(true);

        // Componente — ComboBox siempre visible
        colComponente.setCellValueFactory(c -> c.getValue().componenteProperty());
        colComponente.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<Componente> combo = new ComboBox<>(componentesDisponibles);
            {
                combo.setCellFactory(lv -> new ListCell<>() {
                    {
                        setOnMouseEntered(e -> { if (!isEmpty() && getItem() != null)
                            setStyle("-fx-text-fill: #2C3B54; -fx-background-color: #DDE1E7;"); });
                        setOnMouseExited(e -> { if (!isEmpty() && getItem() != null)
                            setStyle("-fx-text-fill: #2C3B54; -fx-background-color: white;"); });
                    }
                    @Override protected void updateItem(Componente c, boolean empty) {
                        super.updateItem(c, empty);
                        setText(empty || c == null ? null : c.toString());
                        setStyle("-fx-text-fill: #2C3B54; -fx-background-color: white;");
                    }
                });
                combo.setButtonCell(new ListCell<>() {
                    @Override protected void updateItem(Componente c, boolean empty) {
                        super.updateItem(c, empty);
                        setText(empty || c == null ? "" : c.toString());
                        setStyle("-fx-text-fill: #FAFAFA;");
                    }
                });
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.setOnAction(e -> {
                    Componente val = combo.getValue();
                    int idx = getIndex();
                    if (val != null && idx >= 0 && idx < getTableView().getItems().size())
                        getTableView().getItems().get(idx).setComponente(val);
                });
            }
            @Override protected void updateItem(Componente item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); }
                else { combo.setValue(item); setGraphic(combo); }
            }
        });

        // Proveedor — ComboBox siempre visible, auto-fetch tasa al cambiar
        colProveedor.setCellValueFactory(c -> c.getValue().proveedorProperty());
        colProveedor.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<Proveedor> combo = new ComboBox<>(proveedoresDisponibles);
            {
                combo.setCellFactory(lv -> new ListCell<>() {
                    {
                        setOnMouseEntered(e -> { if (!isEmpty() && getItem() != null)
                            setStyle("-fx-text-fill: #2C3B54; -fx-background-color: #DDE1E7;"); });
                        setOnMouseExited(e -> { if (!isEmpty() && getItem() != null)
                            setStyle("-fx-text-fill: #2C3B54; -fx-background-color: white;"); });
                    }
                    @Override protected void updateItem(Proveedor p, boolean empty) {
                        super.updateItem(p, empty);
                        setText(empty || p == null ? null : p.getNombre());
                        setStyle("-fx-text-fill: #2C3B54; -fx-background-color: white;");
                    }
                });
                combo.setButtonCell(new ListCell<>() {
                    @Override protected void updateItem(Proveedor p, boolean empty) {
                        super.updateItem(p, empty);
                        setText(empty || p == null ? "" : p.getNombre());
                        setStyle("-fx-text-fill: #FAFAFA;");
                    }
                });
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.setOnAction(e -> {
                    Proveedor val = combo.getValue();
                    int idx = getIndex();
                    if (val != null && idx >= 0 && idx < getTableView().getItems().size()) {
                        LineaPedido linea = getTableView().getItems().get(idx);
                        linea.setProveedor(val);
                        fetchTasaParaLinea(linea, val.getDivisa());
                    }
                });
            }
            @Override protected void updateItem(Proveedor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); }
                else { combo.setValue(item); setGraphic(combo); }
            }
        });

        // Cantidad — celda personalizada
        colCantidad.setCellValueFactory(c -> c.getValue().cantidadProperty().asObject());
        colCantidad.setCellFactory(col -> new TableCell<>() {
            private final Label     lbl = new Label();
            private final TextField tf  = new TextField();
            {
                lbl.setStyle(ESTILO_EDITABLE);
                lbl.setMaxWidth(Double.MAX_VALUE);
                tf.setPrefWidth(Double.MAX_VALUE);
                tf.setOnAction(e -> commitEntero());
                tf.focusedProperty().addListener((o, was, focused) -> { if (!focused) commitEntero(); });
                tableRowProperty().addListener((obs, or, nr) -> {
                    if (nr != null) nr.selectedProperty().addListener((o, w, s) -> refresh());
                });
            }
            private void commitEntero() {
                try { commitEdit(Integer.parseInt(tf.getText().trim())); }
                catch (NumberFormatException e) { cancelEdit(); }
            }
            private void refresh() {
                if (isEmpty() || getItem() == null) { setText(null); setGraphic(null); setStyle(""); return; }
                boolean sel = getTableRow() != null && getTableRow().isSelected();
                if (isEditing()) {
                    setStyle("-fx-background-color: transparent; -fx-padding: 3;");
                    setText(null); setGraphic(tf);
                } else if (sel) {
                    setStyle("-fx-background-color: transparent; -fx-padding: 3;");
                    lbl.setText(String.valueOf(getItem()));
                    setText(null); setGraphic(lbl);
                } else {
                    setStyle(""); setText(String.valueOf(getItem())); setGraphic(null);
                }
            }
            @Override public void startEdit() {
                super.startEdit();
                tf.setText(getItem() == null ? "" : String.valueOf(getItem()));
                setStyle("-fx-background-color: transparent; -fx-padding: 3;");
                setText(null); setGraphic(tf);
                tf.selectAll(); tf.requestFocus();
            }
            @Override public void cancelEdit() { super.cancelEdit(); refresh(); }
            @Override protected void updateItem(Integer item, boolean empty) { super.updateItem(item, empty); refresh(); }
        });
        colCantidad.setOnEditCommit(e -> {
            if (e.getNewValue() != null && e.getNewValue() > 0)
                e.getRowValue().setCantidad(e.getNewValue());
            tablaLineas.refresh();
        });

        // Precio — celda personalizada, símbolo según divisa del proveedor de la línea
        colPrecio.setCellValueFactory(c -> c.getValue().precioUnidadProperty().asObject());
        colPrecio.setCellFactory(col -> new TableCell<>() {
            private final Label     lbl = new Label();
            private final TextField tf  = new TextField();
            {
                lbl.setStyle(ESTILO_EDITABLE);
                lbl.setMaxWidth(Double.MAX_VALUE);
                tf.setPrefWidth(Double.MAX_VALUE);
                tf.setOnAction(e -> commitDecimal());
                tf.focusedProperty().addListener((o, was, focused) -> { if (!focused) commitDecimal(); });
                tableRowProperty().addListener((obs, or, nr) -> {
                    if (nr != null) nr.selectedProperty().addListener((o, w, s) -> refresh());
                });
            }
            private void commitDecimal() {
                try { commitEdit(Double.parseDouble(tf.getText().trim().replace(",", "."))); }
                catch (NumberFormatException e) { cancelEdit(); }
            }
            private String simb() {
                LineaPedido l = getTableRow() != null ? getTableRow().getItem() : null;
                if (l == null || l.getProveedor() == null) return "€";
                return "USD".equals(l.getProveedor().getDivisa()) ? "$" : "€";
            }
            private void refresh() {
                if (isEmpty() || getItem() == null) { setText(null); setGraphic(null); setStyle(""); return; }
                boolean sel = getTableRow() != null && getTableRow().isSelected();
                if (isEditing()) {
                    setStyle("-fx-background-color: transparent; -fx-padding: 3;");
                    setText(null); setGraphic(tf);
                } else if (sel) {
                    setStyle("-fx-background-color: transparent; -fx-padding: 3;");
                    lbl.setText(String.format("%.2f %s", getItem(), simb()));
                    setText(null); setGraphic(lbl);
                } else {
                    setStyle(""); setText(String.format("%.2f %s", getItem(), simb())); setGraphic(null);
                }
            }
            @Override public void startEdit() {
                super.startEdit();
                tf.setText(getItem() == null ? "" : String.valueOf(getItem()));
                setStyle("-fx-background-color: transparent; -fx-padding: 3;");
                setText(null); setGraphic(tf);
                tf.selectAll(); tf.requestFocus();
            }
            @Override public void cancelEdit() { super.cancelEdit(); refresh(); }
            @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); refresh(); }
        });
        colPrecio.setOnEditCommit(e -> {
            if (e.getNewValue() != null && e.getNewValue() >= 0)
                e.getRowValue().setPrecioUnidad(e.getNewValue());
            tablaLineas.refresh();
        });

        // Urgente — CheckBox
        colUrgente.setCellValueFactory(c -> c.getValue().esUrgenteProperty());
        colUrgente.setCellFactory(CheckBoxTableCell.forTableColumn(colUrgente));

        // Total EUR — usa la tasa de la línea
        colTotalEur.setCellValueFactory(c -> {
            LineaPedido l = c.getValue();
            double total = l.getPrecioUnidad() * l.getTasa() * l.getCantidad();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.2f €", total));
        });

        // Selección azul
        tablaLineas.setRowFactory(tv -> new TableRow<>() {
            { selectedProperty().addListener((obs, o, sel) -> actualizarEstilo()); }
            private void actualizarEstilo() {
                if (isEmpty() || getItem() == null) { setStyle(""); return; }
                setStyle(isSelected()
                        ? "-fx-background-color: #2C3B54;" +
                          "-fx-border-color: transparent transparent #3D5070 transparent;" +
                          "-fx-border-width: 0 0 0.2 0;"
                        : "");
            }
            @Override protected void updateItem(LineaPedido item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstilo();
            }
        });

        // Quitar fila
        Image imgBorrar = new Image(getClass().getResourceAsStream("/images/borrar.png"));
        colQuitar.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv  = new ImageView(imgBorrar);
            private final HBox      box = new HBox(iv);
            {
                iv.setFitWidth(25); iv.setFitHeight(25); iv.setPreserveRatio(true);
                iv.setStyle("-fx-cursor: hand;");
                box.setAlignment(Pos.CENTER);
                iv.setOnMouseClicked(e -> lineas.remove(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ─── Añadir fila ──────────────────────────────────────────────────────────

    @FXML private void anadirLinea() { añadirFila(null); }

    private void añadirFila(Componente preselect) {
        LineaPedido linea = new LineaPedido();
        if (preselect != null && componentesDisponibles != null)
            componentesDisponibles.stream()
                    .filter(c -> c.getIdCom() == preselect.getIdCom())
                    .findFirst()
                    .ifPresent(linea::setComponente);
        lineas.add(linea);
        tablaLineas.getSelectionModel().selectLast();
        tablaLineas.scrollTo(lineas.size() - 1);
    }

    // ─── Confirmar ────────────────────────────────────────────────────────────

    @FXML private void confirmar() {
        if (lineas.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Añade al menos una línea.").showAndWait();
            return;
        }
        for (int i = 0; i < lineas.size(); i++) {
            LineaPedido l = lineas.get(i);
            if (l.getComponente() == null) {
                new Alert(Alert.AlertType.WARNING, "Línea " + (i+1) + ": selecciona un componente.").showAndWait();
                return;
            }
            if (l.getProveedor() == null) {
                new Alert(Alert.AlertType.WARNING, "Línea " + (i+1) + ": selecciona un proveedor.").showAndWait();
                return;
            }
            if (l.getCantidad() <= 0) {
                new Alert(Alert.AlertType.WARNING, "Línea " + (i+1) + ": la cantidad debe ser mayor que 0.").showAndWait();
                return;
            }
            if (l.getPrecioUnidad() < 0) {
                new Alert(Alert.AlertType.WARNING, "Línea " + (i+1) + ": el precio no puede ser negativo.").showAndWait();
                return;
            }
        }
        try {
            for (LineaPedido linea : lineas) {
                Proveedor prov  = linea.getProveedor();
                String    divisa = prov.getDivisa();
                double    tasa   = tipoCambioDAO.getTasa(divisa);
                double    precioEur = linea.getPrecioUnidad() * tasa;
                compraDAO.insertar(linea.getComponente().getIdCom(), prov.getIdProv(),
                        linea.getCantidad(), linea.isEsUrgente(),
                        linea.getPrecioUnidad(), divisa, precioEur);
            }
            onGuardado.run();
            cerrarVentana();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    @FXML private void cancelar() { cerrarVentana(); }

    private void cerrarVentana() {
        ((Stage) tablaLineas.getScene().getWindow()).close();
    }

    // ─── Apertura estática ────────────────────────────────────────────────────

    public static void abrir(Componente preselect, Runnable onGuardado) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        FormularioCompraController.class.getResource("/views/FormularioCompraView.fxml"));
                Parent root = loader.load();
                FormularioCompraController ctrl = loader.getController();
                Stage stage = new Stage();
                stage.setTitle("Nuevo pedido");
                stage.setScene(new Scene(root));
                stage.setResizable(false);
                stage.initModality(Modality.APPLICATION_MODAL);
                ctrl.init(preselect, onGuardado);
                stage.show();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public static void abrirEditar(CompraComponente pedido, Runnable onGuardado) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        FormularioCompraController.class.getResource("/views/FormularioCompraEditar.fxml"));
                Parent root = loader.load();
                FormularioCompraEditarController ctrl = loader.getController();
                Stage stage = new Stage();
                stage.setTitle("Editar pedido #" + pedido.getIdCompra());
                stage.setScene(new Scene(root));
                stage.setResizable(false);
                stage.initModality(Modality.APPLICATION_MODAL);
                ctrl.init(pedido, onGuardado);
                stage.show();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}
