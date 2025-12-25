🏦 **Sistema de Predicción de Churn – Banco Alura**

Este proyecto implementa una solución de inteligencia artificial de extremo a extremo orientada a la detección temprana de clientes con alta probabilidad de abandono (Churn) en el Banco Alura, específicamente en sus operaciones de España, Francia y Alemania.

El objetivo principal es apoyar la toma de decisiones estratégicas, permitiendo anticipar la evasión de clientes y diseñar acciones de retención basadas en datos.


📁 **Información General**

Versión: 1.0.0

Estado: 🟢 Completado

Dominio: Analítica Predictiva / Machine Learning

**Tecnologías:**

Frontend: HTML5, CSS3

Backend: Java (Spring Boot)

Modelado: Python (XGBoost)

Interoperabilidad: PMML


🚀 **Descripción del Proyecto**

El Banco Alura enfrenta el desafío de retener clientes en un entorno financiero altamente competitivo. Esta solución transforma el dataset histórico Banco_Alura.csv en un modelo predictivo robusto, capaz de estimar el riesgo de abandono de cada cliente en tiempo real.

La arquitectura permite integrar modelos entrenados en Python dentro de un ecosistema Java, garantizando escalabilidad, portabilidad y mantenibilidad.


⭐ **Características Principales**

- Modelo de Machine Learning Avanzado
Implementación del algoritmo XGBoost, reconocido por su alto rendimiento en problemas de clasificación y churn.

- Interoperabilidad mediante PMML
Exportación del modelo a formato PMML, permitiendo su consumo en aplicaciones Java sin dependencias de Python en producción.

- API REST Escalable
Backend desarrollado con Spring Boot, preparado para procesar solicitudes de predicción en tiempo real.

- Interfaz Web Intuitiva
Dashboard web orientado a usuarios no técnicos para la consulta de resultados.


📊 **Ingeniería de Características (Features)**

El modelo utiliza variables seleccionadas y transformadas estratégicamente para maximizar su poder predictivo:

- Age_Risk
Índice de riesgo basado en el segmento etario del cliente.

- NumOfProducts
Cantidad de productos financieros contratados.


- Inactivo_40_70
Indicador de inactividad en clientes de mediana edad.

- Products_Risk_Flag
Alerta sobre combinaciones de productos con comportamiento inestable.

- Country_Risk_Flag
Factor de riesgo asociado al país de residencia del cliente.


🛠 **Arquitectura y Estructura del Proyecto**

📂 Backend – Lógica de Aplicación (/src/main/java)

 - ChurnApplication.java
Punto de entrada de la aplicación Spring Boot.

- ChurnService.java
Servicio encargado de cargar y evaluar el modelo PMML.

- ChurnController.java
Controlador REST para la gestión de solicitudes de predicción.

📂 Recursos y Configuración (/src/main/resources)

- modelo_churn_banco.pmml
Modelo predictivo entrenado con XGBoost.

- application.properties
Configuración de la aplicación.

- pom.xml
Gestión de dependencias mediante Maven.

📂 Frontend – Interfaz de Usuario (/static)

- index.html
Interfaz web para el consumo de predicciones.


🔗 **Enlaces del Proyecto**

Recurso	Enlace

📂 Repositorio GitHub	Ver código fuente

📖 Documentación Técnica	Leer Wiki

🚀 Demo en Producción	Ir al sitio

📋 Gestión del Proyecto	Trello / Jira
⚙️ Instalación y Ejecución Local

1️⃣ Clonar el repositorio
git clone https://github.com/Gameto2025/Banco-alura-churn.git

2️⃣ Compilar e instalar dependencias
mvn clean install

3️⃣ Ejecutar la aplicación
mvn spring-boot:run


📍 La API estará disponible en:  http://localhost:8081

👥 **Equipo de Trabajo**

[Gabriel Mendez oteiza]
Desarrollador Fullstack & Data Scientist

[Equipo backEnd]
Especialista en UI/UX y QA
