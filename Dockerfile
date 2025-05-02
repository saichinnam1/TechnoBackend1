# Use a base image that Choreo supports
FROM eclipse-temurin:17-jre

# Set the working directory
WORKDIR /app

# Copy the pre-built JAR file
COPY target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]