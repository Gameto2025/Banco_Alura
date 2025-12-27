# 🏦 Sistema de Predicción de Churn – Banco Alura

![Estado](https://img.shields.io/badge/Estado-Completado-green) ![Versión](https://img.shields.io/badge/Versión-1.0.0-blue)

Este proyecto implementa una solución de **inteligencia artificial** de extremo a extremo orientada a la detección temprana de clientes con alta probabilidad de abandono (**Churn**) en el Banco Alura.

---

## 📁 Información General
* **Versión:** 1.0.0
* **Estado:** 🟢 Completado
* **Dominio:** Analítica Predictiva / Machine Learning

### 🛠️ Tecnologías
* **Frontend:** HTML5, CSS3
* **Backend:** Java (Spring Boot)
* **Modelado:** Python (XGBoost)
* **Interoperabilidad:** PMML

---

## 🚀 Descripción del Proyecto
El Banco Alura enfrenta el desafío de retener clientes en un entorno financiero competitivo. Esta solución transforma el dataset histórico `Banco_Alura.csv` en un modelo predictivo robusto, capaz de estimar el riesgo de abandono de cada cliente en tiempo real.

### ⭐ Características Principales
1.  **Modelo XGBoost:** Implementación de alto rendimiento para clasificación.
2.  **Interoperabilidad PMML:** Exportación del modelo para consumo en Java sin dependencias de Python.
3.  **API REST:** Backend desarrollado con Spring Boot para procesar solicitudes en tiempo real.

---

## 📂 Estructura del Proyecto

### 💻 Backend (`/src/main/java`)
* `ChurnApplication.java`: Punto de entrada de Spring Boot.
* `ChurnService.java`: Lógica para cargar y evaluar el modelo PMML.
* `ChurnController.java`: Endpoints REST para predicciones.

### ⚙️ Recursos y Configuración (`/src/main/resources`)
* `modelo_churn_banco.pmml`: Modelo predictivo entrenado.
* `pom.xml`: Gestión de dependencias Maven.

---

## ⚙️ Instalación y Ejecución Local

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/Gameto2025/Banco_Alura.git](https://github.com/Gameto2025/Banco_Alura.git)
Compilar e instalar:

Bash

mvn clean install
Ejecutar:

Bash

mvn spring-boot:run
📍 La API esta disponible en: http://localhost:8080/dashboard.html

👥 Equipo de Trabajo
Gabriel Mendez Oteiza: Desarrollador Fullstack & Data Scientist.
