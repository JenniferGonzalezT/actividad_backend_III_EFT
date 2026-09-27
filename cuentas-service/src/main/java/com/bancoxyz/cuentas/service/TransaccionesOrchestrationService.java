package com.bancoxyz.cuentas.service;

import com.bancoxyz.common.dto.TransaccionResumenDTO;
import com.bancoxyz.cuentas.feign.TransaccionesClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransaccionesOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(TransaccionesOrchestrationService.class);

    private final TransaccionesClient transaccionesClient;

    public TransaccionesOrchestrationService(TransaccionesClient transaccionesClient) {
        this.transaccionesClient = transaccionesClient;
    }

    @CircuitBreaker(name = "transaccionesClient", fallbackMethod = "transaccionFallback")
    @Retry(name = "transaccionesClient")
    public TransaccionResumenDTO obtenerTransaccion(Long id) {
        return transaccionesClient.obtenerTransaccion(id);
    }

    private TransaccionResumenDTO transaccionFallback(Long id, Throwable throwable) {
        log.warn("Fallback activado para transacciones-service (id={}): {}", id, throwable.toString());
        return new TransaccionResumenDTO(id, "no disponible", BigDecimal.ZERO, "desconocido", true);
    }
}
