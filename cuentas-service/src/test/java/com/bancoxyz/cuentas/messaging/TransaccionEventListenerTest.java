package com.bancoxyz.cuentas.messaging;

import com.bancoxyz.common.events.MovimientoAplicadoEvent;
import com.bancoxyz.common.events.TransaccionRegistradaEvent;
import com.bancoxyz.cuentas.entity.CuentaAnual;
import com.bancoxyz.cuentas.repository.CuentaAnualRepository;
import com.bancoxyz.cuentas.repository.EventoProcesadoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransaccionEventListenerTest {

    private final CuentaAnualRepository cuentas = mock(CuentaAnualRepository.class);
    private final EventoProcesadoRepository procesados = mock(EventoProcesadoRepository.class);
    private final MovimientoEventPublisher publisher = mock(MovimientoEventPublisher.class);
    private final TransaccionEventListener listener = new TransaccionEventListener(cuentas, procesados, publisher);

    private TransaccionRegistradaEvent evento(String tipo) {
        return new TransaccionRegistradaEvent("e1", "c1", Instant.now(), 7L, 103L, tipo, new BigDecimal("500"), "2024-01-01");
    }

    @Test
    void creditoRegistraDepositoYPublicaMovimiento() throws Exception {
        listener.onTransaccionRegistrada(evento("credito"));

        ArgumentCaptor<CuentaAnual> c = ArgumentCaptor.forClass(CuentaAnual.class);
        verify(cuentas).save(c.capture());
        assertEquals("deposito", c.getValue().getTransaccion());
        ArgumentCaptor<MovimientoAplicadoEvent> ev = ArgumentCaptor.forClass(MovimientoAplicadoEvent.class);
        verify(publisher).publicar(ev.capture());
        assertEquals(7L, ev.getValue().transaccionId());
        assertEquals("c1", ev.getValue().correlationId());
    }

    @Test
    void eventoDuplicadoSeIgnora() throws Exception {
        when(procesados.existsById("e1")).thenReturn(true);

        listener.onTransaccionRegistrada(evento("debito"));

        verify(cuentas, never()).save(any());
        verify(publisher, never()).publicar(any());
    }
}
