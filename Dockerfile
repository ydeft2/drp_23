FROM hseeberger/scala-sbt:11.0.17_1.8.0_1.9.6

WORKDIR /app
COPY . .

RUN sbt frontend/fullOptJS
RUN sbt backend/assembly

CMD ["java", "-jar", "backend/target/scala-2.13/backend.jar"]
