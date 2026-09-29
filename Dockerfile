FROM maven:3.9-eclipse-temurin-17 AS build
<<<<<<< HEAD
WORKDIR /workspace
COPY pom.xml .mvn-settings.xml ./
COPY src ./src
RUN mvn -B -s .mvn-settings.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/target/*.jar /app/app.jar
USER 10001
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
=======

WORKDIR /app

COPY pom.xml .

RUN mvn -q -B dependency:go-offline

COPY src ./src

RUN mvn -q -B package -DskipTests


FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -q -O /dev/null http://localhost:${PORT:-${SERVER_PORT:-8082}}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
>>>>>>> 7bd0c33e715997e825808a80d83570bef2067213
