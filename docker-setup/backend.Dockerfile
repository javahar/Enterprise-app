# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# Use the official Maven image with Java 21 to compile and package the app
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first and download dependencies
# This layer is cached — only re-runs if pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the JAR (skip tests in Docker build)
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
# Use a slim JRE-only image — no Maven, no JDK, much smaller final image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user to run the app (security best practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy only the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the backend port
EXPOSE 9090

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
