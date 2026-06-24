# Step 1: Base image with Java
FROM eclipse-temurin:17-jdk

# Step 2: Working directory
WORKDIR /app

# Step 3: Copy pom.xml first
COPY pom.xml .

# Step 4: Copy source code
COPY src ./src

# Step 5: Install Maven (inside container)
RUN apt-get update && apt-get install -y maven

# Step 6: Build project
RUN mvn clean package -DskipTests

# Step 7: Run jar
EXPOSE 8080

CMD ["java", "-jar", "target/safe-trip-planner-0.0.1-SNAPSHOT.jar"]
