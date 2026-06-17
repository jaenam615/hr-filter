# syntax=docker/dockerfile:1
# api/batch 두 앱 공용 Dockerfile. MODULE 인자로 어느 모듈의 bootJar를 담을지 정한다.
# (compose가 서비스별로 MODULE을 넘김 — docker-compose.yml의 build.args 참고)
#   docker build --build-arg MODULE=application-api   .   # 서버 + 대시보드
#   docker build --build-arg MODULE=application-batch .   # 배치 스케줄러
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
ARG MODULE
# --mount=cache: gradle 의존성을 빌드 간/이미지 간 공유 (BuildKit)
RUN ./gradlew --no-daemon -x test ":${MODULE}:bootJar" \
 && cp "$(ls ${MODULE}/build/libs/*.jar | grep -v -- '-plain.jar')" /app/app.jar \

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar
# EXPOSE는 application-api(웹 서버)에서만 의미 있음.
# application-batch는 web-application-type=none이라 포트를 열지 않음(compose에서도 미매핑).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
