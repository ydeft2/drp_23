# ---- Builder stage ----
FROM openjdk:11-slim AS builder

ARG SBT_VERSION=1.9.6
WORKDIR /app

# Install sbt and dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
      curl gnupg2 ca-certificates \
  && echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" \
       > /etc/apt/sources.list.d/sbt.list \
  && curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99E82A75642AC823" | apt-key add - \
  && apt-get update \
  && apt-get install -y --no-install-recommends sbt=${SBT_VERSION}* \
  && rm -rf /var/lib/apt/lists/*

# Copy the project
COPY . .

# Build frontend (Scala.js)
RUN sbt frontend/fastLinkJS

# Copy the JS bundle to backend/public
RUN mkdir -p backend/public \
  && cp frontend/target/scala-3.3.1/frontend-fastopt/main.js backend/public/frontend.js \
  && cp frontend/public/index.html backend/public/index.html
RUN sbt backend/assembly

# ---- Runtime stage ----
FROM openjdk:11-jre-slim
WORKDIR /app

# Copy the assembled JAR
COPY --from=builder /app/backend/target/scala-3.3.1/*-assembly-*.jar ./app.jar
COPY --from=builder /app/backend/public ./public
ENTRYPOINT ["java", "-jar", "app.jar"]