FROM gradle:8.14.3-jdk21-alpine AS builder
WORKDIR /app

COPY backend/ ./
RUN gradle clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8000

COPY --from=builder /app/build/libs/*.jar ./app.jar

EXPOSE 8000

CMD ["sh", "-c", "java -Dserver.port=${PORT} -jar /app/app.jar"]
