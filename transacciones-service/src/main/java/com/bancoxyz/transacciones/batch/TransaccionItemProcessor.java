package com.bancoxyz.transacciones.batch;

import com.bancoxyz.transacciones.entity.Transaccion;
import com.bancoxyz.transacciones.service.MigracionEstadoService;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Limpia los problemas descritos en el README para transacciones.csv: formatos de
 * fecha inconsistentes, montos negativos/vacios/cero, tipos invalidos y duplicados.
 */
public class TransaccionItemProcessor implements ItemProcessor<TransaccionCsvRow, Transaccion> {

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private static final List<String> TIPOS_VALIDOS = List.of("debito", "credito");

    private final DuplicateGuard duplicateGuard;
    private final MigracionEstadoService estadoService;

    public TransaccionItemProcessor(DuplicateGuard duplicateGuard, MigracionEstadoService estadoService) {
        this.duplicateGuard = duplicateGuard;
        this.estadoService = estadoService;
    }

    @Override
    public Transaccion process(TransaccionCsvRow item) {
        estadoService.incrementarLeidos();

        if (duplicateGuard.esDuplicado(item.rawKey())) {
            throw new RegistroDuplicadoException("Fila duplicada: " + item.rawKey());
        }

        Long id = parseId(item.getId());
        LocalDate fecha = parseFecha(item.getFecha());
        BigDecimal monto = parseMonto(item.getMonto());
        String tipo = parseTipo(item.getTipo());

        return new Transaccion(id, fecha, monto, tipo);
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RegistroInvalidoException("id vacio");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new RegistroInvalidoException("id invalido: " + raw);
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

    private String parseTipo(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RegistroInvalidoException("tipo vacio");
        }
        String tipo = raw.trim().toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new RegistroInvalidoException("tipo invalido: " + raw);
        }
        return tipo;
    }
}
