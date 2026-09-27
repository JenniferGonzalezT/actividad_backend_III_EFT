package com.bancoxyz.intereses.web;

import com.bancoxyz.common.dto.CuentaResumenDTO;
import com.bancoxyz.common.dto.InteresResumenDTO;
import com.bancoxyz.intereses.entity.Interes;
import com.bancoxyz.intereses.repository.InteresRepository;
import com.bancoxyz.intereses.service.CuentasOrchestrationService;
import com.bancoxyz.intereses.service.MigracionEstadoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InteresController {

    private static final Map<String, BigDecimal> TASA_ANUAL = Map.of(
            "ahorro", new BigDecimal("0.02"),
            "prestamo", new BigDecimal("0.12"),
            "hipoteca", new BigDecimal("0.06")
    );

    private final InteresRepository repository;
    private final MigracionEstadoService estadoService;
    private final CuentasOrchestrationService cuentasOrchestrationService;
    private final MigracionJobRunner migracionJobRunner;

    public InteresController(InteresRepository repository,
                              MigracionEstadoService estadoService,
                              CuentasOrchestrationService cuentasOrchestrationService,
                              MigracionJobRunner migracionJobRunner) {
        this.repository = repository;
        this.estadoService = estadoService;
        this.cuentasOrchestrationService = cuentasOrchestrationService;
        this.migracionJobRunner = migracionJobRunner;
    }

    @GetMapping("/intereses")
    public Page<Interes> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @GetMapping("/intereses/{id}")
    public ResponseEntity<Interes> obtener(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/intereses/resumen/{cuentaId}")
    public InteresResumenDTO resumen(@PathVariable Long cuentaId) {
        Interes interes = repository.findFirstByCuentaId(cuentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay registros de intereses para cuentaId=" + cuentaId));
        BigDecimal tasaAnual = TASA_ANUAL.getOrDefault(interes.getTipo(), BigDecimal.ZERO);
        BigDecimal interesMensual = interes.getSaldo()
                .multiply(tasaAnual)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        return new InteresResumenDTO(cuentaId, interes.getNombre(), interes.getSaldo(), interes.getTipo(),
                interesMensual, false);
    }

    @GetMapping("/intereses/resumen-con-cuenta/{cuentaId}")
    public CuentaResumenDTO resumenConCuenta(@PathVariable Long cuentaId) {
        return cuentasOrchestrationService.obtenerResumenCuenta(cuentaId);
    }

    @GetMapping("/migracion/status")
    public MigracionEstadoService.Estado estadoMigracion() {
        return estadoService.snapshot();
    }

    @PostMapping("/migracion/rerun")
    public Map<String, String> reejecutarMigracion() {
        migracionJobRunner.ejecutar();
        return Map.of("status", "migracion re-ejecutada");
    }
}
