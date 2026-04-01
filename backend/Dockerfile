FROM gradle:8.14.3-jdk21-alpine AS builder
WORKDIR /app

# 1) 의존성 관련 파일 먼저 복사해서 레이어 캐시 극대화
COPY backend/build.gradle backend/settings.gradle ./
COPY backend/gradle ./gradle
COPY backend/gradlew ./gradlew

# 2) 의존성 선해결 (소스 변경과 분리)
RUN gradle --no-daemon dependencies

# 3) 애플리케이션 소스 복사 후 빌드
COPY backend/src ./src
RUN gradle --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8000

COPY --from=builder /app/build/libs/*.jar ./app.jar

EXPOSE 8000

CMD ["sh", "-c", "java -Dserver.port=${PORT} -jar /app/app.jar"]
