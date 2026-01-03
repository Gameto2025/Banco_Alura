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
    public Map<String, Object> estadisticas() {

        List<Prediccion> lista = repository.findAll();

        // Conteo por nivel de riesgo (ALTO/MEDIO/BAJO/MUY BAJO)
        Map<String, Long> conteo = lista.stream()
                .collect(Collectors.groupingBy(
                        p -> nivelRiesgo(p.getScore()),
                        Collectors.counting()
                ));

        long total = lista.size();
        long churns = lista.stream().filter(p -> p.getResultado() == 1).count();

        Map<String, Object> resp = new HashMap<>();
        resp.put("totalClientes", total);
        resp.put("churns", churns);
        resp.put("tasaChurn", String.format("%.1f%%", total == 0 ? 0 : (churns * 100.0 / total)));
        resp.put("scorePromedio", String.format("%.1f%%",
                lista.stream().mapToDouble(Prediccion::getScore).average().orElse(0) * 100));

        // Distribución SIEMPRE con las 4 claves (evita gráficos en blanco)
        Map<String, Integer> dist = new HashMap<>();
        dist.put("ALTO", conteo.getOrDefault("ALTO", 0L).intValue());
        dist.put("MEDIO", conteo.getOrDefault("MEDIO", 0L).intValue());
        dist.put("BAJO", conteo.getOrDefault("BAJO", 0L).intValue());
        dist.put("MUY BAJO", conteo.getOrDefault("MUY BAJO", 0L).intValue());

        resp.put("distribucionRiesgo", dist);
        return resp;
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
    dto.put("resultado", p.getResultado() == 1 ? "Abandona" : "No abandona");
    dto.put("nivelRiesgo", nivelRiesgo(p.getScore()));

    dto.put("fecha", p.getFecha() != null
            ? p.getFecha().toString().split("T")[0]
            : "N/A");

    // 🔥 EXPLICABILIDAD
    dto.put("factores", p.getFactores());
    dto.put("recomendacion", p.getRecomendacion());

    return dto;
}

    // Niveles CONSISTENTES con el servicio y el dashboard
    private String nivelRiesgo(double score) {
        if (score >= 0.75) return "ALTO";
        if (score >= 0.58) return "MEDIO";
        if (score >= 0.30) return "BAJO";
        return "MUY BAJO";
    }
}