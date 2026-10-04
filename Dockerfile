# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY backend/forum-base-service/pom.xml ./backend/forum-base-service/
COPY backend/forum-base-service/src ./backend/forum-base-service/src
WORKDIR /app/backend/forum-base-service
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/backend/forum-base-service/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 7860
ENTRYPOINT ["java", "-Dserver.port=7860", "-jar", "app.jar"]
