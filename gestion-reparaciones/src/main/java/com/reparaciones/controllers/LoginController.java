package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.UsuarioDAO;
import com.reparaciones.models.Usuario;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField       campoUsuario;
    @FXML private PasswordField   campoPassword;
    @FXML private TextField       campoPasswordVisible;
    @FXML private Button          btnTogglePassword;
    @FXML private javafx.scene.image.ImageView imgOjo;
    @FXML private Label           lblError;
    @FXML private Button          btnLogin;

    private boolean passwordVisible = false;

    private final javafx.scene.image.Image IMG_OJO_ACTIVAR   =
            new javafx.scene.image.Image(getClass().getResourceAsStream("/images/ojo_activar.png"));
    private final javafx.scene.image.Image IMG_OJO_DESACTIVAR =
            new javafx.scene.image.Image(getClass().getResourceAsStream("/images/ojo_desactivar.png"));

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) campoUsuario.getScene().getWindow();
            stage.setResizable(false);
            stage.sizeToScene();
        });

        campoUsuario.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) login();
        });
        campoPassword.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) login();
        });
        campoPasswordVisible.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) login();
        });
    }

    @FXML
    private void togglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            campoPasswordVisible.setText(campoPassword.getText());
            campoPasswordVisible.setVisible(true);  campoPasswordVisible.setManaged(true);
            campoPassword.setVisible(false);        campoPassword.setManaged(false);
            imgOjo.setImage(IMG_OJO_DESACTIVAR);
        } else {
            campoPassword.setText(campoPasswordVisible.getText());
            campoPassword.setVisible(true);          campoPassword.setManaged(true);
            campoPasswordVisible.setVisible(false);  campoPasswordVisible.setManaged(false);
            imgOjo.setImage(IMG_OJO_ACTIVAR);
        }
    }

    /**
     * Autentica con las credenciales introducidas.
     * Si OK → guarda en Sesion y carga MainView.
     * Si falla → muestra mensaje de error en rojo.
     */
    @FXML
    private void login() {
        String usuario  = campoUsuario.getText().trim();
        String password = passwordVisible ? campoPasswordVisible.getText() : campoPassword.getText();

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

}