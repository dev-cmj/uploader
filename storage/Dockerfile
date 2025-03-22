FROM amazoncorretto:21-alpine as build
WORKDIR /workspace/app

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew :storage:bootJar -x test

FROM amazoncorretto:21-alpine
WORKDIR /app

COPY --from=build /workspace/app/storage/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]