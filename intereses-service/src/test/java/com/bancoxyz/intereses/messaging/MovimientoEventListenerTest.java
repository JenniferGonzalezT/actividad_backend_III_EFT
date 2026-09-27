package com.bancoxyz.intereses.messaging;

import com.bancoxyz.common.events.InteresRecalculadoEvent;
import com.bancoxyz.common.events.MovimientoAplicadoEvent;
import com.bancoxyz.intereses.entity.Interes;
import com.bancoxyz.intereses.repository.EventoProcesadoRepository;
import com.bancoxyz.intereses.repository.InteresRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MovimientoEventListenerTest {

    private final InteresRepository intereses = mock(InteresRepository.class);
    private final EventoProcesadoRepository procesados = mock(EventoProcesadoRepository.class);
    private final InteresEventPublisher publisher = mock(InteresEventPublisher.class);
    private final MovimientoEventListener listener = new MovimientoEventListener(intereses, procesados, publisher);

    private MovimientoAplicadoEvent evento(String movimiento) {
        return new MovimientoAplicadoEvent("e1", "c1", Instant.now(), 7L, 103L, movimiento, new BigDecimal("200"));
    }

    @Test
    void retiroRestaSaldoYPublicaResultado() throws Exception {
        Interes i = new Interes(103L, "Ana", new BigDecimal("1000"), 30, "ahorro");
        when(intereses.findFirstByCuentaId(103L)).thenReturn(Optional.of(i));

        listener.onMovimientoAplicado(evento("retiro"));

        ArgumentCaptor<InteresRecalculadoEvent> ev = ArgumentCaptor.forClass(InteresRecalculadoEvent.class);
        verify(publisher).publicar(ev.capture());
        assertEquals(new BigDecimal("800"), ev.getValue().nuevoSaldo());
    }

    @Test
    void cuentaInexistenteLanzaExcepcionParaReintentoYDlt() {
        when(intereses.findFirstByCuentaId(103L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> listener.onMovimientoAplicado(evento("deposito")));
        verifyNoInteractions(publisher);
    }

    @Test
    void eventoDuplicadoSeIgnora() throws Exception {
        when(procesados.existsById("e1")).thenReturn(true);

        listener.onMovimientoAplicado(evento("deposito"));

        verify(intereses, never()).save(any());
    }
}
