# Build stage
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY . .
# Ensure mvnw is executable
RUN chmod +x mvnw
# Build the JAR, skipping tests
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]