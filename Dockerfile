FROM gradle:7-jdk11-alpine

RUN apk add --no-cache jq python3

VOLUME /home/gradle/src
WORKDIR /home/gradle/src
USER gradle

CMD ["gradle", ":io.basicbich.oui:test", "--rerun", "--no-daemon"]
