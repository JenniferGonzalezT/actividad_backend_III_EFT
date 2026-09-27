package com.bancoxyz.cuentas.batch;

import com.bancoxyz.cuentas.entity.CuentaAnual;
import com.bancoxyz.cuentas.service.MigracionEstadoService;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Limpia los problemas descritos en el README para cuentas_anuales.csv: fechas
 * inconsistentes, montos negativos/vacios, y la variante acentuada "depósito" (que se
 * normaliza, no se descarta) frente a "pago" que no es un valor valido. La descripcion
 * vacia se rellena con un valor por defecto en vez de descartar la fila.
 */
public class CuentaAnualItemProcessor implements ItemProcessor<CuentaAnualCsvRow, CuentaAnual> {

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private static final List<String> TRANSACCIONES_VALIDAS = List.of("deposito", "retiro", "compra");
    private static final String DESCRIPCION_POR_DEFECTO = "Sin descripción";

    private final DuplicateGuard duplicateGuard;
    private final MigracionEstadoService estadoService;

    public CuentaAnualItemProcessor(DuplicateGuard duplicateGuard, MigracionEstadoService estadoService) {
        this.duplicateGuard = duplicateGuard;
        this.estadoService = estadoService;
    }

    @Override
    public CuentaAnual process(CuentaAnualCsvRow item) {
        estadoService.incrementarLeidos();

        if (duplicateGuard.esDuplicado(item.rawKey())) {
            throw new RegistroDuplicadoException("Fila duplicada: " + item.rawKey());
        }

        Long cuentaId = parseCuentaId(item.getCuentaId());
        LocalDate fecha = parseFecha(item.getFecha());
        String transaccion = parseTransaccion(item.getTransaccion());
        BigDecimal monto = parseMonto(item.getMonto());
        String descripcion = parseDescripcion(item.getDescripcion());

        return new CuentaAnual(cuentaId, fecha, transaccion, monto, descripcion);
    }

    private Long parseCuentaId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RegistroInvalidoException("cuenta_id vacio");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new RegistroInvalidoException("cuenta_id invalido: " + raw);
        }
    }

    private LocalDate parseFecha(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RegistroInvalidoException("fecha vacia");
        }
        String valor = raw.trim();
        for (DateTimeFormatter formatter : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(valor, formatter);
            } catch (java.time.DateTimeException e) {
                // probar el siguiente formato
            }
        }
        throw new RegistroInvalidoException("fecha invalida: " + raw);
    }

    private String parseTransaccion(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RegistroInvalidoException("transaccion vacia");
        }
        String sinTildes = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        if (!TRANSACCIONES_VALIDAS.contains(sinTildes)) {
            throw new RegistroInvalidoException("transaccion invalida: " + raw);
        }
        return sinTildes;
    }

    private BigDecimal parseMonto(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RegistroInvalidoException("monto vacio");
        }
        BigDecimal monto;
        try {
            monto = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new RegistroInvalidoException("monto invalido: " + raw);
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegistroInvalidoException("monto fuera de rango: " + raw);
        }
        return monto;
    }

    private String parseDescripcion(String raw) {
        if (raw == null || raw.isBlank()) {
            return DESCRIPCION_POR_DEFECTO;
        }
        return raw.trim();
    }
}
