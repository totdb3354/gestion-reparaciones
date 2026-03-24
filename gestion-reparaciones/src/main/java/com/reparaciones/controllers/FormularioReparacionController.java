package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.dao.ReparacionDAO;
import com.reparaciones.models.Componente;
import com.reparaciones.models.FilaReparacion;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Pos;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FormularioReparacionController {

    @FXML
    private Label lblImei;
    @FXML
    private Label lblIncidencia;
    @FXML
    private VBox contenedorFilas;
    @FXML
    private Button btnGuardar;
    @FXML
    private ComboBox<String> cbFiltroModelo;

    private final ReparacionDAO reparacionDAO = new ReparacionDAO();
    private final ComponenteDAO componenteDAO = new ComponenteDAO();

    private long imei;
    private String idRepAnterior;
    private String idAsignacion;
    private Runnable onGuardado;

    private final List<FilaUI> filasUI = new ArrayList<>();

    /**
     * Lista de modelos en orden de tienda Apple.
     * Para añadir iPhone 17 en el futuro:
     * agregar "17", "17plus", "17pro", "17promax" al final
     * No hace falta tocar nada más — traducirModelo lo traduce automáticamente.
     */
    private static final List<String> MODELOS_ORDENADOS = List.of(
            "6s", "6splus", "7", "7plus", "8", "8plus", "se2020",
            "x", "xr", "xs", "xsmax",
            "11", "11pro", "11promax",
            "12", "12mini", "12pro", "12promax",
            "13", "13mini", "13pro", "13promax",
            "14", "14plus", "14pro", "14promax",
            "15", "15plus", "15pro", "15promax",
            "16", "16e", "16plus", "16pro", "16promax"
    // Ejemplo futuro: "17", "17plus", "17pro", "17promax"
    );

    public void init(long imei, String idRepAnterior, String idAsignacion, Runnable onGuardado) {
        this.imei = imei;
        this.idAsignacion = idAsignacion;
        this.onGuardado = onGuardado;

        this.idRepAnterior = idRepAnterior;
        if (this.idRepAnterior == null) {
            try {
                this.idRepAnterior = reparacionDAO.getIncidenciaActivaPorImei(imei);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        lblImei.setText("IMEI: " + imei);
        if (this.idRepAnterior != null) {
            lblIncidencia.setText("⚠ Resuelve incidencia: " + this.idRepAnterior);
            lblIncidencia.setVisible(true);
            lblIncidencia.setManaged(true);
        }

        cargarFilas();
        configurarFiltroModelo();
    }

    private void cargarFilas() {
        try {
            Map<String, List<Componente>> grupos = componenteDAO.getAgrupadosPorTipo();
            Image imgBorrar = new Image(getClass().getResourceAsStream("/images/borrar.png"));
            for (Map.Entry<String, List<Componente>> entry : grupos.entrySet()) {
                if (entry.getValue().isEmpty())
                    continue;
                FilaUI fila = new FilaUI(entry.getKey(), entry.getValue(), imgBorrar);
                fila.setOnCambio(this::actualizarBoton);
                contenedorFilas.getChildren().add(fila.getRoot());
                filasUI.add(fila);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void configurarFiltroModelo() {
        Set<String> modelosEnBD = filasUI.stream()
                .flatMap(f -> f.getSkus().stream()
                        .map(c -> extraerModelo(c.getTipo(), f.getPrefijo())))
                .filter(m -> !m.isEmpty())
                .collect(Collectors.toSet());

        List<String> modelosFiltrados = MODELOS_ORDENADOS.stream()
                .filter(modelosEnBD::contains)
                .collect(Collectors.toList());

        cbFiltroModelo.getItems().add("Todos");
        cbFiltroModelo.getItems().addAll(modelosFiltrados);
        cbFiltroModelo.setValue("Todos");

        cbFiltroModelo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setText(null);
                    return;
                }
                setText(m.equals("Todos") ? "Todos los modelos" : traducirModelo(m));
            }
        });
        cbFiltroModelo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setText(null);
                    return;
                }
                setText(m.equals("Todos") ? "Todos los modelos" : traducirModelo(m));
            }
        });

        cbFiltroModelo.valueProperty().addListener((obs, o, n) -> {
            for (FilaUI fila : filasUI) {
                fila.aplicarFiltroModelo(n);
            }
        });
    }

    private void actualizarBoton() {
        boolean activa = filasUI.stream().anyMatch(FilaUI::isActiva);
        btnGuardar.setDisable(!activa);
        btnGuardar.setStyle(activa
                ? "-fx-background-color: #8AC7AF; -fx-text-fill: white; -fx-font-size: 12px;" +
                        "-fx-font-weight: bold; -fx-background-radius: 0; -fx-padding: 10; -fx-cursor: hand;"
                : "-fx-background-color: #E7E7E7; -fx-text-fill: #A9A9A9; -fx-font-size: 12px;" +
                        "-fx-font-weight: bold; -fx-background-radius: 0; -fx-padding: 10;");
    }

    @FXML
    private void guardar() {
        List<FilaReparacion> filasActivas = new ArrayList<>();
        for (FilaUI fila : filasUI) {
            if (fila.isActiva()) {
                filasActivas.add(new FilaReparacion(
                        fila.getIdComSeleccionado(),
                        fila.getCantidad(),
                        fila.isReutilizado(),
                        fila.getObservacion(),
                        fila.getPrefijo()));
            }
        }
        try {
            reparacionDAO.insertarCompleta(filasActivas, imei,
                    Sesion.getIdTec(), idRepAnterior, idAsignacion);
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
            if (onGuardado != null)
                onGuardado.run();
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo guardar: " + ex.getMessage()).showAndWait();
        }
    }

    public static void abrir(long imei, String idRepAnterior,
            String idAsignacion, Runnable onGuardado) {
        Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        FormularioReparacionController.class.getResource(
                                "/views/FormularioReparacionView.fxml"));
                javafx.scene.Parent root = loader.load();
                FormularioReparacionController ctrl = loader.getController();

                Stage stage = new Stage();
                stage.setTitle("Nueva reparación — IMEI " + imei);
                stage.setScene(new javafx.scene.Scene(root));
                stage.setResizable(true);
                stage.setMinWidth(900);
                stage.setMinHeight(400);

                ctrl.init(imei, idRepAnterior, idAsignacion, onGuardado);

                stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ─── Utilidades estáticas ─────────────────────────────────────────────────

    /**
     * Extrae el modelo del SKU buscando qué modelo conocido aparece
     * al inicio del SKU tras quitar el prefijo y la 'i' de iPhone.
     * Compatible con cualquier sufijo nuevo sin tocar el código.
     */
    static String extraerModelo(String sku, String prefijo) {
        String s = sku.toLowerCase().substring(prefijo.length());
        if (s.startsWith("i"))
            s = s.substring(1);
        final String resto = s;
        return MODELOS_ORDENADOS.stream()
                .sorted((a, b) -> b.length() - a.length())
                .filter(resto::startsWith)
                .findFirst()
                .orElse("");
    }

    /**
     * Traduce el modelo interno del SKU al nombre comercial legible.
     *
     * Modelos numéricos (11, 12, 13, 14, 15, 16...):
     * Se traducen automáticamente — no hace falta añadir casos nuevos.
     * Ejemplo futuro: "17" → "iPhone 17" (automático)
     * "17plus" → "iPhone 17 Plus" (automático)
     * "17pro" → "iPhone 17 Pro" (automático)
     * "17promax" → "iPhone 17 Pro Max" (automático)
     *
     * Modelos especiales sin número (x, xs, xsmax, xr, se2020):
     * Hardcodeados porque son históricos y no van a cambiar.
     */
    static String traducirModelo(String modelo) {
        if (modelo == null || modelo.isEmpty())
            return modelo;
        return switch (modelo) {
            case "se2020" -> "iPhone SE 2020";
            case "x" -> "iPhone X";
            case "xr" -> "iPhone XR";
            case "xs" -> "iPhone XS";
            case "xsmax" -> "iPhone XS Max";
            // Casos especiales con letra sin número claro
            case "6s" -> "iPhone 6S";
            case "6splus" -> "iPhone 6S Plus";
            default -> {
                String num = modelo.replaceAll("[^0-9]", "");
                String variante = modelo.replaceAll("[0-9]", "");
                String sufijo = switch (variante) {
                    case "plus" -> " Plus";
                    case "mini" -> " Mini";
                    case "pro" -> " Pro";
                    case "promax" -> " Pro Max";
                    case "e" -> "e";
                    default -> "";
                };
                yield "iPhone " + num + sufijo;
            }
        };
    }

    // ─── FilaUI ───────────────────────────────────────────────────────────────

    static class FilaUI {

        private final String prefijo;
        private final List<Componente> skus;
        private final HBox root;

        private final Label lblContador;
        private final Button btnMas;
        private final Button btnMenos;
        private final ComboBox<Componente> cbSku;
        private final Label lblStock;
        private final CheckBox chkReutilizado;
        private final Button btnObservacion;
        private final Label lblObservacion;
        private final Button btnBorrarObs;

        private int cantidad = 0;
        private String observacion = null;
        private Runnable onCambio;

        FilaUI(String prefijo, List<Componente> skus, Image imgBorrar) {
            this.prefijo = prefijo;
            this.skus = skus;

            lblContador = new Label("0");
            lblContador.setMinWidth(34);
            lblContador.setPrefWidth(34);
            lblContador.setAlignment(Pos.CENTER);
            lblContador.setFont(Font.font("Inter", FontWeight.BOLD, 20));
            lblContador.setStyle("-fx-text-fill: #A9A9A9;");

            btnMas = botonContador("+");
            btnMenos = botonContador("-");
            btnMenos.setDisable(true);

            VBox botonera = new VBox(btnMas, btnMenos);
            botonera.setMinWidth(35);
            botonera.setMaxWidth(35);

            HBox wrapContador = new HBox(lblContador, botonera);
            wrapContador.setMinWidth(70);
            wrapContador.setMaxWidth(70);
            wrapContador.setAlignment(Pos.CENTER_LEFT);

            Label lblNombre = new Label(traducirTipo(prefijo));
            lblNombre.setMinWidth(100);
            lblNombre.setPrefWidth(100);
            lblNombre.setMaxWidth(100);
            lblNombre.setStyle("-fx-font-size: 12px; -fx-padding: 0 10 0 10;");

            chkReutilizado = new CheckBox("Reutilizado");
            chkReutilizado.setMinWidth(110);
            chkReutilizado.setPrefWidth(110);
            chkReutilizado.setStyle("-fx-font-size: 12px; -fx-padding: 0 10 0 10;");

            cbSku = new ComboBox<>();
            cbSku.getItems().addAll(skus);
            cbSku.setMinWidth(170);
            cbSku.setPrefWidth(170);
            cbSku.setMaxWidth(170);
            cbSku.setStyle("-fx-font-size: 11px;");

            cbSku.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Componente c, boolean empty) {
                    super.updateItem(c, empty);
                    if (empty || c == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    setText(c.getTipo());
                    aplicarColorStock(this, c);
                }
            });
            cbSku.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Componente c, boolean empty) {
                    super.updateItem(c, empty);
                    if (empty || c == null) {
                        setText("—");
                        setStyle("");
                        return;
                    }
                    setText(c.getTipo());
                    aplicarColorStock(this, c);
                }
            });

            Componente porDefecto = skus.stream()
                    .filter(c -> c.getStock() > 0)
                    .findFirst().orElse(skus.get(0));
            cbSku.setValue(porDefecto);

            lblStock = new Label(prefijo.equals("otro") ? "—" : String.valueOf(porDefecto.getStock()));
            lblStock.setMinWidth(70);
            lblStock.setPrefWidth(70);
            lblStock.setMaxWidth(70);
            lblStock.setAlignment(Pos.CENTER);
            lblStock.setStyle("-fx-font-size: 12px; -fx-padding: 0 10 0 10;");

            btnMas.setDisable(!prefijo.equals("otro") && porDefecto.getStock() <= 0);

            cbSku.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    lblStock.setText(prefijo.equals("otro") ? "—" : String.valueOf(n.getStock()));
                    if (!prefijo.equals("otro")) {
                        if (cantidad > n.getStock()) {
                            cantidad = 0;
                            actualizarContador();
                            if (!chkReutilizado.isSelected())
                                chkReutilizado.setDisable(false);
                            notificar();
                        }
                        if (!chkReutilizado.isSelected()) {
                            btnMas.setDisable(cantidad >= n.getStock());
                        }
                    }
                }
            });

            btnObservacion = new Button("✎  Añadir observación");
            btnObservacion.setStyle(
                    "-fx-background-color: #A9A9A9; -fx-text-fill: #E7E7E7;" +
                            "-fx-font-size: 11px; -fx-cursor: hand;" +
                            "-fx-background-radius: 0; -fx-padding: 4 10 4 10;");
            btnObservacion.setMinHeight(27);
            btnObservacion.setMaxHeight(27);

            lblObservacion = new Label();
            lblObservacion.setStyle("-fx-font-size: 12px; -fx-text-fill: #000000;");
            lblObservacion.setMaxWidth(Double.MAX_VALUE);
            lblObservacion.setTextOverrun(OverrunStyle.ELLIPSIS);
            lblObservacion.setVisible(false);
            lblObservacion.setManaged(false);

            ImageView ivBorrarObs = new ImageView(imgBorrar);
            ivBorrarObs.setFitWidth(12);
            ivBorrarObs.setFitHeight(12);
            ivBorrarObs.setPreserveRatio(true);
            btnBorrarObs = new Button();
            btnBorrarObs.setGraphic(ivBorrarObs);
            btnBorrarObs.setStyle(
                    "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4 2 4;");
            btnBorrarObs.setVisible(false);
            btnBorrarObs.setManaged(false);

            HBox wrapObs = new HBox(4, btnObservacion, lblObservacion, btnBorrarObs);
            wrapObs.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(wrapObs, Priority.ALWAYS);
            HBox.setHgrow(lblObservacion, Priority.ALWAYS);

            root = new HBox(wrapContador, lblNombre, cbSku, lblStock, chkReutilizado, wrapObs);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setMinHeight(37);
            root.setMaxHeight(37);
            root.setStyle("-fx-background-color: #F3F3F3; " +
                    "-fx-border-color: transparent transparent #E0E0E0 transparent;" +
                    "-fx-border-width: 0 0 1 0;");

            btnMas.setOnAction(e -> {
                if (prefijo.equals("otro")) {
                    cantidad++;
                    actualizarContador();
                    chkReutilizado.setDisable(true);
                    notificar();
                    return;
                }
                int stockActual = cbSku.getValue() != null ? cbSku.getValue().getStock() : 0;
                if (cantidad < stockActual) {
                    cantidad++;
                    actualizarContador();
                    btnMas.setDisable(cantidad >= stockActual);
                    chkReutilizado.setDisable(true);
                    notificar();
                }
            });

            btnMenos.setOnAction(e -> {
                if (cantidad > 0) {
                    cantidad--;
                    actualizarContador();
                    if (!prefijo.equals("otro")) {
                        int stockActual = cbSku.getValue() != null ? cbSku.getValue().getStock() : 0;
                        btnMas.setDisable(cantidad >= stockActual);
                    }
                    if (cantidad == 0)
                        chkReutilizado.setDisable(false);
                    notificar();
                }
            });

            chkReutilizado.setOnAction(e -> {
                boolean marcado = chkReutilizado.isSelected();
                btnMas.setDisable(marcado);
                btnMenos.setDisable(marcado);
                notificar();
            });

            btnObservacion.setOnAction(e -> abrirObservacion());

            btnBorrarObs.setOnAction(e -> {
                observacion = null;
                lblObservacion.setText("");
                btnObservacion.setVisible(true);
                btnObservacion.setManaged(true);
                lblObservacion.setVisible(false);
                lblObservacion.setManaged(false);
                btnBorrarObs.setVisible(false);
                btnBorrarObs.setManaged(false);
            });
        }

        // ── Filtro global de modelo ───────────────────────────────────────────

        void aplicarFiltroModelo(String modelo) {
            cantidad = 0;
            actualizarContador();
            chkReutilizado.setSelected(false);
            chkReutilizado.setDisable(false);

            if (modelo == null || modelo.equals("Todos")) {
                cbSku.getItems().setAll(skus);
                Componente def = skus.stream()
                        .filter(c -> c.getStock() > 0)
                        .findFirst().orElse(skus.get(0));
                cbSku.setValue(def);
                cbSku.setDisable(false);
                btnMas.setDisable(!prefijo.equals("otro") && def.getStock() <= 0);
                btnMenos.setDisable(true);
                chkReutilizado.setDisable(false);
                btnObservacion.setDisable(false);
                root.setOpacity(1.0);
                return;
            }

            List<Componente> filtrados = skus.stream()
                    .filter(c -> extraerModelo(c.getTipo(), prefijo).equals(modelo))
                    .collect(Collectors.toList());

            if (filtrados.isEmpty()) {
                cbSku.getItems().clear();
                cbSku.setValue(null);
                cbSku.setDisable(true);
                btnMas.setDisable(true);
                btnMenos.setDisable(true);
                chkReutilizado.setDisable(true);
                // Deshabilitar observación y limpiarla — la fila no se guarda
                btnObservacion.setDisable(true);
                observacion = null;
                lblObservacion.setText("");
                btnObservacion.setVisible(true);
                btnObservacion.setManaged(true);
                lblObservacion.setVisible(false);
                lblObservacion.setManaged(false);
                btnBorrarObs.setVisible(false);
                btnBorrarObs.setManaged(false);
                root.setOpacity(0.4);
            } else {
                cbSku.getItems().setAll(filtrados);
                Componente def = filtrados.stream()
                        .filter(c -> c.getStock() > 0)
                        .findFirst().orElse(filtrados.get(0));
                cbSku.setValue(def);
                cbSku.setDisable(false);
                btnMas.setDisable(!prefijo.equals("otro") && def.getStock() <= 0);
                btnMenos.setDisable(true);
                chkReutilizado.setDisable(false);
                btnObservacion.setDisable(false);
                root.setOpacity(1.0);
            }
        }

        // ── Color stock ───────────────────────────────────────────────────────

        private static void aplicarColorStock(ListCell<Componente> cell, Componente c) {
            if (c.getStock() == 0) {
                cell.setStyle("-fx-text-fill: #FB8888;");
            } else if (c.getStock() <= c.getStockMinimo()) {
                cell.setStyle("-fx-text-fill: #FFA500;");
            } else {
                cell.setStyle("");
            }
        }

        // ── Observación ───────────────────────────────────────────────────────

        private void abrirObservacion() {
            TextArea ta = new TextArea(observacion != null ? observacion : "");
            ta.setWrapText(true);
            ta.setPrefRowCount(5);
            ta.setPrefWidth(400);
            ta.setStyle("-fx-font-size: 13px;");

            Button btnGuardar = new Button("Guardar");
            btnGuardar.setMaxWidth(Double.MAX_VALUE);
            btnGuardar.setStyle("-fx-background-color: #8AC7AF; -fx-text-fill: white;" +
                    "-fx-font-size: 12px; -fx-padding: 8; -fx-cursor: hand;");

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Observación");
            dialog.setHeaderText("Observación para: " + traducirTipo(prefijo));
            dialog.getDialogPane().setContent(new VBox(8, ta, btnGuardar));
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.getDialogPane().setPrefWidth(440);

            btnGuardar.setOnAction(e -> {
                String trimmed = ta.getText().trim();
                if (!trimmed.isEmpty()) {
                    observacion = trimmed;
                    lblObservacion.setText(observacion);
                    btnObservacion.setVisible(false);
                    btnObservacion.setManaged(false);
                    lblObservacion.setVisible(true);
                    lblObservacion.setManaged(true);
                    btnBorrarObs.setVisible(true);
                    btnBorrarObs.setManaged(true);
                }
                dialog.close();
            });

            dialog.showAndWait();
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private static Button botonContador(String texto) {
            Button btn = new Button(texto);
            btn.setMinWidth(35);
            btn.setMaxWidth(35);
            btn.setMinHeight(18);
            btn.setMaxHeight(18);
            btn.setStyle("-fx-background-color: #A9A9A9; -fx-text-fill: #E7E7E7;" +
                    "-fx-font-size: 14px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;");
            return btn;
        }

        private void actualizarContador() {
            lblContador.setText(String.valueOf(cantidad));
            btnMenos.setDisable(cantidad == 0);
            lblContador.setStyle("-fx-text-fill: " + (cantidad > 0 ? "#000000" : "#A9A9A9") + ";");
        }

        private void notificar() {
            if (onCambio != null)
                onCambio.run();
        }

        static String traducirTipo(String prefijo) {
            return switch (prefijo) {
                case "bat" -> "Batería";
                case "cha" -> "Chasis";
                case "g" -> "Glass";
                case "cam" -> "Cámara";
                case "lcd" -> "Pantalla";
                case "mc" -> "Marco";
                case "otro" -> "Otros";
                default -> prefijo;
            };
        }

        // ── Estado público ────────────────────────────────────────────────────

        boolean isActiva() {
            return cantidad > 0 || chkReutilizado.isSelected();
        }

        int getIdComSeleccionado() {
            return cbSku.getValue() != null ? cbSku.getValue().getIdCom() : -1;
        }

        int getCantidad() {
            return cantidad;
        }

        boolean isReutilizado() {
            return chkReutilizado.isSelected();
        }

        String getObservacion() {
            return observacion;
        }

        String getPrefijo() {
            return prefijo;
        }

        List<Componente> getSkus() {
            return skus;
        }

        HBox getRoot() {
            return root;
        }

        void setOnCambio(Runnable r) {
            this.onCambio = r;
        }
    }
}