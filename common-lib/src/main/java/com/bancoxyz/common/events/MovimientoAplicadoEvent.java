package com.bancoxyz.common.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Publicado por cuentas-service cuando registra el movimiento en la cuenta. */
public record MovimientoAplicadoEvent(
        String eventId,
        String correlationId,
        Instant occurredAt,
        Long transaccionId,
        Long cuentaId,
        String movimiento,
        BigDecimal monto
) implements EventoTransaccional {
}
