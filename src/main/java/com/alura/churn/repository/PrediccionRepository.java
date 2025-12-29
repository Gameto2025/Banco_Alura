package com.alura.churn.repository;

import com.alura.churn.model.Prediccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrediccionRepository extends JpaRepository<Prediccion, Long> {
    // Esto contará cuántos clientes tienen riesgo de fuga
    long countByResultado(Integer resultado);
}