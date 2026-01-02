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
    public void init() {
        try {
            InputStream is = getClass().getResourceAsStream("/modelo_churn_banco.pmml");
            this.evaluator = new LoadingModelEvaluatorBuilder().load(is).build();
            this.evaluator.verify();
            modeloCargado = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> predecir(Map<String, Object> datosCliente) {

        try {

            int edad = obtenerEntero(datosCliente, "edad", 0);
            int productos = obtenerEntero(datosCliente, "productos", 1);
            int activo = obtenerEntero(datosCliente, "activo", 1);
            int pais = obtenerEntero(datosCliente, "pais", 0);

            double inactivo4070 = (edad >= 40 && edad <= 70 && activo == 0) ? 1 : 0;
            double productsRiskFlag = (productos >= 3) ? 1 : 0;
            double countryRiskFlag = (pais == 2) ? 1 : 0;

            List<String> factoresClave = new ArrayList<>();
            if (inactivo4070 == 1) factoresClave.add("Cuenta inactiva en edad crítica");
            if (productsRiskFlag == 1) factoresClave.add("Muchos productos (>=3)");
            if (pais == 2) factoresClave.add("Cliente de Alemania");
            if (productos == 1) factoresClave.add("Baja vinculación");
            if (factoresClave.isEmpty()) factoresClave.add("Perfil estable");

            Map<String, Object> inputs = new HashMap<>();
            inputs.put("Inactivo_40_70", inactivo4070);
            inputs.put("NumOfProducts", (double) productos);
            inputs.put("Products_Risk_Flag", productsRiskFlag);
            inputs.put("Country_Risk_Flag", countryRiskFlag);

            Map<FieldName, FieldValue> arguments = new HashMap<>();
            for (InputField inputField : evaluator.getInputFields()) {
                arguments.put(inputField.getName(), inputField.prepare(inputs.getOrDefault(inputField.getName().getValue(), 0)));
            }

            Map<FieldName, ?> results = evaluator.evaluate(arguments);
            double probabilidad = extraerProbabilidad(results);

            String nivelRiesgo = determinarNivelRiesgo(probabilidad);

            String factoresTxt = factoresClave.stream().limit(3).collect(Collectors.joining(" | "));

            String recomendacion =
                    nivelRiesgo.equals("ALTO") ? "Contactar de inmediato" :
                    nivelRiesgo.equals("MEDIO") ? "Enviar incentivo personalizado" :
                    nivelRiesgo.equals("BAJO") ? "Seguimiento normal" :
                            "Cliente fidelizado";

            Prediccion p = new Prediccion();
            p.setEdad(edad);
            p.setScore(probabilidad);
            p.setResultado(probabilidad >= 0.58 ? 1 : 0);
            p.setPais(pais);
            p.setProductos(productos);
            p.setFactores(factoresTxt);
            p.setRecomendacion(recomendacion);
            repository.save(p);

            Map<String, Object> resp = new HashMap<>();
            resp.put("score", probabilidad);
            resp.put("nivelRiesgo", nivelRiesgo);
            resp.put("resultado", probabilidad >= 0.58 ? "Churn" : "No Churn");
            resp.put("factoresClave", factoresTxt);
            resp.put("recomendacion", recomendacion);
            resp.put("colorRiesgo", getColorPorRiesgo(nivelRiesgo));

            return resp;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error en predicción");
        }
    }

    private double extraerProbabilidad(Map<FieldName, ?> results) {
        for (Object value : results.values()) {
            if (value instanceof ProbabilityDistribution<?>) {
                ProbabilityDistribution<?> pd = (ProbabilityDistribution<?>) value;
                return pd.getProbability(pd.getCategories().iterator().next());
            }
        }
        return 0;
    }

    private String determinarNivelRiesgo(double p) {
        if (p >= 0.75) return "ALTO";
        if (p >= 0.58) return "MEDIO";
        if (p >= 0.30) return "BAJO";
        return "MUY BAJO";
    }

    private int obtenerEntero(Map<String, Object> map, String k, int d) {
        try { return Integer.parseInt(map.get(k).toString()); } catch(Exception e){ return d; }
    }

    private String getColorPorRiesgo(String r) {
        return r.equals("ALTO") ? "#dc3545" : r.equals("MEDIO") ? "#fd7e14" : r.equals("BAJO") ? "#20c997" : "#0d6efd";
    }
}