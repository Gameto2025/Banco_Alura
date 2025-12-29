# Etapa 1: Construcción (Build)
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Runtime)
FROM openjdk:17-jdk-slim
# Copia el archivo .jar generado en la etapa anterior
COPY --from=build /target/*.jar app.jar
# Expone el puerto por defecto de Spring Boot
EXPOSE 8080
# Comando para ejecutar la aplicación
ENTRYPOINT ["java","-jar","app.jar"]