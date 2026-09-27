package com.bancoxyz.intereses.service;

import com.bancoxyz.common.dto.CuentaResumenDTO;
import com.bancoxyz.intereses.feign.CuentasClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CuentasOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(CuentasOrchestrationService.class);

    private final CuentasClient cuentasClient;

    public CuentasOrchestrationService(CuentasClient cuentasClient) {
        this.cuentasClient = cuentasClient;
    }

    @CircuitBreaker(name = "cuentasClient", fallbackMethod = "cuentasFallback")
    @Retry(name = "cuentasClient")
    public CuentaResumenDTO obtenerResumenCuenta(Long cuentaId) {
        return cuentasClient.obtenerResumen(cuentaId);
    }

    private CuentaResumenDTO cuentasFallback(Long cuentaId, Throwable throwable) {
        log.warn("Fallback activado para cuentas-service (cuentaId={}): {}", cuentaId, throwable.toString());
        return new CuentaResumenDTO(cuentaId, 0L, BigDecimal.ZERO, true);
    }
}
