package com.bancoxyz.transacciones.feign;

import com.bancoxyz.common.dto.InteresResumenDTO;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class InteresesClientFallback implements InteresesClient {

    @Override
    public InteresResumenDTO obtenerResumen(Long cuentaId) {
        return new InteresResumenDTO(
                cuentaId, 
                "SISTEMA_NO_DISPONIBLE", 
                BigDecimal.ZERO, 
                "FALLBACK", 
                BigDecimal.ZERO, 
                true
        );
    }
}