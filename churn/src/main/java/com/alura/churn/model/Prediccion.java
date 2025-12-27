package com.alura.churn.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predicciones")
public class Prediccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer edad;
    private Double score;
    private Integer resultado;
    
    // --- 1. COLOCA LOS NUEVOS CAMPOS AQUÍ (Debajo de los anteriores) ---
    private Integer pais;      //
    private Integer productos; //
    
    private LocalDateTime fecha = LocalDateTime.now();

    public Prediccion() {}

    // Constructor actualizado
    public Prediccion(Integer edad, Double score, Integer resultado, Integer pais, Integer productos) {
        this.edad = edad;
        this.score = score;
        this.resultado = resultado;
        this.pais = pais;
        this.productos = productos;
    }

    // --- 2. COLOCA LOS GETTERS Y SETTERS AQUÍ ---
    public Long getId() { return id; }

    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Integer getResultado() { return resultado; }
    public void setResultado(Integer resultado) { this.resultado = resultado; }

    // Métodos que VS Code marcaba como faltantes:
    public void setPais(Integer pais) { this.pais = pais; }
    public Integer getPais() { return pais; }

    public void setProductos(Integer productos) { this.productos = productos; }
    public Integer getProductos() { return productos; }

    public LocalDateTime getFecha() { return fecha; }
}