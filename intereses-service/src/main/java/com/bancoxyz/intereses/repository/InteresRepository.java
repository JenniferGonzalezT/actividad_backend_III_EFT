package com.bancoxyz.intereses.repository;

import com.bancoxyz.intereses.entity.Interes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InteresRepository extends JpaRepository<Interes, Long> {

    Optional<Interes> findFirstByCuentaId(Long cuentaId);
}
