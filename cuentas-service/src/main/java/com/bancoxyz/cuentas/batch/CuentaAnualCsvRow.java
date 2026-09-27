package com.bancoxyz.cuentas.batch;

public class CuentaAnualCsvRow {

    private String cuentaId;
    private String fecha;
    private String transaccion;
    private String monto;
    private String descripcion;

    public String getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(String cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getTransaccion() {
        return transaccion;
    }

    public void setTransaccion(String transaccion) {
        this.transaccion = transaccion;
    }

    public String getMonto() {
        return monto;
    }

    public void setMonto(String monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String rawKey() {
        return String.join("|",
                trimOrEmpty(cuentaId), trimOrEmpty(fecha), trimOrEmpty(transaccion),
                trimOrEmpty(monto), trimOrEmpty(descripcion));
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
