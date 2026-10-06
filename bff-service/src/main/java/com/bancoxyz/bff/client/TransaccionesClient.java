package com.bancoxyz.bff.client;

import com.bancoxyz.common.dto.TransaccionResumenDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "transacciones-service")
public interface TransaccionesClient {
    @GetMapping("/api/transacciones/resumen/{cuentaId}")
    TransaccionResumenDTO obtenerResumenTransaccion(@PathVariable("cuentaId") Long cuentaId);
}