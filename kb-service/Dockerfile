FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY pom.xml .
RUN mvn install -N -q
COPY kb-service/pom.xml kb-service/
RUN mvn -f kb-service/pom.xml dependency:go-offline -q || true
COPY kb-service/src kb-service/src
RUN mvn -f kb-service/pom.xml package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /build/kb-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
