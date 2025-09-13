########################
# BUILD
########################
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY . .
RUN chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar -x test

########################
# RUNTIME
########################
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd -r -u 1001 app && chown -R 1001:0 /app
USER 1001

COPY --from=build /app/build/libs/*.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=70"

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
