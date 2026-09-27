package com.bancoxyz.cuentas.feign;

import com.bancoxyz.common.dto.TransaccionResumenDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "transacciones-service")
public interface TransaccionesClient {

    @GetMapping("/api/transacciones/{id}")
    TransaccionResumenDTO obtenerTransaccion(@PathVariable("id") Long id);
}
