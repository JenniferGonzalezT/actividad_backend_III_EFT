package com.bancoxyz.common.dto;

import java.math.BigDecimal;

public record InteresResumenDTO(
        Long cuentaId,
        String nombre,
        BigDecimal saldo,
        String tipo,
        BigDecimal interesMensualEstimado,
        boolean fallback
) {
}
