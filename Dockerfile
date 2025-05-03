# Build stage
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar
# Use the PORT environment variable (default to 8080 if not set)
EXPOSE ${PORT:-8080}
ENTRYPOINT ["java", "-jar", "-Dserver.port=${PORT:-8080}", "app.jar"]