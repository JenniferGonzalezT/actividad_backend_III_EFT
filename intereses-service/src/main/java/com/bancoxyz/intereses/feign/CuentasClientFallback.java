package com.bancoxyz.intereses.feign;

import com.bancoxyz.common.dto.CuentaResumenDTO;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CuentasClientFallback implements CuentasClient {

    @Override
    public CuentaResumenDTO obtenerResumen(Long cuentaId) {
        return new CuentaResumenDTO(
                cuentaId, 
                0L, 
                BigDecimal.ZERO, 
                true
        );
    }
}