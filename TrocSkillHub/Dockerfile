# --- Stage 1: Maven image with Java 21 for the build ---
FROM maven:3.9-eclipse-temurin-21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy pom.xml first to cache dependency downloads
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copy the source code
COPY src ./src

# Build the project (skip tests)
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests


# --- Stage 2: Lightweight runtime image ---

# Use a Java 21 JRE image (no Maven)
FROM eclipse-temurin:21-jre

# Set the working directory
WORKDIR /app

# Copy the JAR built in the 'build' stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Application startup command
ENTRYPOINT ["java", "-jar", "app.jar"]
