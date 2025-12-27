package com.alura.churn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.alura.churn.repository.PrediccionRepository;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/churn")
@CrossOrigin(origins = "*") // <--- ESTA LÍNEA ES LA CLAVE

public class ChurnController {

 @Autowired
    private ChurnService churnService; 

    @Autowired
    private PrediccionRepository repository;

    @PostMapping("/predict")
    public Map<String, Object> predict(@RequestBody Map<String, Object> datos) {
        // Ahora ya no marcará error aquí
        return churnService.predecir(datos);
    }

    @GetMapping("/stats")
    public Map<String, Object> obtenerEstadisticas() {
        long total = repository.count();
        long churns = repository.countByResultado(1);
        double tasaChurn = total > 0 ? (double) churns / total : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_evaluados", total);
        stats.put("tasa_churn", tasaChurn);
        return stats;
    }
}