# Use a base image supported by Choreo
FROM eclipse-temurin:17-jre

# Set the working directory
WORKDIR /app

# Create a non-root user and group with UID/GID between 10001 and 20000
RUN groupadd -r -g 12001 appgroup && \
    useradd -r -u 12001 -g appgroup -m -d /home/appuser appuser

# Copy the pre-built JAR file into the container
COPY target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Set ownership of the application files to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user using the username (not just UID)
USER appuser

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
