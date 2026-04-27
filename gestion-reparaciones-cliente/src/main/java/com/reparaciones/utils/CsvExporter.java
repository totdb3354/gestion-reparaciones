package com.reparaciones.utils;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Utilidad genérica para exportar datos a CSV.
 * Abre un FileChooser y escribe el archivo con separador punto y coma
 * para compatibilidad con Excel en español.
 */
public class CsvExporter {

    private CsvExporter() {}

    /**
     * Muestra FileChooser y escribe el CSV.
     *
     * @param owner      ventana padre para el diálogo
     * @param nombreBase nombre sugerido del archivo (sin extensión)
     * @param cabeceras  lista de nombres de columna
     * @param filas      lista de filas, cada fila es una lista de valores
     * @return true si se guardó correctamente, false si el usuario canceló o hubo error
     */
    public static boolean exportar(Stage owner, String nombreBase,
                                   List<String> cabeceras, List<List<String>> filas) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar CSV");
        chooser.setInitialFileName(nombreBase + ".csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo CSV (*.csv)", "*.csv"));

        File archivo = chooser.showSaveDialog(owner);
        if (archivo == null) return false;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo, java.nio.charset.StandardCharsets.UTF_8))) {
            // BOM para que Excel reconozca UTF-8 automáticamente
            bw.write('\uFEFF');
            bw.write(String.join(";", cabeceras));
            bw.newLine();
            for (List<String> fila : filas) {
                bw.write(String.join(";", fila.stream()
                        .map(CsvExporter::escapar)
                        .toList()));
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Escapa valores que contienen punto y coma, comillas o saltos de línea. */
    private static String escapar(String valor) {
        if (valor == null) return "";
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    /**
     * Fuerza que Excel trate el valor como texto usando el formato ="valor".
     * Usar para campos numéricos largos como IMEI que Excel convertiría a notación científica.
     */
    public static String textoForzado(String valor) {
        if (valor == null || valor.isEmpty()) return "";
        return "=\"" + valor.replace("\"", "\"\"") + "\"";
    }
}
