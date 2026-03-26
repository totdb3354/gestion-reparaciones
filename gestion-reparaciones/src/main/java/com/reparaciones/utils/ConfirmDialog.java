package com.reparaciones.utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Diálogo de confirmación reutilizable con cuenta atrás de seguridad.
 * El botón de acción destructiva se habilita solo cuando el contador llega a 0,
 * evitando confirmaciones accidentales.
 *
 * Uso:
 *   ConfirmDialog.mostrar("Título", "Descripción", "Texto acción", "Texto cancelar", () -> accion());
 *   ConfirmDialog.mostrar("Título", "Descripción", "Texto acción", () -> accion()); // cancelar = "Cancelar"
 */
public class ConfirmDialog {

    private static final int SEGUNDOS = 1;

    public static void mostrar(String titulo, String descripcion,
                               String textoAccion, String textoCancel,
                               Runnable onConfirm) {

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.initStyle(StageStyle.UNDECORATED);
        ventana.setResizable(false);

        // ── Título + X ────────────────────────────────────────────────────────
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FB8888;");
        lblTitulo.setWrapText(true);

        Label lblX = new Label("X");
        lblX.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-text-fill: #333333;");
        lblX.setOnMouseClicked(e -> ventana.close());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox barraTop = new HBox(lblTitulo, spacer, lblX);
        barraTop.setAlignment(Pos.CENTER_LEFT);
        barraTop.setPadding(new Insets(0, 0, 8, 0));

        // ── Descripción ───────────────────────────────────────────────────────
        Label lblDesc = new Label(descripcion);
        lblDesc.setStyle("-fx-font-size: 16px; -fx-text-fill: #000000;");
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(352);

        // ── Botón acción con cuenta atrás ─────────────────────────────────────
        final int[] segs = {SEGUNDOS};
        Button btnAccion = new Button(textoAccion + " ( " + segs[0] + " segs)");
        btnAccion.setMaxWidth(Double.MAX_VALUE);
        btnAccion.setDisable(true);
        btnAccion.setStyle(
                "-fx-background-color: rgba(217,217,217,0.64); -fx-text-fill: #A9A9A9;" +
                "-fx-background-radius: 4; -fx-font-size: 12px; -fx-padding: 10;");

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segs[0]--;
            if (segs[0] > 0) {
                btnAccion.setText(textoAccion + " ( " + segs[0] + " segs)");
            } else {
                btnAccion.setText(textoAccion);
                btnAccion.setDisable(false);
                btnAccion.setStyle(
                        "-fx-background-color: #E7E7E7; -fx-text-fill: #555555;" +
                        "-fx-background-radius: 4; -fx-font-size: 12px;" +
                        "-fx-padding: 10; -fx-cursor: hand;");
            }
        }));
        countdown.setCycleCount(SEGUNDOS);
        countdown.play();

        btnAccion.setOnAction(e -> {
            countdown.stop();
            ventana.close();
            onConfirm.run();
        });

        // ── Botón cancelar ────────────────────────────────────────────────────
        Button btnCancelar = new Button(textoCancel);
        btnCancelar.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.setStyle(
                "-fx-background-color: #A9A9A9; -fx-text-fill: white;" +
                "-fx-background-radius: 4; -fx-font-size: 12px;" +
                "-fx-padding: 10; -fx-cursor: hand;");
        btnCancelar.setOnAction(e -> {
            countdown.stop();
            ventana.close();
        });

        // ── Layout ────────────────────────────────────────────────────────────
        VBox contenido = new VBox(10, barraTop, lblDesc, btnAccion, btnCancelar);
        contenido.setPadding(new Insets(24));
        contenido.setPrefWidth(400);
        contenido.setStyle(
                "-fx-background-color: #F3F3F3;" +
                "-fx-border-color: #CCCCCC; -fx-border-width: 1;");

        // ── Arrastrar ventana ─────────────────────────────────────────────────
        final double[] drag = new double[2];
        contenido.setOnMousePressed(e  -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        contenido.setOnMouseDragged(e  -> {
            ventana.setX(e.getScreenX() - drag[0]);
            ventana.setY(e.getScreenY() - drag[1]);
        });

        Scene scene = new Scene(contenido);
        scene.setFill(Color.web("#F3F3F3"));
        ventana.setScene(scene);
        ventana.setOnCloseRequest(e -> countdown.stop());
        ventana.showAndWait();
    }

    // Sobrecarga con texto de cancelar por defecto
    public static void mostrar(String titulo, String descripcion,
                               String textoAccion, Runnable onConfirm) {
        mostrar(titulo, descripcion, textoAccion, "Cancelar", onConfirm);
    }

    // ── Popup de lectura (read-only, seleccionable, copiable) ─────────────────
    public static void mostrarTexto(String titulo, String texto) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.initStyle(StageStyle.UNDECORATED);
        ventana.setResizable(false);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #555555;");
        lblTitulo.setWrapText(true);

        Label lblX = new Label("X");
        lblX.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-text-fill: #333333;");
        lblX.setOnMouseClicked(e -> ventana.close());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox barraTop = new HBox(lblTitulo, spacer, lblX);
        barraTop.setAlignment(Pos.CENTER_LEFT);
        barraTop.setPadding(new Insets(0, 0, 8, 0));

        TextArea ta = new TextArea(texto != null ? texto : "");
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefWidth(380);
        ta.setPrefRowCount(6);
        ta.setStyle("-fx-font-size: 13px; -fx-background-color: white;" +
                "-fx-border-color: #CCCCCC; -fx-border-width: 1;");

        Button btnCopiar = new Button("Copiar");
        btnCopiar.setMaxWidth(Double.MAX_VALUE);
        btnCopiar.setStyle("-fx-background-color: #A9A9A9; -fx-text-fill: white;" +
                "-fx-font-size: 12px; -fx-padding: 8; -fx-cursor: hand;");
        btnCopiar.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(texto != null ? texto : "");
            Clipboard.getSystemClipboard().setContent(cc);
            ventana.close();
        });

        VBox contenido = new VBox(10, barraTop, ta, btnCopiar);
        contenido.setPadding(new Insets(20));
        contenido.setPrefWidth(420);
        contenido.setStyle("-fx-background-color: #F3F3F3;" +
                "-fx-border-color: #CCCCCC; -fx-border-width: 1;");

        final double[] drag = new double[2];
        contenido.setOnMousePressed(e  -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        contenido.setOnMouseDragged(e  -> {
            ventana.setX(e.getScreenX() - drag[0]);
            ventana.setY(e.getScreenY() - drag[1]);
        });

        Scene scene = new Scene(contenido);
        scene.setFill(Color.web("#F3F3F3"));
        ventana.setScene(scene);
        ventana.showAndWait();
    }
}
