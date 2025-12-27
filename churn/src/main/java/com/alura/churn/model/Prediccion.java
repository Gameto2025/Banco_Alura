package com.alura.churn.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predicciones")
public class Prediccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer edad;      // Añadimos este campo para guardar la edad
    private Double score;
    private Integer resultado; 
    private LocalDateTime fecha = LocalDateTime.now();

    public Prediccion() {}

    // Constructor actualizado
    public Prediccion(Integer edad, Double score, Integer resultado) {
        this.edad = edad;
        this.score = score;
        this.resultado = resultado;
    }

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }

    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Integer getResultado() { return resultado; }
    public void setResultado(Integer resultado) { this.resultado = resultado; }

    public LocalDateTime getFecha() { return fecha; }
}