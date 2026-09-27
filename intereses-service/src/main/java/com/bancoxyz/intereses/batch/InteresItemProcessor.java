package com.bancoxyz.intereses.batch;

import com.bancoxyz.intereses.entity.Interes;
import com.bancoxyz.intereses.service.MigracionEstadoService;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Limpia los problemas descritos en el README para intereses.csv: saldos vacios,
 * edades no validas, tipos invalidos y filas duplicadas (9 casos reales confirmados
 * al perfilar data/semana_3/intereses.csv).
 */
public class InteresItemProcessor implements ItemProcessor<InteresCsvRow, Interes> {

    private static final List<String> TIPOS_VALIDOS = List.of("ahorro", "prestamo", "hipoteca");
    private static final int EDAD_MINIMA = 1;
    private static final int EDAD_MAXIMA = 100;

    private final DuplicateGuard duplicateGuard;
    private final MigracionEstadoService estadoService;

    public InteresItemProcessor(DuplicateGuard duplicateGuard, MigracionEstadoService estadoService) {
        this.duplicateGuard = duplicateGuard;
        this.estadoService = estadoService;
    }

    @Override
    public Interes process(InteresCsvRow item) {
        estadoService.incrementarLeidos();

        if (duplicateGuard.esDuplicado(item.rawKey())) {
            throw new RegistroDuplicadoException("Fila duplicada: " + item.rawKey());
        }

        Long cuentaId = parseCuentaId(item.getCuentaId());
        String nombre = parseNombre(item.getNombre());
        BigDecimal saldo = parseSaldo(item.getSaldo());
        Integer edad = parseEdad(item.getEdad());
        String tipo = parseTipo(item.getTipo());

        return new Interes(cuentaId, nombre, saldo, edad, tipo);
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

    private String parseNombre(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RegistroInvalidoException("nombre vacio");
        }
        return raw.trim();
    }

    private BigDecimal parseSaldo(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RegistroInvalidoException("saldo vacio");
        }
        BigDecimal saldo;
        try {
            saldo = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new RegistroInvalidoException("saldo invalido: " + raw);
        }
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegistroInvalidoException("saldo fuera de rango: " + raw);
        }
        return saldo;
    }

    private Integer parseEdad(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RegistroInvalidoException("edad vacia");
        }
        int edad;
        try {
            edad = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new RegistroInvalidoException("edad invalida: " + raw);
        }
        if (edad < EDAD_MINIMA || edad > EDAD_MAXIMA) {
            throw new RegistroInvalidoException("edad fuera de rango: " + raw);
        }
        return edad;
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
