package com.bancoxyz.bff.web;

import com.bancoxyz.bff.client.CuentasClient;
import com.bancoxyz.common.dto.CuentaResumenDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bff")
public class BffController {

    private final CuentasClient cuentasClient;

    public BffController(CuentasClient cuentasClient) {
        this.cuentasClient = cuentasClient;
    }

    // 1. BFF WEB: Información detallada y completa para navegadores
    @GetMapping("/web/cuenta/{id}")
    public Map<String, Object> getCuentaParaWeb(@PathVariable Long id) {
        CuentaResumenDTO cuenta = cuentasClient.obtenerResumenCuenta(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("cuentaId", cuenta.cuentaId());
        response.put("totalMovimientos", cuenta.totalMovimientos());
        response.put("montoTotal", cuenta.montoTotal());
        response.put("fallback", cuenta.fallback());
        response.put("canal", "Portal Web Completo");
        return response;
    }

    // 2. BFF MÓVIL: Respuesta ligera optimizada para ahorrar datos en smartphones
    @GetMapping("/movil/cuenta/{id}")
    public Map<String, Object> getCuentaParaMovil(@PathVariable Long id) {
        CuentaResumenDTO cuenta = cuentasClient.obtenerResumenCuenta(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("cuentaId", cuenta.cuentaId());
        response.put("montoTotal", cuenta.montoTotal()); // Solo los datos esenciales
        response.put("canal", "App Móvil Ligera");
        return response;
    }

    // 3. BFF CAJERO (ATM): Optimizado para consultas rápidas de saldo y transacciones
    @GetMapping("/cajero/cuenta/{id}")
    public Map<String, Object> getCuentaParaCajero(@PathVariable Long id) {
        CuentaResumenDTO cuenta = cuentasClient.obtenerResumenCuenta(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("cuentaId", cuenta.cuentaId());
        response.put("saldoDisponible", cuenta.montoTotal());
        response.put("canal", "Cajero Automático (ATM)");
        return response;
    }
}