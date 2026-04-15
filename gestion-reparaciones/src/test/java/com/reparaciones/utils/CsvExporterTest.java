package com.reparaciones.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvExporterTest {

    // --- textoForzado ---

    @Test
    void textoForzado_imei_envuelveEnFormato() {
        assertEquals("=\"123456789012345\"", CsvExporter.textoForzado("123456789012345"));
    }

    @Test
    void textoForzado_valorNulo_devuelveCadenaVacia() {
        assertEquals("", CsvExporter.textoForzado(null));
    }

    @Test
    void textoForzado_valorVacio_devuelveCadenaVacia() {
        assertEquals("", CsvExporter.textoForzado(""));
    }

    @Test
    void textoForzado_comillasInternas_escapadas() {
        // Si el valor tiene comillas, deben duplicarse dentro del formato ="..."
        assertEquals("=\"valor con \"\"comillas\"\"\"", CsvExporter.textoForzado("valor con \"comillas\""));
    }

    @Test
    void textoForzado_numeroCorto_envuelveIgual() {
        assertEquals("=\"42\"", CsvExporter.textoForzado("42"));
    }
}
