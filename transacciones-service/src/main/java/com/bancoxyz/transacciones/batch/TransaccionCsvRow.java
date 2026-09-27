package com.bancoxyz.transacciones.batch;

/**
 * Fila cruda del CSV, todos los campos como String para que el reader nunca falle
 * al parsear: la validacion/conversion de tipos ocurre a proposito en el ItemProcessor.
 */
public class TransaccionCsvRow {

    private String id;
    private String fecha;
    private String monto;
    private String tipo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getMonto() {
        return monto;
    }

    public void setMonto(String monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String rawKey() {
        return String.join("|",
                trimOrEmpty(id), trimOrEmpty(fecha), trimOrEmpty(monto), trimOrEmpty(tipo));
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
