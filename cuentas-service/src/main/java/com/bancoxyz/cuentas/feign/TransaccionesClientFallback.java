package com.bancoxyz.cuentas.feign;

import com.bancoxyz.common.dto.TransaccionResumenDTO;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TransaccionesClientFallback implements TransaccionesClient {

    @Override
    public TransaccionResumenDTO obtenerTransaccion(Long id) {
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Constructor del Record: id, fecha, monto, tipo, fallback
        return new TransaccionResumenDTO(
                id, 
                fechaActual, 
                BigDecimal.ZERO, 
                "NO_DISPONIBLE", 
                true
        );
    }
}