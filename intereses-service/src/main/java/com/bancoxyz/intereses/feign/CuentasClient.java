package com.bancoxyz.intereses.feign;

import com.bancoxyz.common.dto.CuentaResumenDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cuentas-service")
public interface CuentasClient {

    @GetMapping("/api/cuentas/resumen/{cuentaId}")
    CuentaResumenDTO obtenerResumen(@PathVariable("cuentaId") Long cuentaId);
}
