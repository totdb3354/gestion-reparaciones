package com.reparaciones.models;

import javafx.beans.property.*;

/** Representa una línea de un pedido multi-componente antes de persistirse. */
public class LineaPedido {

    private final ObjectProperty<Componente> componente  = new SimpleObjectProperty<>();
    private final IntegerProperty            cantidad    = new SimpleIntegerProperty(1);
    private final DoubleProperty             precioUnidad= new SimpleDoubleProperty(0.0);
    private final BooleanProperty            esUrgente   = new SimpleBooleanProperty(false);

    public LineaPedido() {}

    public LineaPedido(Componente componente, int cantidad, double precioUnidad, boolean esUrgente) {
        this.componente.set(componente);
        this.cantidad.set(cantidad);
        this.precioUnidad.set(precioUnidad);
        this.esUrgente.set(esUrgente);
    }

    // ── Properties ────────────────────────────────────────────────────────────
    public ObjectProperty<Componente> componenteProperty()  { return componente; }
    public IntegerProperty            cantidadProperty()     { return cantidad; }
    public DoubleProperty             precioUnidadProperty() { return precioUnidad; }
    public BooleanProperty            esUrgenteProperty()    { return esUrgente; }

    // ── Getters / setters ─────────────────────────────────────────────────────
    public Componente getComponente()              { return componente.get(); }
    public int        getCantidad()                { return cantidad.get(); }
    public double     getPrecioUnidad()            { return precioUnidad.get(); }
    public boolean    isEsUrgente()                { return esUrgente.get(); }

    public void setComponente(Componente c)        { componente.set(c); }
    public void setCantidad(int v)                 { cantidad.set(v); }
    public void setPrecioUnidad(double v)          { precioUnidad.set(v); }
    public void setEsUrgente(boolean v)            { esUrgente.set(v); }
}
