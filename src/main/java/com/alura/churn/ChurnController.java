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
        resp.put("tasaChurn", String.format("%.1f%%",
                total == 0 ? 0 : (churns * 100.0 / total)));
        resp.put("scorePromedio", String.format("%.1f%%",
                lista.stream().mapToDouble(Prediccion::getScore).average().orElse(0) * 100));

        Map<String, Integer> dist = new HashMap<>();
        dist.put("ALTO", conteo.getOrDefault("ALTO", 0L).intValue());
        dist.put("MEDIO", conteo.getOrDefault("MEDIO", 0L).intValue());
        dist.put("BAJO", conteo.getOrDefault("BAJO", 0L).intValue());
        dist.put("MUY BAJO", conteo.getOrDefault("MUY BAJO", 0L).intValue());

        resp.put("distribucionRiesgo", dist);
        return resp;
    }

    // ==================== TODOS LOS CLIENTES ====================
    @GetMapping("/clientes")
    public List<Map<String, Object>> getClientes() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(
                        Prediccion::getFecha,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(this::mapearPrediccion)
                .collect(Collectors.toList());
    }

    // ==================== CLIENTES CRÍTICOS ====================
    @GetMapping("/criticos")
    public List<Map<String, Object>> getCriticos() {
        return repository.findAll().stream()
                .filter(p -> p.getResultado() == 1 || p.getScore() >= 0.80)
                .sorted(Comparator.comparingDouble(Prediccion::getScore).reversed())
                .map(p -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", p.getId());
                    dto.put("edad", p.getEdad());
                    dto.put("pais", paisTexto(p.getPais()));
                    dto.put("score", String.format("%.2f%%", p.getScore() * 100));
                    dto.put("factores", p.getFactores());
                    dto.put("recomendacion", p.getRecomendacion());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ==================== RESET BASE H2 ====================
    @DeleteMapping("/reset")
    public Map<String, Object> reset() {
        long count = repository.count();
        repository.deleteAll();
        return Map.of("mensaje", "Base H2 limpiada", "registros_eliminados", count);
    }

    // ==================== MAPEO GENERAL ====================
    private Map<String, Object> mapearPrediccion(Prediccion p) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", p.getId());
        dto.put("edad", p.getEdad());
        dto.put("score", String.format("%.2f%%", p.getScore() * 100));
        dto.put("nivelRiesgo", nivelRiesgo(p.getScore()));
        dto.put("resultado", p.getResultado() == 1 ? "Abandona" : "No abandona");
        dto.put("fecha", p.getFecha() != null
                ? p.getFecha().toString().split("T")[0]
                : "N/A");
        dto.put("factores", p.getFactores());
        dto.put("recomendacion", p.getRecomendacion());
        return dto;
    }

    private String nivelRiesgo(double score) {
        if (score >= 0.75) return "ALTO";
        if (score >= 0.58) return "MEDIO";
        if (score >= 0.30) return "BAJO";
        return "MUY BAJO";
    }

    private String paisTexto(Integer p) {
        if (p == null) return "N/A";
        return p == 2 ? "Germany" : p == 1 ? "Spain" : "France";
    }
}