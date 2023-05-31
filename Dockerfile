FROM openjdk:21-ea-23-jdk-slim as builder

WORKDIR /usr/source/

COPY . .

RUN ./gradlew installDist --no-daemon

FROM openjdk:21-ea-23-jdk-slim

WORKDIR /usr/app

COPY --from builder /usr/source/build/install/antiscam/ /usr/app/

ENTRYPOINT ["/usr/app/bin/antiscam"]
