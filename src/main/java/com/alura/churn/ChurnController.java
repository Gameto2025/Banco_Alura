package com.alura.churn;

import com.alura.churn.model.Prediccion;
import com.alura.churn.repository.PrediccionRepository;
import com.alura.churn.ChurnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller; // Importante para las vistas
import org.springframework.web.servlet.ModelAndView; // Para manejar páginas

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/churn")
@CrossOrigin(origins = "*")
public class ChurnController {

    @Autowired
    private ChurnService churnService;

    @Autowired
    private PrediccionRepository repository;

    // ==================== RUTAS DE NAVEGACIÓN (CORRECCIÓN 404) ====================
    
    // Esto permite que al ir a la raíz se cargue el index.html de static
    @GetMapping("/")
    public ModelAndView home() {
        return new ModelAndView("redirect:/index.html");
    }

    // ==================== PREDICCIÓN (MÉTODO EXISTENTE) ====================
    @PostMapping("/predict")
    public Map<String, Object> predict(@RequestBody Map<String, Object> datos) {
        return churnService.predecir(datos);
    }

    // ==================== ENDPOINTS PARA DASHBOARD (DATOS REALES) ====================

    @GetMapping("/estadisticas")
    public Map<String, Object> getEstadisticasCompletas() {
        List<Prediccion> todas = repository.findAll();
        long total = todas.size();
        
        long churns = todas.stream().filter(p -> p.getResultado() == 1).count();
        long noChurns = total - churns;
        
        double scorePromedio = todas.stream()
                .mapToDouble(Prediccion::getScore)
                .average()
                .orElse(0.0);

        Map<String, Long> distribucionRiesgo = todas.stream()
                .collect(Collectors.groupingBy(
                        p -> getNivelRiesgo(p.getScore()),
                        Collectors.counting()
                ));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClientes", total);
        stats.put("churns", churns);
        stats.put("noChurns", noChurns);
        stats.put("tasaChurn", total > 0 ? String.format("%.1f%%", ((double) churns / total) * 100) : "0%");
        stats.put("scorePromedio", String.format("%.1f%%", scorePromedio * 100));
        stats.put("distribucionRiesgo", distribucionRiesgo);
        
        return stats;
    }

    @GetMapping("/top-riesgo")
    public List<Map<String, Object>> getTopClientesRiesgo() {
        return repository.findAll().stream()
                .sorted((p1, p2) -> Double.compare(p2.getScore(), p1.getScore()))
                .limit(10)
                .map(this::mapearPrediccion)
                .collect(Collectors.toList());
    }

    @GetMapping("/clientes")
    public List<Map<String, Object>> getAllClientes() {
        return repository.findAll().stream()
                .sorted((p1, p2) -> {
                    if (p1.getFecha() != null && p2.getFecha() != null) {
                        return p2.getFecha().compareTo(p1.getFecha());
                    }
                    return 0;
                })
                .map(this::mapearPrediccion)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/reset")
    public Map<String, Object> resetearDatos() {
        long count = repository.count();
        repository.deleteAll();
        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Base de datos H2 limpiada con éxito");
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
        dto.put("fecha", p.getFecha() != null ? p.getFecha().toString().split("T")[0] : "N/A");
        dto.put("nivelRiesgo", getNivelRiesgo(p.getScore()));
        dto.put("color", getColorRiesgo(p.getScore()));
        return dto;
    }

    private String getNivelRiesgo(double score) {
        if (score >= 0.75) return "ALTO";
        if (score >= 0.50) return "MEDIO";
        return "BAJO";
    }

    private String getColorRiesgo(double score) {
        if (score >= 0.75) return "#dc3545"; 
        if (score >= 0.50) return "#ffc107"; 
        return "#28a745"; 
    }
}