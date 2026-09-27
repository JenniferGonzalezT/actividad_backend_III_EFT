package com.bancoxyz.transacciones.feign;

import com.bancoxyz.common.dto.InteresResumenDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "intereses-service")
public interface InteresesClient {

    @GetMapping("/api/intereses/resumen/{cuentaId}")
    InteresResumenDTO obtenerResumen(@PathVariable("cuentaId") Long cuentaId);
}
