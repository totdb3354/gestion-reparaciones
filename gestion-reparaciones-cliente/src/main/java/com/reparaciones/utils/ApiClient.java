package com.reparaciones.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

/**
 * Cliente HTTP centralizado para todas las llamadas a la API REST del servidor.
 * <p>Gestiona la URL base (leída de {@code config.properties}), el token JWT de sesión,
 * la serialización/deserialización JSON con Gson y la traducción de errores HTTP
 * a excepciones tipadas.</p>
 *
 * <p><b>Errores:</b></p>
 * <ul>
 *   <li>{@code 409} → {@link StaleDataException} (concurrencia optimista)</li>
 *   <li>{@code 401/403/404} → {@link SQLException} con mensaje descriptivo</li>
 *   <li>Error de red → {@link SQLException} wrapping la {@link IOException}</li>
 * </ul>
 *
 * <p>Los DAOs usan los métodos estáticos directamente y declaran {@code throws SQLException}
 * igual que antes — los controladores JavaFX no necesitan cambios.</p>
 */
public class ApiClient {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                @Override
                public void write(JsonWriter out, LocalDateTime value) throws IOException {
                    if (value == null) out.nullValue();
                    else out.value(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
                @Override
                public LocalDateTime read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
                    return LocalDateTime.parse(in.nextString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            })
            .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                @Override
                public void write(JsonWriter out, LocalDate value) throws IOException {
                    if (value == null) out.nullValue();
                    else out.value(value.toString());
                }
                @Override
                public LocalDate read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
                    return LocalDate.parse(in.nextString());
                }
            })
            .create();

    private static final String baseUrl;
    private static String token;

    static {
        String url = "http://localhost:8080";
        try (InputStream is = ApiClient.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                url = props.getProperty("api.url", url);
            }
        } catch (IOException ignored) {}
        baseUrl = url;
    }

    // ── Token ─────────────────────────────────────────────────────────────────

    /** Guarda el JWT recibido tras el login. Llamar desde {@code UsuarioDAO.login}. */
    public static void setToken(String jwt) { token = jwt; }

    /** Limpia el token al cerrar sesión. Llamar desde {@code Sesion.cerrar}. */
    public static void clearToken() { token = null; }

    // ── GET ───────────────────────────────────────────────────────────────────

    /**
     * GET que deserializa la respuesta al tipo indicado.
     * <p>Admite clases simples ({@code Componente.class}) y tipos genéricos complejos
     * construidos con {@link TypeToken} ({@code new TypeToken<Map<String,List<Componente>>>(){}.getType()}).</p>
     *
     * @param path ruta relativa a la base URL (p. ej. {@code "/componentes/stock-bajo"})
     * @param type tipo de destino para la deserialización
     * @return objeto deserializado, o {@code null} si la respuesta está vacía
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String path, Type type) throws SQLException {
        HttpResponse<String> response = send(builder(path).GET().build());
        handleErrors(response);
        String body = response.body();
        if (body == null || body.isBlank()) return null;
        return (T) GSON.fromJson(body, type);
    }

    /**
     * GET que deserializa la respuesta como lista del tipo indicado.
     *
     * @param path        ruta relativa a la base URL
     * @param elementType clase de cada elemento de la lista
     * @return lista deserializada, nunca {@code null}
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static <T> List<T> getList(String path, Class<T> elementType) throws SQLException {
        return get(path, TypeToken.getParameterized(List.class, elementType).getType());
    }

    /**
     * GET que espera una respuesta escalar {@code {"value": bool}}.
     *
     * @param path ruta relativa a la base URL
     * @return valor booleano del campo {@code value}
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static boolean getBoolean(String path) throws SQLException {
        JsonObject obj = get(path, JsonObject.class);
        return obj != null && obj.get("value").getAsBoolean();
    }

    /**
     * GET que espera una respuesta escalar {@code {"value": int}}.
     *
     * @param path ruta relativa a la base URL
     * @return valor entero del campo {@code value}, o {@code 0} si la respuesta es nula
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static int getInt(String path) throws SQLException {
        JsonObject obj = get(path, JsonObject.class);
        return obj == null ? 0 : obj.get("value").getAsInt();
    }

    /**
     * GET que espera una respuesta escalar {@code {"value": double}}.
     *
     * @param path ruta relativa a la base URL
     * @return valor decimal del campo {@code value}, o {@code 0.0} si la respuesta es nula
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static double getDouble(String path) throws SQLException {
        JsonObject obj = get(path, JsonObject.class);
        return obj == null ? 0.0 : obj.get("value").getAsDouble();
    }

    /**
     * GET que espera una respuesta escalar {@code {"value": "..."}} o {@code {"value": null}}.
     *
     * @param path ruta relativa a la base URL
     * @return valor del campo {@code value}, o {@code null} si es nulo o la respuesta está vacía
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static String getString(String path) throws SQLException {
        JsonObject obj = get(path, JsonObject.class);
        if (obj == null) return null;
        JsonElement el = obj.get("value");
        return (el == null || el.isJsonNull()) ? null : el.getAsString();
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    /**
     * POST sin cuerpo de respuesta esperado (respuesta 201/204).
     *
     * @param path ruta relativa a la base URL
     * @param body objeto a serializar como JSON en el body de la petición
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static void post(String path, Object body) throws SQLException {
        HttpResponse<String> response = send(builder(path)
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build());
        handleErrors(response);
    }

    /**
     * POST con cuerpo de respuesta deserializado.
     *
     * @param path         ruta relativa a la base URL
     * @param body         objeto a serializar como JSON
     * @param responseType tipo de la respuesta esperada
     * @return objeto deserializado de la respuesta
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static <T> T post(String path, Object body, Class<T> responseType) throws SQLException {
        HttpResponse<String> response = send(builder(path)
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build());
        handleErrors(response);
        String responseBody = response.body();
        if (responseBody == null || responseBody.isBlank()) return null;
        return GSON.fromJson(responseBody, responseType);
    }

    // ── PUT ───────────────────────────────────────────────────────────────────

    /**
     * PUT con body JSON, sin cuerpo de respuesta esperado.
     *
     * @param path ruta relativa a la base URL
     * @param body objeto a serializar como JSON
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static void put(String path, Object body) throws SQLException {
        HttpResponse<String> response = send(builder(path)
                .PUT(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build());
        handleErrors(response);
    }

    // ── PATCH ─────────────────────────────────────────────────────────────────

    /**
     * PATCH con body JSON opcional.
     *
     * @param path ruta relativa a la base URL
     * @param body objeto a serializar como JSON, o {@code null} para body vacío
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static void patch(String path, Object body) throws SQLException {
        HttpRequest.BodyPublisher publisher = body != null
                ? HttpRequest.BodyPublishers.ofString(GSON.toJson(body))
                : HttpRequest.BodyPublishers.noBody();
        HttpResponse<String> response = send(builder(path)
                .method("PATCH", publisher)
                .build());
        handleErrors(response);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * DELETE sin body.
     *
     * @param path ruta relativa a la base URL (incluye query params si los hay)
     * @throws SQLException si hay error de red o el servidor devuelve un código de error
     */
    public static void delete(String path) throws SQLException {
        HttpResponse<String> response = send(builder(path).DELETE().build());
        handleErrors(response);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static HttpRequest.Builder builder(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b;
    }

    private static HttpResponse<String> send(HttpRequest request) throws SQLException {
        try {
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            String detalle = e.getMessage() != null ? ": " + e.getMessage() : "";
            throw new SQLException("Sin conexión con el servidor" + detalle, e);
        }
    }

    private static void handleErrors(HttpResponse<String> response) throws SQLException {
        int status = response.statusCode();
        if (status >= 200 && status < 300) return;
        String msg = extractMessage(response.body());
        switch (status) {
            case 401 -> throw new SQLException("Sesión expirada. Vuelve a iniciar sesión.");
            case 403 -> throw new SQLException("No tienes permisos para realizar esta acción.");
            case 404 -> throw new SQLException("Recurso no encontrado.");
            case 409 -> throw new StaleDataException(msg);
            default  -> throw new SQLException("Error del servidor (" + status + "): " + msg);
        }
    }

    private static String extractMessage(String body) {
        if (body == null || body.isBlank()) return "Sin detalles.";
        try {
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            if (obj.has("message") && !obj.get("message").getAsString().isBlank())
                return obj.get("message").getAsString();
            if (obj.has("error")) return obj.get("error").getAsString();
        } catch (Exception ignored) {}
        return body;
    }
}
