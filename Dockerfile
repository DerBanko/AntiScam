FROM openjdk:17-alpine as builder

WORKDIR /usr/source/

COPY . .

RUN ./gradlew installDist --no-daemon

FROM openjdk:17-alpine

WORKDIR /usr/app

COPY --from builder /usr/source/build/install/antiscam/ /usr/app/

ENTRYPOINT ["/usr/app/bin/antiscam"]
