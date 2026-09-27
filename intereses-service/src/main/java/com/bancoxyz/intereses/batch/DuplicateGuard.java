package com.bancoxyz.intereses.batch;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@StepScope
public class DuplicateGuard {

    private final Set<String> vistos = new HashSet<>();

    public boolean esDuplicado(String key) {
        return !vistos.add(key);
    }
}
