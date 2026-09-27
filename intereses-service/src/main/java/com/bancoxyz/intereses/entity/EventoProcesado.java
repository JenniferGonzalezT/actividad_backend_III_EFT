package com.bancoxyz.intereses.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Registro de eventos ya aplicados; garantiza idempotencia ante reentregas de Kafka. */
@Entity
@Table(name = "evento_procesado")
public class EventoProcesado {

    @Id
    private String eventId;

    public EventoProcesado() {
    }

    public EventoProcesado(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
