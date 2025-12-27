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
            
            // Buscar el archivo PMML
            InputStream is = getClass().getResourceAsStream("/modelo_churn_banco.pmml");
            if (is == null) {
                System.err.println("ERROR: No se encontró modelo_churn_banco.pmml en classpath");
                System.err.println("Asegúrate de que el archivo está en: src/main/resources/");
                return;
            }
            
            System.out.println("✓ Archivo PMML encontrado");
            
            // Cargar modelo
            this.evaluator = new LoadingModelEvaluatorBuilder()
                .load(is)
                .build();
            
            this.evaluator.verify();
            System.out.println("✓ Modelo verificado correctamente");
            
            // Información del modelo (SOLO métodos que existen)
            System.out.println("\n=== INFORMACIÓN DEL MODELO ===");
            
            // 1. Input Fields
            System.out.println("1. VARIABLES DE ENTRADA:");
            List<InputField> inputFields = evaluator.getInputFields();
            if (inputFields.isEmpty()) {
                System.out.println("   ¡NO HAY VARIABLES DE ENTRADA!");
            } else {
                int i = 1;
                for (InputField field : inputFields) {
                    System.out.println("   " + i + ". " + field.getName().getValue());
                    i++;
                }
            }
            
            // 2. Target Fields
            System.out.println("\n2. VARIABLE OBJETIVO:");
            List<TargetField> targetFields = evaluator.getTargetFields();
            if (targetFields.isEmpty()) {
                System.out.println("   ¡NO HAY VARIABLE OBJETIVO!");
            } else {
                for (TargetField field : targetFields) {
                    System.out.println("   - " + field.getName().getValue());
                }
            }
            
            // 3. Output Fields
            System.out.println("\n3. VARIABLES DE SALIDA:");
            List<OutputField> outputFields = evaluator.getOutputFields();
            if (outputFields.isEmpty()) {
                System.out.println("   No hay variables de salida definidas");
            } else {
                for (OutputField field : outputFields) {
                    System.out.println("   - " + field.getName().getValue());
                }
            }
            
            modeloCargado = true;
            System.out.println("\n=== MODELO LISTO PARA USAR ===\n");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR al cargar el modelo:");
            e.printStackTrace();
            modeloCargado = false;
        }
    }

    public Map<String, Object> predecir(Map<String, Object> datosCliente) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("NUEVA PREDICCIÓN SOLICITADA");
        System.out.println("=".repeat(50));
        
        if (!modeloCargado) {
            System.err.println("ERROR: Modelo no cargado");
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Modelo no disponible");
            return error;
        }
        
        try {
            // 1. Mostrar datos recibidos
            System.out.println("Datos recibidos: " + datosCliente);

            // 2. Extraer datos
            int edad = obtenerEntero(datosCliente, "edad", 0);
            int productos = obtenerEntero(datosCliente, "productos", 0);
            int activo = obtenerEntero(datosCliente, "activo", 1);
            int pais = obtenerEntero(datosCliente, "pais", 0);
            
            System.out.println("Edad: " + edad + ", Productos: " + productos + 
                             ", Activo: " + activo + ", País: " + pais);

            // 3. Calcular variables (IGUAL que en Python)
            double ageRisk = (edad >= 40 && edad <= 70) ? 1.0 : 0.0;
            double inactivo4070 = (edad >= 40 && edad <= 70 && activo == 0) ? 1.0 : 0.0;
            double productsRiskFlag = (productos >= 3) ? 1.0 : 0.0;
            double countryRiskFlag = (pais == 1) ? 1.0 : 0.0; // Germany=1

            System.out.println("Variables calculadas:");
            System.out.println("  Age_Risk: " + ageRisk);
            System.out.println("  Inactivo_40_70: " + inactivo4070);
            System.out.println("  Products_Risk_Flag: " + productsRiskFlag);
            System.out.println("  Country_Risk_Flag: " + countryRiskFlag);

            // 4. Preparar inputs
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("Age_Risk", ageRisk);
            inputs.put("NumOfProducts", (double) productos);
            inputs.put("Inactivo_40_70", inactivo4070);
            inputs.put("Products_Risk_Flag", productsRiskFlag);
            inputs.put("Country_Risk_Flag", countryRiskFlag);

            // 5. Verificar que el modelo espera estas variables
            System.out.println("Variables que espera el modelo:");
            Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
            
            for (InputField inputField : evaluator.getInputFields()) {
                FieldName fieldName = inputField.getName();
                String fieldNameStr = fieldName.getValue();
                
                Object value = inputs.get(fieldNameStr);
                if (value == null) {
                    System.err.println("  ⚠️  Falta: " + fieldNameStr);
                    value = 0.0;
                } else {
                    System.out.println("  ✓ " + fieldNameStr + ": " + value);
                }
                
                FieldValue fieldValue = inputField.prepare(value);
                arguments.put(fieldName, fieldValue);
            }

            // 6. Ejecutar modelo
            System.out.println("Ejecutando modelo...");
            Map<FieldName, ?> results = evaluator.evaluate(arguments);
            
            // 7. Mostrar resultados
            System.out.println("Resultados completos:");
            for (Map.Entry<FieldName, ?> entry : results.entrySet()) {
                System.out.println("  " + entry.getKey().getValue() + ": " + entry.getValue());
            }

            // 8. Extraer probabilidad
            double probabilidad = 0.0;
            List<TargetField> targetFields = evaluator.getTargetFields();
            
            if (!targetFields.isEmpty()) {
                FieldName targetName = targetFields.get(0).getName();
                Object targetValue = results.get(targetName);
                
                System.out.println("Valor objetivo: " + targetValue);
                
                if (targetValue instanceof ProbabilityDistribution) {
                    ProbabilityDistribution<?> pd = (ProbabilityDistribution<?>) targetValue;
                    
                    System.out.println("Categorías disponibles:");
                    for (Object category : pd.getCategories()) {
                        double prob = pd.getProbability(category);
                        System.out.println("  " + category + " = " + prob);
                    }
                    
                    // Buscar probabilidad de churn
                    for (Object category : pd.getCategories()) {
                        String catStr = category.toString();
                        if (catStr.equals("1") || catStr.equals("True") || catStr.equals("Churn")) {
                            probabilidad = pd.getProbability(category);
                            System.out.println("Probabilidad encontrada en categoría: " + catStr);
                            break;
                        }
                    }
                    
                    // Si no encontró, tomar la última
                    if (probabilidad == 0.0 && !pd.getCategories().isEmpty()) {
                        List<Object> categories = new ArrayList<>(pd.getCategories());
                        Object ultimaCat = categories.get(categories.size() - 1);
                        probabilidad = pd.getProbability(ultimaCat);
                        System.out.println("Usando última categoría: " + ultimaCat);
                    }
                    
                } else if (targetValue instanceof Number) {
                    probabilidad = ((Number) targetValue).doubleValue();
                }
            }

            System.out.println("Probabilidad final: " + (probabilidad * 100) + "%");

            // 9. Interpretar resultado con mensajes específicos
            double umbralOptimo = 0.58;
            boolean abandona = probabilidad >= umbralOptimo;

            String nivelRiesgo;
            String mensajeDetallado;

            if (probabilidad >= 0.75) {
                nivelRiesgo = "ALTO";
                mensajeDetallado = String.format(
                    "🔴 RIESGO ALTO (%.1f%%) - El cliente probablemente abandonará. " +
                    "Se recomienda contacto urgente y oferta de retención.",
                    probabilidad * 100
                );
            } else if (probabilidad >= umbralOptimo) {
                nivelRiesgo = "MEDIO";
                mensajeDetallado = String.format(
                    "🟡 RIESGO MEDIO (%.1f%%) - El cliente podría abandonar. " +
                    "Se sugiere seguimiento en los próximos días.",
                    probabilidad * 100
                );
            } else if (probabilidad >= 0.30) {
                nivelRiesgo = "BAJO";
                mensajeDetallado = String.format(
                    "🟢 RIESGO BAJO (%.1f%%) - El cliente probablemente se quedará. " +
                    "Mantener servicio actual.",
                    probabilidad * 100
                );
            } else {
                nivelRiesgo = "MUY BAJO";
                mensajeDetallado = String.format(
                    "✅ RIESGO MUY BAJO (%.1f%%) - Cliente fiel y estable. " +
                    "Excelente retención.",
                    probabilidad * 100
                );
            }

            // 10. Guardar en BD
            Prediccion nuevaPrediccion = new Prediccion();
            nuevaPrediccion.setEdad(edad);
            nuevaPrediccion.setScore(probabilidad);
            nuevaPrediccion.setResultado(abandona ? 1 : 0);
            repository.save(nuevaPrediccion);

            // 11. Preparar respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("score", probabilidad);
            respuesta.put("probabilidad", String.format("%.2f%%", probabilidad * 100));
            respuesta.put("abandona", abandona);
            respuesta.put("nivelRiesgo", nivelRiesgo);
            respuesta.put("prediccion", abandona ? "Churn" : "No Churn");
            respuesta.put("mensaje", mensajeDetallado);

            // Resumen para la tarjeta/display
            String resumen = String.format(
                "%s | Probabilidad: %.1f%% | %s",
                getEmojiPorRiesgo(nivelRiesgo),
                probabilidad * 100,
                abandona ? "Requerirá atención" : "Sin acción requerida"
            );
            respuesta.put("resumen", resumen);
            
            // Color para el frontend
            respuesta.put("colorRiesgo", getColorPorRiesgo(nivelRiesgo));
            
            System.out.println("Respuesta: " + respuesta);
            System.out.println("=".repeat(50) + "\n");
            
            return respuesta;

        } catch (Exception e) {
            System.err.println("ERROR en predicción:");
            e.printStackTrace();
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }
    
    private int obtenerEntero(Map<String, Object> map, String key, int defaultValue) {
        try {
            if (map.containsKey(key)) {
                Object value = map.get(key);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                } else if (value != null) {
                    return Integer.parseInt(value.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Error parseando " + key);
        }
        return defaultValue;
    }
    
    private String getEmojiPorRiesgo(String nivelRiesgo) {
        switch(nivelRiesgo.toUpperCase()) {
            case "ALTO": return "🔴";
            case "MEDIO": return "🟡";
            case "BAJO": return "🟢";
            case "MUY BAJO": return "✅";
            default: return "📊";
        }
    }

    private String getColorPorRiesgo(String nivelRiesgo) {
        switch(nivelRiesgo.toUpperCase()) {
            case "ALTO": return "#dc3545"; // Rojo
            case "MEDIO": return "#ffc107"; // Amarillo
            case "BAJO": return "#28a745"; // Verde
            case "MUY BAJO": return "#17a2b8"; // Azul
            default: return "#6c757d"; // Gris
        }
    }
}