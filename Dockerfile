FROM node:24-alpine AS frontend
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci --no-fund
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend /web/dist/supporthub/browser/ ./src/main/resources/static/
RUN mvn --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=backend --chown=app:app /build/target/supporthub-1.0.0.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar"]
