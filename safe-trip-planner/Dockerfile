# Use official Java 17 image
FROM eclipse-temurin:17-jdk

# Set working directory inside container
WORKDIR /app

# Copy Maven wrapper files first (for caching)
COPY mvnw .
COPY .mvn .mvn

# Copy pom.xml first (better caching)
COPY pom.xml .

# Download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the Spring Boot application
RUN ./mvnw clean package -DskipTests

# Expose application port
EXPOSE 8080

# Run the Spring Boot jar
CMD ["java", "-jar", "target/safe-trip-planner-0.0.1-SNAPSHOT.jar"]
