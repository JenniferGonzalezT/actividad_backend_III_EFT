package com.bancoxyz.intereses.batch;

public class InteresCsvRow {

    private String cuentaId;
    private String nombre;
    private String saldo;
    private String edad;
    private String tipo;

    public String getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(String cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSaldo() {
        return saldo;
    }

    public void setSaldo(String saldo) {
        this.saldo = saldo;
    }

    public String getEdad() {
        return edad;
    }

    public void setEdad(String edad) {
        this.edad = edad;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String rawKey() {
        return String.join("|",
                trimOrEmpty(cuentaId), trimOrEmpty(nombre), trimOrEmpty(saldo), trimOrEmpty(edad), trimOrEmpty(tipo));
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
