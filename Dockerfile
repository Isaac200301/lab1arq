# Imagen multietapa: se compila con Maven y se ejecuta sobre un JRE, de modo
# que la imagen final no arrastra el compilador ni el repositorio de Maven.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Se copian primero el pom y luego el codigo. Asi, mientras el pom no cambie,
# Docker reutiliza la capa de dependencias ya descargadas.
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# finalName en el pom.xml fija el nombre del artefacto como lab1arq.jar
COPY --from=build /app/target/lab1arq.jar lab1arq.jar

# Cloud Run publica el puerto de escucha en la variable PORT y espera que la
# aplicacion lo respete; application.properties la lee con ${PORT:8088}.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "lab1arq.jar"]
