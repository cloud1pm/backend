# 1. Build Stage
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Gradle 빌드 (테스트 제외)
RUN ./gradlew clean build -x test --no-daemon

# 2. Run Stage
# openjdk 대신 안정적인 eclipse-temurin 사용
FROM eclipse-temurin:17-jdk
WORKDIR /app
# 빌드 스테이지에서 생성된 JAR 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# RUN groupadd -r spring && useradd -r -g spring spring
# USER spring

ENTRYPOINT ["java", "-jar", "app.jar"]