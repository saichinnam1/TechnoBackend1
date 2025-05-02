# Stage 1: Build the application
FROM openjdk:17-jdk-slim AS builder

# Set the working directory for the build
WORKDIR /app

# Copy the Maven/Gradle files and source code
COPY . .

# Build the application (adjust for Gradle if needed)
RUN apt-get update && apt-get install -y maven && mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:17-jdk-slim

# Set the working directory for the runtime
WORKDIR /app

# Create a non-root user and group with a UID/GID in the range 10000-20000
RUN groupadd -r -g 12000 appgroup && useradd -r -u 12000 -g appgroup -m -d /home/appuser appuser

# Copy the JAR file from the builder stage
COPY --from=builder /app/target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Change ownership of the application files to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to the non-root user using numeric UID:GID
USER 12000:12000

# Expose the port your app runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]