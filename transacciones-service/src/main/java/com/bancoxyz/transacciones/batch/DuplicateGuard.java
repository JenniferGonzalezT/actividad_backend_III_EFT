package com.bancoxyz.transacciones.batch;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * @StepScope es necesario: sin el, este bean seria un singleton y el Set de claves
 * vistas arrastraria estado de una ejecucion del job a la siguiente dentro del mismo JVM.
 */
@Component
@StepScope
public class DuplicateGuard {

    private final Set<String> vistos = new HashSet<>();

    public boolean esDuplicado(String key) {
        return !vistos.add(key);
    }
}
