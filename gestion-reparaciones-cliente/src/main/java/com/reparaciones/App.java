package com.reparaciones;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación JavaFX.
 * <p>Carga {@code LoginView.fxml} como vista inicial y configura la ventana como
 * no redimensionable hasta que el login tenga éxito.</p>
 */
public class App extends Application {

    /**
     * Punto de entrada de JavaFX: carga la pantalla de login y muestra la ventana.
     *
     * @param stage ventana principal proporcionada por el runtime de JavaFX
     * @throws Exception si no se puede cargar el FXML
     */
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/LoginView.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("Gestión de Reparaciones — Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /** Lanza la aplicación JavaFX. */
    public static void main(String[] args) {
        launch(args);
    }
}
