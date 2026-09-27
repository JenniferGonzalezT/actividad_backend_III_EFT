package com.bancoxyz.transacciones.repository;

import com.bancoxyz.transacciones.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    @Query("select coalesce(max(t.id), 0) from Transaccion t")
    long maxId();

    List<Transaccion> findByEstado(String estado);
}
