# 1) builder stage: compile Scala.js + backend assembly
FROM hseeberger/scala-sbt:11.0.18_2.13.12_1.9.6 AS builder
WORKDIR /app

# 1a) cache sbt metadata
COPY build.sbt project/plugins.sbt ./
RUN sbt update

# 1b) bring in your code
COPY . .

# 1c) build the optimized Scala.js bundle
RUN sbt frontend/fullOptJS \
  && mkdir -p backend/src/main/resources/web \
  && cp frontend/target/scala-2.13/dentana-frontend-fullopt.js \
        backend/src/main/resources/web/frontend.js

# 1d) assemble the fat JAR
RUN sbt backend/assembly

# 2) runtime stage: just ship the JAR
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder \
     /app/backend/target/scala-2.13/dentana-backend-assembly-0.1.0-SNAPSHOT.jar \
     ./app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
