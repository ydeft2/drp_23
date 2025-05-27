###########################
# 1) builder: openjdk + sbt
###########################
FROM openjdk:11-slim AS builder

# allow overriding sbt version if you like
ARG SBT_VERSION=1.9.9

WORKDIR /app

# install prereqs + add the sbt apt repo
RUN apt-get update && apt-get install -y --no-install-recommends \
      curl gnupg2 ca-certificates unzip \
  && echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" \
       > /etc/apt/sources.list.d/sbt.list \
  && curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99E82A75642AC823" \
       | apt-key add - \
  && apt-get update \
  && apt-get install -y --no-install-recommends sbt=${SBT_VERSION}* \
  && rm -rf /var/lib/apt/lists/*

# copy in your project
COPY . .

# 1) build the optimized Scala.js bundle
RUN sbt frontend/fullOptJS \
  && mkdir -p backend/src/main/resources/web \
  && cp frontend/target/scala-*/dentana-frontend-fullopt.js \
        backend/src/main/resources/web/frontend.js

# 2) assemble the backend fat JAR
RUN sbt backend/assembly

###########################
# 2) runtime: just the JAR
###########################
FROM openjdk:11-jre-slim
WORKDIR /app

# copy over only the assembled JAR
COPY --from=builder \
     /app/backend/target/scala-2.13/*-assembly-*.jar \
     ./app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
