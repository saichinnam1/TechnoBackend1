# Use a supported base image
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Create non-root user and group
RUN addgroup --gid 10001 appgroup && \
    adduser --uid 10001 --gid 10001 --home /home/appuser --disabled-password --gecos "" appuser

# Copy app JAR
COPY target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Set ownership
RUN chown -R 10001:10001 /app

# ✅ Set numeric UID:GID explicitly
USER 10001:10001

# Expose port
EXPOSE 8080

# Launch app
ENTRYPOINT ["java", "-jar", "app.jar"]
