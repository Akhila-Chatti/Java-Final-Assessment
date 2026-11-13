# One image with every service jar; docker-compose picks one with SERVICE.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/*/target/*.jar ./
ENTRYPOINT ["sh", "-c", "exec java -jar /app/$SERVICE.jar"]
