FROM gradle:7-jdk11-alpine

VOLUME /home/gradle/src
WORKDIR /home/gradle/src

RUN apk add --no-cache jq python3

CMD ["gradle", ":io.basicbich.oui.tests:test", "--rerun", "--no-daemon"]
