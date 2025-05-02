# Use a Choreo-supported base image
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Create non-root user/group with UID and GID in 10000-20000 range
RUN groupadd -g 10001 appgroup && \
    useradd -u 10001 -g appgroup -m -d /home/appuser appuser

# Copy your JAR
COPY target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Set correct ownership
RUN chown -R 10001:10001 /app

# Set the USER using numeric UID:GID (this is what the scan checks)
USER 10001:10001

# Expose app port
EXPOSE 8080

# Start app
ENTRYPOINT ["java", "-jar", "app.jar"]
