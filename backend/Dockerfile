# Backend image in two stages (D84): the first builds the jar, the second only runs it.

# Stage 1: build with the Maven wrapper and the full JDK 21
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Dependencies first: Docker reuses this layer while pom.xml does not change
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
# The tests need PostgreSQL: they run on the computer before every pull request (D81)
RUN ./mvnw -B -q package -DskipTests

# Stage 2: only the Java runtime and the jar, run by a user without privileges
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 resonance
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
USER resonance
ENV SPRING_PROFILES_ACTIVE=prod
# Render passes the port in the PORT variable (application-prod.yml); 8080 is the local default
EXPOSE 8080
# The free instance has 512 MB: 60% for the heap leaves room for the rest of the JVM
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]
