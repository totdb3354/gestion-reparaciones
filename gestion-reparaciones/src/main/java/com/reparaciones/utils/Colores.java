package com.reparaciones.utils;

/**
 * Paleta de colores centralizada.
 * Los colores de marca se usan en estilos inline de controladores;
 * los colores del CSS (reparacion.css, main.css) se gestionan allí directamente.
 */
public final class Colores {

    private Colores() {} // impide instanciar — solo contiene constantes estáticas

    // ─── Paleta de marca ──────────────────────────────────────────────────────

    public static final String AZUL_NOCHE  = "#001232";
    public static final String AZUL_MEDIO  = "#2C3B54";
    public static final String AZUL_GRIS   = "#586376";
    public static final String CREMA       = "#F6F6F6";
    public static final String AMARILLO    = "#F1E356";

    // ─── Estados funcionales (filas tabla) ────────────────────────────────────

    /** Fila en modo edición activa — azul claro */
    public static final String FILA_EDICION_BG  = "#EBF4FF";
    public static final String FILA_EDICION_BRD = "#B3D4F5";

    /** Fila ya reparada (deshabilitada) — verde suave */
    public static final String FILA_REPARADO_BG  = "#EBF5EB";
    public static final String FILA_REPARADO_BRD = "#C5E1C5";
    public static final String FILA_REPARADO_ICO = "#8AC7AF";

    /** Fila con solicitud de pieza pendiente — naranja */
    public static final String FILA_SOLICITUD_BG  = "rgba(255,165,0,0.12)";
    public static final String FILA_SOLICITUD_BRD = "#FFA500";

    /** Fila con incidencia activa — rojo suave */
    public static final String FILA_INCIDENCIA_BG  = "rgba(251,136,136,0.16)";
    public static final String FILA_INCIDENCIA_BRD = "#FB8888";

    // ─── Texto funcional ──────────────────────────────────────────────────────

    /** Texto de advertencia / incidencia */
    public static final String TEXTO_ERROR    = "#CC4444";

    /** Texto de acción secundaria — azul medio */
    public static final String TEXTO_ACCION   = "#5B8CFF";

    /** Verde confirmación */
    public static final String VERDE_OK       = "#4CAF50";
}
