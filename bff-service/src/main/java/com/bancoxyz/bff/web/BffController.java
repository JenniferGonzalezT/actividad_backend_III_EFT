package com.bancoxyz.bff.web;

import com.bancoxyz.bff.client.CuentasClient;
import com.bancoxyz.bff.client.InteresesClient;
import com.bancoxyz.bff.client.TransaccionesClient;
import com.bancoxyz.common.dto.CuentaResumenDTO;
import com.bancoxyz.common.dto.InteresResumenDTO;
import com.bancoxyz.common.dto.TransaccionResumenDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bff")
public class BffController {

    private final CuentasClient cuentasClient;
    private final InteresesClient interesesClient;
    private final TransaccionesClient transaccionesClient;

    public BffController(CuentasClient cuentasClient, InteresesClient interesesClient, TransaccionesClient transaccionesClient) {
        this.cuentasClient = cuentasClient;
        this.interesesClient = interesesClient;
        this.transaccionesClient = transaccionesClient;
    }

    // 1. BFF WEB: Información detallada y completa con agregación segura
    @GetMapping("/web/cuenta/{id}")
    public Map<String, Object> getCuentaParaWeb(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            CuentaResumenDTO cuenta = cuentasClient.obtenerResumenCuenta(id);
            response.put("cuenta", cuenta);
        } catch (Exception e) {
            response.put("cuenta", Map.of("cuentaId", id, "montoTotal", BigDecimal.ZERO));
        }

        try {
            InteresResumenDTO interes = interesesClient.obtenerResumenInteres(id);
            response.put("interes", interes);
        } catch (Exception e) {
            response.put("interes", Map.of("mensaje", "Interés no registrado para este ID"));
        }

        try {
            TransaccionResumenDTO transaccion = transaccionesClient.obtenerResumenTransaccion(id);
            response.put("transaccion_reciente", transaccion);
        } catch (Exception e) {
            response.put("transaccion_reciente", Map.of("mensaje", "Sin transacciones recientes"));
        }

        response.put("canal", "Portal Web Completo (Agregado)");
        return response;
    }

    // 2. BFF MÓVIL: Respuesta ligera optimizada
    @GetMapping("/movil/cuenta/{id}")
    public Map<String, Object> getCuentaParaMovil(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            CuentaResumenDTO cuenta = cuentasClient.obtenerResumenCuenta(id);
            response.put("cuentaId", cuenta.cuentaId());
            response.put("montoTotal", cuenta.montoTotal());
        } catch (Exception e) {
            response.put("cuentaId", id);
            response.put("montoTotal", BigDecimal.ZERO);
        }
        response.put("canal", "App Móvil Ligera");
        return response;
    }

    // 3. BFF CAJERO (ATM): Optimizado para consultas rápidas
    @GetMapping("/cajero/cuenta/{id}")
    public Map<String, Object> getCuentaParaCajero(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            CuentaResumenDTO cuenta = cuentasClient.obtenerResumenCuenta(id);
            response.put("cuentaId", cuenta.cuentaId());
            response.put("saldoDisponible", cuenta.montoTotal());
        } catch (Exception e) {
            response.put("cuentaId", id);
            response.put("saldoDisponible", BigDecimal.ZERO);
        }
        response.put("canal", "Cajero Automático (ATM)");
        return response;
    }
}