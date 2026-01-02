package com.alura.churn;

import com.alura.churn.model.Prediccion;
import com.alura.churn.repository.PrediccionRepository;
import org.jpmml.evaluator.*;
import org.dmg.pmml.FieldName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChurnService {

    @Autowired
    private PrediccionRepository repository;

    private Evaluator evaluator;
    private boolean modeloCargado = false;

    @PostConstruct
    public void init() throws Exception {
        try {
            System.out.println("\n=== INICIANDO CARGA DEL MODELO PMML ===");
            InputStream is = getClass().getResourceAsStream("/modelo_churn_banco.pmml");
            if (is == null) {
                System.err.println("ERROR: No se encontró modelo_churn_banco.pmml");
                return;
            }
            this.evaluator = new LoadingModelEvaluatorBuilder().load(is).build();
            this.evaluator.verify();
            modeloCargado = true;
            System.out.println("=== MODELO LISTO PARA USAR ===\n");
        } catch (Exception e) {
            e.printStackTrace();
            modeloCargado = false;
        }
    }

    public Map<String, Object> predecir(Map<String, Object> datosCliente) {
        if (!modeloCargado) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Modelo no disponible");
            return error;
        }

        try {
            // 1. Extraer datos base (Aseguramos que coincidan con los nombres del JSON enviado desde el form)
            int edad = obtenerEntero(datosCliente, "edad", 0);
            int productos = obtenerEntero(datosCliente, "productos", 1); // Valor por defecto 1
            int activo = obtenerEntero(datosCliente, "activo", 1);
            int pais = obtenerEntero(datosCliente, "pais", 0);

            // 2. Calcular variables de entrada para el modelo IA
            double ageRisk = (edad >= 40 && edad <= 70) ? 1.0 : 0.0;
            double inactivo4070 = (edad >= 40 && edad <= 70 && activo == 0) ? 1.0 : 0.0;
            double productsRiskFlag = (productos >= 3) ? 1.0 : 0.0;
            double countryRiskFlag = (pais == 2) ? 1.0 : 0.0; // Alemania suele ser index 2 en modelos de Alura

            // 3. --- LÓGICA DE EXPLICABILIDAD ---
            List<String> factoresClave = new ArrayList<>();
            if (inactivo4070 == 1.0) factoresClave.add("Inactividad en edad crítica");
            if (productsRiskFlag == 1.0) factoresClave.add("Exceso de productos (>=3)");
            if (pais == 2) factoresClave.add("Riesgo por región (Alemania)");
            if (productos == 1) factoresClave.add("Baja vinculación (1 producto)");
            
            if (factoresClave.isEmpty()) factoresClave.add("Perfil estable");

            // 4. Preparar inputs para PMML
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("Age_Risk", ageRisk);
            inputs.put("NumOfProducts", (double) productos);
            inputs.put("Inactivo_40_70", inactivo4070);
            inputs.put("Products_Risk_Flag", productsRiskFlag);
            inputs.put("Country_Risk_Flag", countryRiskFlag);

            Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
            for (InputField inputField : evaluator.getInputFields()) {
                FieldName fieldName = inputField.getName();
                Object value = inputs.getOrDefault(fieldName.getValue(), 0.0);
                arguments.put(fieldName, inputField.prepare(value));
            }

            // 5. Ejecutar modelo y extraer probabilidad
            Map<FieldName, ?> results = evaluator.evaluate(arguments);
            double probabilidad = extraerProbabilidad(results);

            // 6. Interpretar y GUARDAR (CORRECCIÓN: Guardar país y productos)
            boolean abandona = probabilidad >= 0.58;
            String nivelRiesgo = determinarNivelRiesgo(probabilidad);

            Prediccion nuevaPrediccion = new Prediccion();
            nuevaPrediccion.setEdad(edad);
            nuevaPrediccion.setScore(probabilidad);
            nuevaPrediccion.setResultado(abandona ? 1 : 0);
            nuevaPrediccion.setPais(pais);
            nuevaPrediccion.setProductos(productos);

            // ======= FACTORES Y RECOMENDACIÓN (NUEVO) =======
            String factoresTxt = factoresClave.stream()
                    .limit(3)
                    .collect(Collectors.joining(" | "));

            String recomendacion =
                    nivelRiesgo.equals("ALTO") ? "📞 Contactar de inmediato"
                : nivelRiesgo.equals("MEDIO") ? "📧 Enviar incentivo"
                : nivelRiesgo.equals("BAJO") ? "🙂 Seguimiento normal"
                : "⭐ Cliente fidelizado";

            nuevaPrediccion.setFactores(factoresTxt);
            nuevaPrediccion.setRecomendacion(recomendacion);

            // AHORA SÍ SE GUARDA COMPLETA
            repository.save(nuevaPrediccion);

            // 7. Respuesta JSON (Sincronizada con Dashboard)
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("score", probabilidad);
            respuesta.put("probabilidad", String.format("%.1f%%", probabilidad * 100));
            respuesta.put("nivelRiesgo", nivelRiesgo);
            respuesta.put("resultado", abandona ? "Churn" : "No Churn");
            respuesta.put("factoresClave", factoresTxt);
            respuesta.put("recomendacion", recomendacion);
            respuesta.put("colorRiesgo", getColorPorRiesgo(nivelRiesgo));

            return respuesta;

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private double extraerProbabilidad(Map<FieldName, ?> results) {
        for (Object value : results.values()) {
            if (value instanceof ProbabilityDistribution) {
                ProbabilityDistribution<?> pd = (ProbabilityDistribution<?>) value;
                for (Object cat : pd.getCategories()) {
                    String s = cat.toString();
                    if (s.equals("1") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("churn")) {
                        return pd.getProbability(cat);
                    }
                }
                return pd.getProbability(new ArrayList<>(pd.getCategories()).get(pd.getCategories().size()-1));
            }
        }
        return 0.0;
    }

    private String determinarNivelRiesgo(double prob) {
        if (prob >= 0.75) return "ALTO";
        if (prob >= 0.58) return "MEDIO";
        if (prob >= 0.30) return "BAJO";
        return "MUY BAJO";
    }

    private int obtenerEntero(Map<String, Object> map, String key, int defaultValue) {
        try {
            Object v = map.get(key);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) return Integer.parseInt(v.toString());
        } catch (Exception e) {}
        return defaultValue;
    }

    private String getColorPorRiesgo(String nivelRiesgo) {
        switch(nivelRiesgo) {
            case "ALTO": return "#dc3545";
            case "MEDIO": return "#ffc107";
            case "BAJO": return "#28a745";
            default: return "#17a2b8";
        }
    }
}