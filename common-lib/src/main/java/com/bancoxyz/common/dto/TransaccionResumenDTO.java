package com.bancoxyz.common.dto;

import java.math.BigDecimal;

public record TransaccionResumenDTO(
        Long id,
        String fecha,
        BigDecimal monto,
        String tipo,
        boolean fallback
) {
}
