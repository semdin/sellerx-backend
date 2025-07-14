# Backend Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
