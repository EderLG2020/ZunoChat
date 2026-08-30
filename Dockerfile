# ─── Build ───────────────────────────────────────────────────────────────
# No hay mvnw en el repo, así que usamos una imagen con Maven ya instalado.
# Fija JDK 21 (mismo que java.version en pom.xml) — evita depender del JDK
# que tenga instalado cada máquina (en dev, un JDK 25 rompe Lombok).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cachea las dependencias en su propia capa: solo se re-descargan si cambia
# el pom.xml, no en cada cambio de código fuente.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# ─── Runtime ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080

# MaxRAMPercentage en vez de -Xmx fijo: se adapta al límite de memoria del
# contenedor (fly.toml [[vm]] memory) sin tener que sincronizar dos valores.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -jar app.jar"]
