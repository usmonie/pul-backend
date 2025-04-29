FROM gradle:8.12.0-jdk23 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

FROM openjdk:23-jre-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/bank-bots-api.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-jar", "/app/bank-bots-api.jar"]
