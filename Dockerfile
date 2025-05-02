# Use a base image that Choreo supports
FROM eclipse-temurin:17-jre

# Set the working directory
WORKDIR /app

# Create a non-root user and group with a UID/GID in the range 10000-20000
RUN groupadd -r -g 12000 appgroup && useradd -r -u 12000 -g appgroup -m -d /home/appuser appuser

# Copy the pre-built JAR file
COPY target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Change ownership of the application files to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to the non-root user using numeric UID:GID
USER 12000:12000

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]