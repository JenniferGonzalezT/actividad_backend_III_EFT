package com.bancoxyz.transacciones.web;

import java.math.BigDecimal;

/** tipo: "credito" (deposito) o "debito" (retiro). */
public record NuevaTransaccionRequest(Long cuentaId, BigDecimal monto, String tipo) {
}
