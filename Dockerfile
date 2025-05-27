# 1) frontend
FROM hseeberger/scala-sbt:11.0.18_1.9.20_2.13.12 as frontend-builder
WORKDIR /app
COPY . .
RUN sbt frontend/fastOptJS

# 2) backend
FROM hseeberger/scala-sbt:11.0.18_1.9.20_2.13.12 as backend-builder
WORKDIR /app
COPY . .
# assemble a fat jar
RUN sbt backend/assembly

# 3) final
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=backend-builder /app/backend/target/scala-2.13/dentana-backend-assembly-0.1.0-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
