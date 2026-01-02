package com.alura.churn;

import com.alura.churn.model.Prediccion;
import com.alura.churn.repository.PrediccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/churn")
@CrossOrigin(origins = "*")
public class ChurnController {

    @Autowired
    private ChurnService churnService;

    @Autowired
    private PrediccionRepository repository;

    // ==================== PREDICCIÓN ====================
    @PostMapping("/predict")
    public Map<String, Object> predict(@RequestBody Map<String, Object> datos) {
        return churnService.predecir(datos);
    }

    // ==================== ESTADÍSTICAS DASHBOARD ====================
    @GetMapping("/estadisticas")
    public Map<String, Object> getEstadisticas() {

        List<Prediccion> todas = repository.findAll();
        long total = todas.size();
        long churns = todas.stream()
                .filter(p -> p.getResultado() == 1)
                .count();

        double scorePromedio = todas.stream()
                .mapToDouble(Prediccion::getScore)
                .average()
                .orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClientes", total);
        stats.put("churns", churns);
        stats.put("noChurns", total - churns);
        stats.put("tasaChurn", total > 0
                ? String.format("%.1f%%", ((double) churns / total) * 100)
                : "0%");
        stats.put("scorePromedio", String.format("%.1f%%", scorePromedio * 100));

        return stats;
    }

    // ==================== CLIENTES ====================
    @GetMapping("/clientes")
    public List<Map<String, Object>> getClientes() {
        return repository.findAll()
                .stream()
                .sorted(Comparator.comparing(Prediccion::getFecha,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapearPrediccion)
                .collect(Collectors.toList());
    }

    // ==================== TOP RIESGO ====================
    @GetMapping("/top-riesgo")
    public List<Map<String, Object>> getTopRiesgo() {
        return repository.findAll()
                .stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(10)
                .map(this::mapearPrediccion)
                .collect(Collectors.toList());
    }

    // ==================== RESET BASE H2 ====================
    @DeleteMapping("/reset")
    public Map<String, Object> reset() {
        long count = repository.count();
        repository.deleteAll();

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Base H2 limpiada correctamente");
        response.put("registros_eliminados", count);

        return response;
    }

    // ==================== MÉTODOS AUXILIARES ====================
    private Map<String, Object> mapearPrediccion(Prediccion p) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", p.getId());
        dto.put("edad", p.getEdad());
        dto.put("score", p.getScore());
        dto.put("probabilidad", String.format("%.1f%%", p.getScore() * 100));
        dto.put("resultado", p.getResultado() == 1 ? "Churn" : "No Churn");
        dto.put("fecha", p.getFecha() != null
                ? p.getFecha().toString().split("T")[0]
                : "N/A");
        dto.put("nivelRiesgo", nivelRiesgo(p.getScore()));
        return dto;
    }

    private String nivelRiesgo(double score) {
        if (score >= 0.75) return "ALTO";
        if (score >= 0.50) return "MEDIO";
        return "BAJO";
    }
}