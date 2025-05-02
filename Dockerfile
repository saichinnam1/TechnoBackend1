# Use an official OpenJDK runtime as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Create a non-root user and group with a UID and GID between 10001–20000
RUN groupadd -r -g 10001 appgroup && \
    useradd -r -u 10001 -g appgroup -m -d /home/appuser appuser

# Copy the Spring Boot JAR file into the container
COPY target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Change ownership of the application files to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER 10001:10001

# Expose the port your app runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
