FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/*.jar app.jar
# 실행 시 JAVA_OPTS로 프로필을 분리하여 실행 (예: -Dspring.profiles.active=backend)
ENTRYPOINT ["java", "-jar", "app.jar"]