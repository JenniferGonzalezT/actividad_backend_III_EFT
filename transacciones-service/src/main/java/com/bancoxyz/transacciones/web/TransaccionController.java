package com.bancoxyz.transacciones.web;

import com.bancoxyz.common.dto.InteresResumenDTO;
import com.bancoxyz.transacciones.entity.Transaccion;
import com.bancoxyz.transacciones.repository.TransaccionRepository;
import com.bancoxyz.transacciones.service.InteresesOrchestrationService;
import com.bancoxyz.transacciones.service.MigracionEstadoService;
import com.bancoxyz.transacciones.service.TransaccionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TransaccionController {

    private final TransaccionRepository repository;
    private final MigracionEstadoService estadoService;
    private final InteresesOrchestrationService interesesOrchestrationService;
    private final MigracionJobRunner migracionJobRunner;
    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionRepository repository,
                                  MigracionEstadoService estadoService,
                                  InteresesOrchestrationService interesesOrchestrationService,
                                  MigracionJobRunner migracionJobRunner,
                                  TransaccionService transaccionService) {
        this.repository = repository;
        this.estadoService = estadoService;
        this.interesesOrchestrationService = interesesOrchestrationService;
        this.migracionJobRunner = migracionJobRunner;
        this.transaccionService = transaccionService;
    }

    @GetMapping("/transacciones")
    public Page<Transaccion> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /** Registra una transaccion de forma asincrona: responde 202 y la saga continua por Kafka. */
    @PostMapping("/transacciones")
    public ResponseEntity<Transaccion> registrar(@RequestBody NuevaTransaccionRequest request) {
        return ResponseEntity.accepted().body(transaccionService.registrar(request));
    }

    @GetMapping("/transacciones/{id}")
    public ResponseEntity<Transaccion> obtener(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/transacciones/resumen-con-intereses/{cuentaId}")
    public InteresResumenDTO resumenConIntereses(@PathVariable Long cuentaId) {
        return interesesOrchestrationService.obtenerResumenIntereses(cuentaId);
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
