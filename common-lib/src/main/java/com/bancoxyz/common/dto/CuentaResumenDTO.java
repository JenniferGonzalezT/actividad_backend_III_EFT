package com.bancoxyz.common.dto;

import java.math.BigDecimal;

public record CuentaResumenDTO(
        Long cuentaId,
        long totalMovimientos,
        BigDecimal montoTotal,
        boolean fallback
) {
}
