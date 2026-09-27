package com.bancoxyz.cuentas.web;

import com.bancoxyz.common.dto.CuentaResumenDTO;
import com.bancoxyz.common.dto.TransaccionResumenDTO;
import com.bancoxyz.cuentas.entity.CuentaAnual;
import com.bancoxyz.cuentas.repository.CuentaAnualRepository;
import com.bancoxyz.cuentas.service.MigracionEstadoService;
import com.bancoxyz.cuentas.service.TransaccionesOrchestrationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CuentaAnualController {

    private final CuentaAnualRepository repository;
    private final MigracionEstadoService estadoService;
    private final TransaccionesOrchestrationService transaccionesOrchestrationService;
    private final MigracionJobRunner migracionJobRunner;

    public CuentaAnualController(CuentaAnualRepository repository,
                                  MigracionEstadoService estadoService,
                                  TransaccionesOrchestrationService transaccionesOrchestrationService,
                                  MigracionJobRunner migracionJobRunner) {
        this.repository = repository;
        this.estadoService = estadoService;
        this.transaccionesOrchestrationService = transaccionesOrchestrationService;
        this.migracionJobRunner = migracionJobRunner;
    }

    @GetMapping("/cuentas")
    public Page<CuentaAnual> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @GetMapping("/cuentas/{id}")
    public ResponseEntity<CuentaAnual> obtener(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cuentas/resumen/{cuentaId}")
    public CuentaResumenDTO resumen(@PathVariable Long cuentaId) {
        List<CuentaAnual> movimientos = repository.findByCuentaId(cuentaId);
        if (movimientos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No hay movimientos para cuentaId=" + cuentaId);
        }
        BigDecimal montoTotal = movimientos.stream()
                .map(CuentaAnual::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CuentaResumenDTO(cuentaId, movimientos.size(), montoTotal, false);
    }

    @GetMapping("/cuentas/resumen-con-transaccion/{transaccionId}")
    public TransaccionResumenDTO resumenConTransaccion(@PathVariable Long transaccionId) {
        return transaccionesOrchestrationService.obtenerTransaccion(transaccionId);
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
