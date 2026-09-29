FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .mvn-settings.xml ./
COPY src ./src

RUN mvn -B -s .mvn-settings.xml -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN groupadd --system appgroup \
    && useradd --system --uid 10001 --gid appgroup appuser

COPY --from=build /workspace/target/*.jar /app/app.jar

USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
