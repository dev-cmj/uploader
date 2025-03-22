FROM amazoncorretto:21-alpine as build

WORKDIR /workspace/app

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew :validate:bootJar -x test

FROM amazoncorretto:21-alpine
WORKDIR /app

COPY --from=build /workspace/app/validate/build/libs/*.jar app.jar

# 업로드 디렉토리 접근을 위한 설정
VOLUME /app/uploads

ENTRYPOINT ["java", "-jar", "app.jar"]