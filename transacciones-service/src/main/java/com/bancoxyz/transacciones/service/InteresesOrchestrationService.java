package com.bancoxyz.transacciones.service;

import com.bancoxyz.common.dto.InteresResumenDTO;
import com.bancoxyz.transacciones.feign.InteresesClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class InteresesOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(InteresesOrchestrationService.class);

    private final InteresesClient interesesClient;

    public InteresesOrchestrationService(InteresesClient interesesClient) {
        this.interesesClient = interesesClient;
    }

    @CircuitBreaker(name = "interesesClient", fallbackMethod = "interesesFallback")
    @Retry(name = "interesesClient")
    public InteresResumenDTO obtenerResumenIntereses(Long cuentaId) {
        return interesesClient.obtenerResumen(cuentaId);
    }

    private InteresResumenDTO interesesFallback(Long cuentaId, Throwable throwable) {
        log.warn("Fallback activado para intereses-service (cuentaId={}): {}", cuentaId, throwable.toString());
        return new InteresResumenDTO(cuentaId, "no disponible", BigDecimal.ZERO, "desconocido", BigDecimal.ZERO, true);
    }
}
