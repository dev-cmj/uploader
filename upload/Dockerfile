FROM amazoncorretto:21-alpine as build

WORKDIR /workspace/app

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew :upload:bootJar -x test

FROM amazoncorretto:21-alpine
WORKDIR /app

COPY --from=build /workspace/app/upload/build/libs/*.jar app.jar

# 업로드 디렉토리 생성
RUN mkdir -p /app/uploads/temp

ENTRYPOINT ["java", "-jar", "app.jar"]