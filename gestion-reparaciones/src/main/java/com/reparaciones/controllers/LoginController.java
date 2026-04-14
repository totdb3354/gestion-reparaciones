package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.UsuarioDAO;
import com.reparaciones.models.Usuario;
import com.reparaciones.utils.Colores;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField campoUsuario;
    @FXML
    private PasswordField campoPassword;
    @FXML
    private Label lblError;
    @FXML
    private Button btnLogin;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) campoUsuario.getScene().getWindow();
            stage.setResizable(false);
            stage.sizeToScene();
        });

        campoUsuario.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER)
                login();
        });
        campoPassword.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER)
                login();
        });
    }

    /**
     * Autentica con las credenciales introducidas.
     * Si OK → guarda en Sesion y carga MainView.
     * Si falla → muestra mensaje de error en rojo.
     */
    @FXML
    private void login() {
        String usuario = campoUsuario.getText().trim();
        String password = campoPassword.getText();

        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarError("Rellena usuario y contraseña.");
            return;
        }

        try {
            Usuario u = usuarioDAO.login(usuario, password);
            if (u == null) {
                mostrarError("Usuario o contraseña incorrectos.");
                return;
            }
            Sesion.iniciar(u);
            cargarMainView();
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error de conexión con la base de datos.");
        }
    }

    /**
     * Carga MainView en la misma ventana tras login exitoso.
     * Establece tamaño mínimo y permite redimensionar.
     */
    private void cargarMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/MainView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setResizable(true);
            stage.setScene(new Scene(root));
            stage.setTitle("Gestión de Reparaciones");
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            boolean esMac = System.getProperty("os.name").toLowerCase().contains("mac");
            if (esMac) {
                javafx.application.Platform.runLater(() -> {
                    javafx.geometry.Rectangle2D pantalla =
                            javafx.stage.Screen.getPrimary().getVisualBounds();
                    stage.setX(pantalla.getMinX());
                    stage.setY(pantalla.getMinY());
                    stage.setWidth(pantalla.getWidth());
                    stage.setHeight(pantalla.getHeight());
                });
            } else {
                javafx.application.Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }

    /**
     * Pide la contraseña maestra (del admin) antes de navegar al registro.
     * Si es correcta → carga RegisterView.
     */
    @FXML
    private void irARegistro() {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.initStyle(StageStyle.UNDECORATED);
        ventana.setResizable(false);

        Label lblTitulo = new Label("Acceso restringido");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + Colores.CREMA + ";");

        Label lblX = new Label("✕");
        lblX.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: " + Colores.AZUL_GRIS + ";");
        lblX.setOnMouseClicked(e -> ventana.close());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox barraTop = new HBox(lblTitulo, spacer, lblX);
        barraTop.setAlignment(Pos.CENTER_LEFT);
        barraTop.setPadding(new Insets(0, 0, 12, 0));

        Label lblSub = new Label("Introduce la contraseña maestra");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Colores.AZUL_GRIS + ";");

        PasswordField pf = new PasswordField();
        pf.setPromptText("Contraseña");
        pf.setStyle("-fx-background-color: " + Colores.AZUL_NOCHE + "; -fx-border-color: " + Colores.AZUL_GRIS + ";" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;" +
                "-fx-text-fill: " + Colores.CREMA + "; -fx-prompt-text-fill: " + Colores.AZUL_GRIS + ";");

        Label lblError2 = new Label();
        lblError2.setStyle("-fx-font-size: 11px; -fx-text-fill: " + Colores.TEXTO_ERROR + ";");
        lblError2.setVisible(false);

        Button btnAcceder = new Button("Acceder");
        btnAcceder.setMaxWidth(Double.MAX_VALUE);
        btnAcceder.setStyle("-fx-background-color: " + Colores.AMARILLO + "; -fx-text-fill: " + Colores.AZUL_NOCHE + ";" +
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 10; -fx-cursor: hand;");

        Runnable intentarAcceso = () -> {
            String password = pf.getText();
            try {
                if (usuarioDAO.verificarPasswordAdmin(password)) {
                    ventana.close();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RegisterView.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) btnLogin.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Gestión de Reparaciones — Registro");
                    stage.centerOnScreen();
                } else {
                    lblError2.setText("Contraseña incorrecta.");
                    lblError2.setVisible(true);
                    pf.clear();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        btnAcceder.setOnAction(e -> intentarAcceso.run());
        pf.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ENTER) intentarAcceso.run(); });

        VBox contenido = new VBox(10, barraTop, lblSub, pf, lblError2, btnAcceder);
        contenido.setPadding(new Insets(24));
        contenido.setPrefWidth(320);
        contenido.setStyle("-fx-background-color: " + Colores.AZUL_MEDIO + ";" +
                "-fx-border-color: " + Colores.AZUL_GRIS + "; -fx-border-width: 1;");

        final double[] drag = new double[2];
        contenido.setOnMousePressed(e  -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        contenido.setOnMouseDragged(e  -> {
            ventana.setX(e.getScreenX() - drag[0]);
            ventana.setY(e.getScreenY() - drag[1]);
        });

        Scene scene = new Scene(contenido);
        scene.setFill(Color.web(Colores.AZUL_MEDIO));
        ventana.setScene(scene);
        Platform.runLater(pf::requestFocus);
        ventana.showAndWait();
    }
}