# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

# Package stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a non-root user and group with specific UID and GID
RUN groupadd -r -g 1001 appgroup && useradd -r -u 1001 -g appgroup appuser

# Copy the jar file from the build stage
COPY --from=build /app/target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Change ownership of the app directory to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]