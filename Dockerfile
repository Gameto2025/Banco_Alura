# ETAPA 1: Construcción
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# ETAPA 2: Ejecución
FROM eclipse-temurin:17-jre-alpine
# Esta línea ahora buscará cualquier archivo .jar en target y lo renombrará a app.jar
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]