package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.models.Componente;
import com.reparaciones.utils.Colores;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class MainController {

    @FXML private StackPane contenedor;
    @FXML private Button    btnReparaciones;
    @FXML private Button    btnStock;
    @FXML private Button    btnUsuario;
    @FXML private Label     lblUsuario;
    @FXML private Label     lblAlertaStock;

    private ContextMenu menuUsuario;

    private List<Componente> alertasCriticas = List.of();
    private com.reparaciones.utils.Recargable controladorActivo;

    @FXML
    public void initialize() {
        lblUsuario.setText("Hola, " + Sesion.getUsuario().getNombreUsuario());
        inicializarMenuUsuario();
        mostrarReparaciones();
        if (Sesion.esAdmin()) {
            verificarStockAlertas();
        }
        // Recargar al recuperar el foco de la ventana principal
        contenedor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            newScene.windowProperty().addListener((obs2, oldWin, win) -> {
                if (win == null) return;
                win.focusedProperty().addListener((obs3, wasFocused, isFocused) -> {
                    if (isFocused && controladorActivo != null)
                        controladorActivo.recargar();
                });
            });
        });
    }

    private void verificarStockAlertas() {
        try {
            List<Componente> todos = new ComponenteDAO().getAllGestionados();
            alertasCriticas = todos.stream()
                    .filter(c -> c.getStock() <= c.getStockMinimo())
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        if (alertasCriticas.isEmpty()) return;

        lblAlertaStock.setVisible(true);
        lblAlertaStock.setManaged(true);
        lblAlertaStock.setOnMouseClicked(e -> mostrarDialogoAlerta());
        Platform.runLater(this::mostrarDialogoAlerta);
    }

    private void mostrarDialogoAlerta() {
        List<Componente> sinStock = alertasCriticas.stream().filter(c -> c.getStock() == 0).collect(Collectors.toList());
        List<Componente> bajoMin  = alertasCriticas.stream().filter(c -> c.getStock() > 0).collect(Collectors.toList());

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.initStyle(StageStyle.UNDECORATED);
        ventana.setResizable(false);

        Label lblIcono = new Label("⚠");
        lblIcono.setStyle("-fx-font-size: 20px; -fx-text-fill: " + Colores.AMARILLO + ";");

        Label lblTitulo = new Label("Alerta de stock");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + Colores.CREMA + ";");

        Label lblX = new Label("✕");
        lblX.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: " + Colores.AZUL_GRIS + ";");
        lblX.setOnMouseClicked(e -> ventana.close());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox cabecera = new HBox(8, lblIcono, lblTitulo, spacer, lblX);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.setPadding(new Insets(0, 0, 12, 0));

        Label lblResumen = new Label(alertasCriticas.size() + " componente(s) requieren atención");
        lblResumen.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Colores.AZUL_GRIS + ";");

        VBox cuerpo = new VBox(10, cabecera, lblResumen);

        if (!sinStock.isEmpty()) {
            Label lblSec = new Label("Sin stock (" + sinStock.size() + ")");
            lblSec.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Colores.ROJO_SIN_STOCK + ";");
            VBox filas = new VBox(4);
            for (Componente c : sinStock) {
                Label fila = new Label("• " + c.getTipo());
                fila.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Colores.CREMA + ";");
                filas.getChildren().add(fila);
            }
            ScrollPane scroll = new ScrollPane(filas);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(140);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            cuerpo.getChildren().addAll(lblSec, scroll);
        }

        if (!bajoMin.isEmpty()) {
            Label lblSec = new Label("Bajo mínimo (" + bajoMin.size() + ")");
            lblSec.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Colores.AMARILLO + ";");
            VBox filas = new VBox(4);
            for (Componente c : bajoMin) {
                Label fila = new Label("• " + c.getTipo() + "   (" + c.getStock() + " uds.)");
                fila.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Colores.CREMA + ";");
                filas.getChildren().add(fila);
            }
            ScrollPane scroll = new ScrollPane(filas);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(140);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            cuerpo.getChildren().addAll(lblSec, scroll);
        }

        Button btnCerrar = new Button("Entendido");
        btnCerrar.setMaxWidth(Double.MAX_VALUE);
        btnCerrar.setStyle(
                "-fx-background-color: " + Colores.AMARILLO + "; -fx-text-fill: " + Colores.AZUL_NOCHE + ";" +
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 4;" +
                "-fx-padding: 10; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> ventana.close());
        cuerpo.getChildren().add(btnCerrar);

        cuerpo.setPadding(new Insets(24));
        cuerpo.setPrefWidth(360);
        cuerpo.setStyle("-fx-background-color: " + Colores.AZUL_MEDIO + ";" +
                "-fx-border-color: " + Colores.AZUL_GRIS + "; -fx-border-width: 1;");

        final double[] drag = new double[2];
        cuerpo.setOnMousePressed(ev  -> { drag[0] = ev.getSceneX(); drag[1] = ev.getSceneY(); });
        cuerpo.setOnMouseDragged(ev  -> {
            ventana.setX(ev.getScreenX() - drag[0]);
            ventana.setY(ev.getScreenY() - drag[1]);
        });

        Scene scene = new Scene(cuerpo);
        scene.setFill(Color.web(Colores.AZUL_MEDIO));
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    @FXML
    private void mostrarReparaciones() {
        String vista = Sesion.esAdmin()
                ? "/views/ReparacionViewAdmin.fxml"
                : "/views/ReparacionViewTecnico.fxml";
        mostrarVista(vista, btnReparaciones, btnStock);
    }

    @FXML
    private void mostrarStock() {
        mostrarVista("/views/StockView.fxml", btnStock, btnReparaciones);
    }

    private void inicializarMenuUsuario() {
        MenuItem itemDescargar = new MenuItem("Descargar CSV");
        SeparatorMenuItem sep  = new SeparatorMenuItem();
        MenuItem itemCerrar    = new MenuItem("Cerrar Sesión");

        itemDescargar.setOnAction(e -> { /* TODO */ });
        itemCerrar.setOnAction(e -> cerrarSesion());

        menuUsuario = new ContextMenu();
        if (Sesion.esAdmin()) {
            MenuItem itemGestionar = new MenuItem("Gestionar técnicos");
            itemGestionar.setOnAction(e -> abrirGestionTecnicos());
            menuUsuario.getItems().addAll(itemGestionar, new SeparatorMenuItem());
        }
        menuUsuario.getItems().addAll(itemDescargar, sep, itemCerrar);
    }

    private void abrirGestionTecnicos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RegisterView.fxml"));
            Parent root = loader.load();
            Stage ventana = new Stage();
            ventana.initModality(Modality.APPLICATION_MODAL);
            ventana.initOwner(btnUsuario.getScene().getWindow());
            ventana.setTitle("Gestión de técnicos");
            ventana.setScene(new Scene(root));
            ventana.setResizable(false);
            ventana.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void mostrarMenuUsuario() {
        menuUsuario.show(btnUsuario,
                javafx.geometry.Side.BOTTOM,
                0, 4);
    }

    @FXML
    private void cerrarSesion() {
        try {
            if (controladorActivo != null) controladorActivo.detenerPolling();
            Sesion.cerrar();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnUsuario.getScene().getWindow();

            stage.setMaximized(false);
            stage.setResizable(false);
            stage.setMinWidth(0);
            stage.setMinHeight(0);
            stage.setScene(new Scene(root));
            stage.setTitle("Gestión de Reparaciones — Login");
            stage.sizeToScene();
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mostrarVista(String ruta, Button activo, Button inactivo) {
        btnReparaciones.setDisable(true);
        btnStock.setDisable(true);

        try {
            if (controladorActivo != null) controladorActivo.detenerPolling();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Node vista = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof com.reparaciones.utils.Recargable)
                controladorActivo = (com.reparaciones.utils.Recargable) ctrl;
            contenedor.getChildren().setAll(vista);
            setActivo(activo, inactivo);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            btnReparaciones.setDisable(false);
            btnStock.setDisable(false);
        }
    }

    private void setActivo(Button activo, Button inactivo) {
        activo.getStyleClass().remove("nav-btn");
        if (!activo.getStyleClass().contains("nav-btn-active"))
            activo.getStyleClass().add("nav-btn-active");
        inactivo.getStyleClass().remove("nav-btn-active");
        if (!inactivo.getStyleClass().contains("nav-btn"))
            inactivo.getStyleClass().add("nav-btn");
    }
}