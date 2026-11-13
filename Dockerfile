# Etapa 1: compilación
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: ejecución
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd -r -u 1001 appuser \
    && mkdir -p /app/grabaciones \
    && chown appuser /app/grabaciones
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8070
ENTRYPOINT ["java", "-jar", "app.jar"]
