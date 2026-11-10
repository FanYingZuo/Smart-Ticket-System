FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY pom.xml .
RUN mvn install -N -q
COPY ticket-service/pom.xml ticket-service/
RUN mvn -f ticket-service/pom.xml dependency:go-offline -q || true
COPY ticket-service/src ticket-service/src
RUN mvn -f ticket-service/pom.xml package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /build/ticket-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
