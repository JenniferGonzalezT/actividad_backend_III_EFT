package com.bancoxyz.cuentas.repository;

import com.bancoxyz.cuentas.entity.CuentaAnual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaAnualRepository extends JpaRepository<CuentaAnual, Long> {

    List<CuentaAnual> findByCuentaId(Long cuentaId);
}
