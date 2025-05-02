# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the Maven build output (JAR file) into the container
COPY target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar

# Create the uploads directory
RUN mkdir -p /uploads

# Expose the port the app runs on
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]